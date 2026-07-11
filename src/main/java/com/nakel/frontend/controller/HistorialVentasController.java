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
                            if (empty) {
                                setGraphic(null);
                            } else {
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

    private void abrirOpcionesImpresionEnvio(Venta venta) {
        System.out.println("🖨️/📧 Abriendo opciones de imprimir/mail para la venta: " + venta.getId());
        // (Acá irá la Etapa 4)
    }

    private void iniciarProcesoCambio(Venta venta) {
        System.out.println("🔄 Evaluando los 30 días para cambio/devolución de la venta: " + venta.getId());
        // (Acá irá la Etapa 3)
    }
}