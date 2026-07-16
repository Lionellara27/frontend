package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.Venta;
import com.nakel.frontend.service.VentaApiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.lang.reflect.Type;
import java.util.List;

public class HistorialVentasController {

    @FXML private ComboBox<String> cmbMes;
    @FXML private TextField txtBuscarVenta;
    @FXML private Label lblTotalFacturado;

    // La tabla y sus columnas (conectadas al FXML)
    @FXML private TableView<Venta> tablaVentas;
    @FXML private TableColumn<Venta, String> colFecha;
    @FXML private TableColumn<Venta, String> colNro;
    @FXML private TableColumn<Venta, String> colCliente;
    @FXML private TableColumn<Venta, Double> colTotal;
    @FXML private TableColumn<Venta, String> colEstado;
    @FXML private TableColumn<Venta, Void> colAcciones;

    private final VentaApiService apiService = new VentaApiService();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        System.out.println("Módulo de Historial de Ventas listo.");
        tablaVentas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        configurarColumnas();
        cargarVentas();
    }

    private void configurarColumnas() {
        // 1. Datos directos
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        // 2. Datos procesados (Magia de JavaFX para adaptar el modelo a la vista)

        // 🔥 ACÁ ESTÁ EL CAMBIO: Evaluamos si hay cliente y sacamos su nombre
        colCliente.setCellValueFactory(cell -> {
            if (cell.getValue().getCliente() != null) {
                return new SimpleStringProperty(cell.getValue().getCliente().getNombre());
            } else {
                return new SimpleStringProperty("Consumidor Final");
            }
        });

        // Simulamos un número de comprobante con el ID de la base de datos
        colNro.setCellValueFactory(cell -> new SimpleStringProperty("0001 - " + String.format("%08d", cell.getValue().getId())));

        // Lógica de AFIP (Si esFiscal es true, mostramos que hay que facturar)
        colEstado.setCellValueFactory(cell -> {
            boolean esFiscal = cell.getValue().getEsFiscal() != null && cell.getValue().getEsFiscal();
            return new SimpleStringProperty(esFiscal ? "A Facturar (AFIP)" : "No Declarado");
        });

        // Parseamos la fecha
        colFecha.setCellValueFactory(cell -> {
            String fechaCruda = cell.getValue().getFechaHora(); // Tomamos la fecha original
            if (fechaCruda == null) return new SimpleStringProperty("");
            try {
                java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(fechaCruda);
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");
                return new SimpleStringProperty(dateTime.format(formatter));
            } catch (Exception e) {
                return new SimpleStringProperty(fechaCruda); // Si falla, muestra la original
            }
        });

        // 3. La Fábrica de Botones
        if (colAcciones != null) {
            javafx.util.Callback<TableColumn<Venta, Void>, TableCell<Venta, Void>> cellFactory = new javafx.util.Callback<>() {
                @Override
                public TableCell<Venta, Void> call(final TableColumn<Venta, Void> param) {
                    return new TableCell<>() {
                        // Creamos los botones importando FontIcon directamente
                        private final Button btnVer = new Button("", new org.kordamp.ikonli.javafx.FontIcon("fas-eye"));
                        private final Button btnOpciones = new Button("", new org.kordamp.ikonli.javafx.FontIcon("fas-print"));
                        private final Button btnCambio = new Button("", new org.kordamp.ikonli.javafx.FontIcon("fas-exchange-alt"));

                        private final javafx.scene.layout.HBox panelAcciones = new javafx.scene.layout.HBox(10, btnVer, btnOpciones, btnCambio);

                        {
                            panelAcciones.setAlignment(javafx.geometry.Pos.CENTER);

                            btnVer.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #2196F3; -fx-font-size: 14px;");
                            btnOpciones.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #607D8B; -fx-font-size: 14px;");
                            btnCambio.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #FF9800; -fx-font-size: 14px;");

                            btnVer.setOnAction(e -> verDetalleVenta(getTableView().getItems().get(getIndex())));
                            btnOpciones.setOnAction(e -> abrirOpcionesImpresionEnvio(getTableView().getItems().get(getIndex())));
                            btnCambio.setOnAction(e -> iniciarProcesoCambio(getTableView().getItems().get(getIndex())));
                        }

                        @Override
                        public void updateItem(Void item, boolean empty) {
                            super.updateItem(item, empty);
                            // Verificamos que la fila no esté vacía ni sea nula
                            if (empty || getTableView().getItems().get(getIndex()) == null) {
                                setGraphic(null);
                            } else {
                                // Agarramos la venta de esta fila específica
                                Venta ventaFila = getTableView().getItems().get(getIndex());

                                try {
                                    // 🔥 ACÁ OCURRE LA MAGIA DEL CÁLCULO
                                    java.time.LocalDateTime fechaVenta = java.time.LocalDateTime.parse(ventaFila.getFechaHora());
                                    java.time.LocalDateTime fechaLimite = fechaVenta.plusDays(30); // Le sumamos 30 días a la fecha original

                                    // Comparamos la fecha límite con el instante exacto de AHORA
                                    if (java.time.LocalDateTime.now().isAfter(fechaLimite)) {
                                        // 🛑 Pasaron los 30 días -> Pintamos el botón de GRIS
                                        btnCambio.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #9E9E9E; -fx-font-size: 14px;");
                                    } else {
                                        // ✅ Está dentro del plazo -> Pintamos el botón de VERDE
                                        btnCambio.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #4CAF50; -fx-font-size: 14px;");
                                    }
                                } catch (Exception e) {
                                    // Si por algún motivo la fecha viene mal o vacía, lo dejamos naranja por defecto
                                    btnCambio.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #FF9800; -fx-font-size: 14px;");
                                }

                                // Finalmente, metemos la caja con los 3 botones en la celda
                                setGraphic(panelAcciones);
                            }
                        }
                    };
                }
            };
            colAcciones.setCellFactory(cellFactory);
        }
        // 3. (A futuro) Acá podés meter tu CellFactory con FontAwesome para los botones de Imprimir/Ver
    }

    private void cargarVentas() {
        String json = apiService.obtenerHistorialVentas();

        if (json != null && !json.equals("[]") && !json.isEmpty()) {
            try {
                // Leemos el objeto paginado del backend
                JsonObject respuestaServidor = JsonParser.parseString(json).getAsJsonObject();
                JsonArray arregloVentas = respuestaServidor.getAsJsonArray("content");

                // Convertimos a Java
                Type tipoLista = new TypeToken<List<Venta>>(){}.getType();
                List<Venta> listaVentas = gson.fromJson(arregloVentas, tipoLista);

                // Llenamos la tabla
                ObservableList<Venta> datosObservable = FXCollections.observableArrayList(listaVentas);
                tablaVentas.setItems(datosObservable);

                // Actualizamos la etiqueta del total facturado
                calcularTotalPantalla(listaVentas);

                System.out.println("✅ Historial cargado con " + listaVentas.size() + " ventas.");
            } catch (Exception e) {
                System.out.println("❌ Error al cargar historial: " + e.getMessage());
            }
        }
    }

    private void calcularTotalPantalla(List<Venta> ventas) {
        double suma = ventas.stream().mapToDouble(Venta::getTotal).sum();
        lblTotalFacturado.setText("$ " + String.format("%.2f", suma));
    }

    @FXML
    public void abrirPuntoDeVenta(ActionEvent event) {
        System.out.println("🚀 Saltando al Mostrador usando el Router...");
        // Acá llamás a tu clase Navegador/Router como tenías pensado
        com.nakel.frontend.util.Navegador.cargarVista("/com/nakel/frontend/view/venta-view.fxml");
    }

    // --- ACCIONES DE LOS BOTONES DE LA TABLA ---



    private void verDetalleVenta(Venta venta) {
        System.out.println("👁️ Abriendo modal de detalles para la venta: " + venta.getId());
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/detalle-venta-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            // Le pasamos la venta al controlador del modal
            DetalleVentaController controller = loader.getController();
            controller.cargarDatosVenta(venta);

            javafx.stage.Stage modalStage = new javafx.stage.Stage();
            modalStage.setTitle("Detalle de Venta");
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.setResizable(false);
            modalStage.showAndWait();

        } catch (Exception e) {
            System.err.println("Error al abrir el detalle de la venta.");
            e.printStackTrace();
        }
    }

    private void iniciarProcesoCambio(Venta venta) {
        try {
            java.time.LocalDateTime fechaVenta = java.time.LocalDateTime.parse(venta.getFechaHora());
            java.time.LocalDateTime fechaLimite = fechaVenta.plusDays(30);

            if (java.time.LocalDateTime.now().isAfter(fechaLimite)) {
                // 🛑 ESTÁ VENCIDO: Invocamos el Pop-up de Administrador
                Dialog<String> dialog = new Dialog<>();
                dialog.setTitle("Autorización Requerida");
                dialog.setHeaderText("El plazo de 30 días ha vencido.\nIngrese contraseña de Administrador para forzar el cambio:");

                // Botones del pop-up
                ButtonType btnAutorizar = new ButtonType("Autorizar", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(btnAutorizar, ButtonType.CANCEL);

                // Campo de contraseña oculta
                PasswordField txtClave = new PasswordField();
                txtClave.setPromptText("Contraseña...");
                dialog.getDialogPane().setContent(txtClave);

                // Capturamos el resultado
                dialog.setResultConverter(dialogButton -> {
                    if (dialogButton == btnAutorizar) return txtClave.getText();
                    return null;
                });

                // Mostramos y evaluamos
                dialog.showAndWait().ifPresent(clave -> {
                    // TODO: A futuro validar esto contra el backend o una variable encriptada
                    if ("admin123".equals(clave)) {
                        System.out.println("✅ Autorizado por la dueña.");
                        abrirPantallaCambio(venta); // Avanza
                    } else {
                        Alert error = new Alert(Alert.AlertType.ERROR, "Contraseña incorrecta. Operación cancelada.");
                        error.showAndWait();
                    }
                });

            } else {
                // ✅ ESTÁ DENTRO DE LOS 30 DÍAS: Pasa directo
                System.out.println("En regla. Abriendo módulo de cambio...");
                abrirPantallaCambio(venta);
            }

        } catch (Exception e) {
            System.err.println("Error al procesar la fecha de cambio: " + e.getMessage());
        }
    }

    // El método que va a abrir la ventana pesada de stock (La armamos en el próximo paso)
    private void abrirPantallaCambio(Venta venta) {
        System.out.println("🚀 ¡Abriendo el módulo maestro de Cambios y Devoluciones para la venta: " + venta.getId() + "!");
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/cambio-venta-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            // Le pasamos la venta al controlador del módulo de cambios
            CambioVentaController controller = loader.getController();
            controller.cargarVentaOriginal(venta);

            javafx.stage.Stage modalStage = new javafx.stage.Stage();
            modalStage.setTitle("Gestión de Cambios y Devoluciones");
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.setResizable(false);
            modalStage.showAndWait();

        } catch (Exception e) {
            System.err.println("❌ Error al abrir la pantalla de cambios.");
            e.printStackTrace();
        }
    }


    private void abrirOpcionesImpresionEnvio(Venta venta) {
        System.out.println("🖨️/📧 Abriendo opciones de imprimir/mail para la venta: " + venta.getId());
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/opciones-impresion-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            // Pasamos la venta seleccionada al controlador
            OpcionesImpresionController controller = loader.getController();
            controller.cargarVenta(venta);

            javafx.stage.Stage modalStage = new javafx.stage.Stage();
            modalStage.setTitle("Opciones de Impresión");
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.setResizable(false);
            modalStage.showAndWait();

        } catch (Exception e) {
            System.err.println("❌ Error al abrir la pantalla de opciones de impresión.");
            e.printStackTrace();
        }
    }
}