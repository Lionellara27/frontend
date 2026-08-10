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

    @FXML private Pagination paginadorHistorial;

    @FXML private ComboBox<String> cmbCampoBusqueda; // 🔥 NUEVO: El selector de tipo de búsqueda

    private final VentaApiService apiService = new VentaApiService();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        System.out.println("========== HISTORIAL VENTAS ==========");
        System.out.println("🚀 Inicializando módulo de Historial de Ventas...");
        tablaVentas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        configurarColumnas();

        // 🔥 1. Llenamos el ComboBox de Meses
        cmbMes.getItems().addAll("Todos", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre");
        cmbMes.setValue("Todos");

        // 🔥 NUEVO: Llenamos el ComboBox de Campo de Búsqueda
        if (cmbCampoBusqueda != null) {
            cmbCampoBusqueda.getItems().addAll("Nro. Comprobante", "Cliente (Nombre/DNI)");
            cmbCampoBusqueda.setValue("Nro. Comprobante");

            // Escuchador al cambiar el criterio (busca de cero al cambiar entre Factura o Cliente)
            cmbCampoBusqueda.valueProperty().addListener((observable, valorViejo, valorNuevo) -> {
                recargarHistorialDesdeCero();
            });
        }

        // 🔥 2. Escuchadores para filtrar al instante (ComboBox Mes)
        cmbMes.setOnAction(e -> recargarHistorialDesdeCero());

        // 🔥 3. Búsqueda predictiva en tiempo real (Al tipear letra por letra)
        txtBuscarVenta.textProperty().addListener((observable, oldValue, newValue) -> {
            recargarHistorialDesdeCero();
        });

        // 🔥 4. Respaldo por si la cajera presiona ENTER por inercia
        txtBuscarVenta.setOnKeyReleased(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                recargarHistorialDesdeCero();
            }
        });

        // 🔥 5. Conexión del paginador nativo
        if (paginadorHistorial != null) {
            paginadorHistorial.setPageFactory(paginaIndex -> {
                cargarVentas(paginaIndex);
                return new javafx.scene.layout.VBox();
            });
        } else {
            cargarVentas(0);
        }
        System.out.println("======================================");

    }

    // 🔥 Atrapa el clic del botón de la lupa para que no tire error
    @FXML
    public void buscarVenta(javafx.event.ActionEvent event) {
        // No hace falta poner código acá porque el listener de arriba ya hace el trabajo al tipear
        System.out.println("Lupa clickeada. Filtrando...");
    }

    // Método de apoyo para resetear la página a 0 cuando buscás algo nuevo
    private void recargarHistorialDesdeCero() {
        if (paginadorHistorial != null) {
            paginadorHistorial.setCurrentPageIndex(0);
        }
        cargarVentas(0);
    }

    private void configurarColumnas() {
        // 1. Datos directos
// 1. Datos directos
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(column -> new TableCell<Venta, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Magia: Formatea el número a $ 2.338.000
                    setText(java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("es", "AR")).format(item));
                }
            }
        });

        // 🔥 ACÁ VOLVEMOS A AGREGAR LA COLUMNA DEL CLIENTE QUE SE HABÍA BORRADO
        colCliente.setCellValueFactory(cell -> { com.nakel.frontend.model.Venta venta = cell.getValue(); System.out.println(); System.out.println("👀 [HISTORIAL CHISMOSO] Renderizando cliente de una venta..."); if (venta == null) { System.out.println("❌ [HISTORIAL CHISMOSO] Venta = NULL"); return new javafx.beans.property.SimpleStringProperty("Consumidor Final"); } System.out.println("🧾 [HISTORIAL CHISMOSO] Venta ID: " + venta.getId()); if (venta.getCliente() != null) { System.out.println("👤 [HISTORIAL CHISMOSO] Cliente recibido:"); System.out.println(" ├─ ID: " + venta.getCliente().getId()); System.out.println(" ├─ Nombre: " + venta.getCliente().getNombre()); System.out.println(" └─ CUIT: " + venta.getCliente().getCuit()); String nombreCliente = venta.getCliente().getNombre(); if (nombreCliente != null && !nombreCliente.trim().isEmpty()) { return new javafx.beans.property.SimpleStringProperty(nombreCliente); } } else { System.out.println("⚠️ [HISTORIAL CHISMOSO] venta.getCliente() = NULL"); System.out.println(" └─ Se mostrará: Consumidor Final"); } return new javafx.beans.property.SimpleStringProperty("Consumidor Final");
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

                            if (empty || getTableView().getItems().get(getIndex()) == null) {
                                setGraphic(null);
                            } else {
                                Venta ventaFila = getTableView().getItems().get(getIndex());

                                // 1. RESETEO VITAL: Como JavaFX recicla celdas al scrollear,
                                // siempre arrancamos asumiendo que el botón está habilitado
                                btnCambio.setDisable(false);

                                // 2. VERIFICACIÓN DE CAMBIO: ¿Esta venta ya fue cambiada antes?
                                // (Usamos Boolean.TRUE.equals para evitar NullPointerExceptions si el campo viene nulo)
                                // 2. VERIFICACIÓN DE CAMBIOS Y LÍMITES
                                int cantidadCambios = (ventaFila.getHistorialCambios() != null) ? ventaFila.getHistorialCambios().size() : 0;

                                if (cantidadCambios >= 2) {
                                    // 🛑 ESTADO 2: Bloqueo total (Llegó al límite máximo)
                                    btnCambio.setStyle("-fx-background-color: transparent; -fx-cursor: default; -fx-text-fill: #E0E0E0; -fx-font-size: 14px;");
                                    btnCambio.setDisable(true); // Bloqueado, no se puede hacer más nada

                                } else if (cantidadCambios == 1) {
                                    // ⚠️ ESTADO 1: Ya tiene un cambio. Se permite un segundo forzado (Botón gris pero HABILITADO)
                                    btnCambio.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #9e9e9e; -fx-font-size: 14px;");
                                    btnCambio.setDisable(false);

                                } else {
                                    // ✅ ESTADO 0: Cero cambios. Evaluamos la regla de los 30 días
                                    btnCambio.setDisable(false);
                                    try {
                                        java.time.LocalDateTime fechaVenta = java.time.LocalDateTime.parse(ventaFila.getFechaHora());
                                        java.time.LocalDateTime fechaLimite = fechaVenta.plusDays(30);

                                        if (java.time.LocalDateTime.now().isAfter(fechaLimite)) {
                                            // Vencido -> Naranja
                                            btnCambio.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #FF9800; -fx-font-size: 14px;");
                                        } else {
                                            // En regla -> Verde
                                            btnCambio.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #4CAF50; -fx-font-size: 14px;");
                                        }
                                    } catch (Exception e) {
                                        btnCambio.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: #FF9800; -fx-font-size: 14px;");
                                    }
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

    private void cargarVentas(int numeroPagina) {
        String busqueda = txtBuscarVenta.getText() != null ? txtBuscarVenta.getText().trim() : "";

        // 🔥 CHISMOSO 1: Ver qué busca y a qué página exacta está apuntando el Frontend
        System.out.println("🔍 [DEBUG HISTORIAL] Pidiendo página al Backend: " + numeroPagina + " | Buscando: '" + busqueda + "'");

        String criterio = (cmbCampoBusqueda != null && cmbCampoBusqueda.getValue() != null)
                ? cmbCampoBusqueda.getValue()
                : "Nro. Comprobante";

        if ("Nro. Comprobante".equals(criterio)) {
            if (busqueda.contains("0001 -")) {
                busqueda = busqueda.replaceAll(".*0001\\s*-\\s*", "").replaceFirst("^0+", "");
            } else if (busqueda.matches("0+\\d+")) {
                busqueda = busqueda.replaceFirst("^0+", "");
            }
        }

        int mesNumero = convertirMesANumero(cmbMes.getValue());

        String json = apiService.obtenerHistorialVentasPaginado(numeroPagina, 20, busqueda, criterio, mesNumero);

        if (json != null && !json.equals("[]") && !json.isEmpty()) {
            try {
                com.google.gson.JsonElement elementoParseado = com.google.gson.JsonParser.parseString(json);
                com.google.gson.JsonArray arregloVentas;

                if (elementoParseado.isJsonObject()) {
                    com.google.gson.JsonObject respuestaServidor = elementoParseado.getAsJsonObject();

                    // 🔥 CHISMOSO CLAVE: ¿Cuántos registros dice el Backend que existen EN TOTAL?
                    if (respuestaServidor.has("totalElements")) {
                        long totalElementos = respuestaServidor.get("totalElements").getAsLong();

                        System.out.println("📊 [DEBUG HISTORIAL] TOTAL DE VENTAS SEGÚN BACKEND: " + totalElementos);
                    } else {
                        System.out.println("⚠️ [DEBUG HISTORIAL] El JSON NO contiene 'totalElements'");
                    }
                    //------------chismeeeeeeeeee

                    if (respuestaServidor.has("totalPages") && paginadorHistorial != null) {
                        int totalPaginas = respuestaServidor.get("totalPages").getAsInt();
                        paginadorHistorial.setPageCount(totalPaginas == 0 ? 1 : totalPaginas);

                        // 🔥 CHISMOSO 2: El Backend te dice cuántas páginas totales armó (debería decir 3)
                        System.out.println("📄 [DEBUG HISTORIAL] Total de páginas calculadas por el Backend: " + totalPaginas);
                    }

                    arregloVentas = respuestaServidor.getAsJsonArray("content");

                    // 🔥 CHISMOSO 3: Cuántas ventas vienen en este bloque específico
                    System.out.println("📦 [DEBUG HISTORIAL] Ventas detectadas en el 'content' de esta página: " + (arregloVentas != null ? arregloVentas.size() : 0));

                } else if (elementoParseado.isJsonArray()) {
                    arregloVentas = elementoParseado.getAsJsonArray();
                    System.out.println("📦 [DEBUG HISTORIAL] Llegó un Array directo de tamaño: " + arregloVentas.size());
                    if (paginadorHistorial != null) paginadorHistorial.setPageCount(1);
                } else {
                    throw new RuntimeException("Formato JSON no reconocido");
                }

                java.lang.reflect.Type tipoLista = new com.google.gson.reflect.TypeToken<java.util.List<Venta>>(){}.getType();
                java.util.List<Venta> listaVentas = gson.fromJson(arregloVentas, tipoLista);

                // 🔥 CHISMOSO 4: Cuántos objetos Java reales se metieron en la tabla
                System.out.println("✅ [DEBUG HISTORIAL] Objetos listos para renderizar en la TableView: " + (listaVentas != null ? listaVentas.size() : 0));

                javafx.collections.ObservableList<Venta> datosObservable = javafx.collections.FXCollections.observableArrayList(listaVentas);
                tablaVentas.setItems(datosObservable);

                double totalGlobal = apiService.obtenerTotalGlobal(busqueda, criterio, mesNumero);
                String totalFormateado = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("es", "AR")).format(totalGlobal);
                lblTotalFacturado.setText("TOTAL: " + totalFormateado + " 💰");

            } catch (Exception e) {
                System.out.println("❌ Error al cargar historial paginado: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ [DEBUG HISTORIAL] La respuesta del servidor vino vacía o nula.");
            tablaVentas.getItems().clear();
            if (lblTotalFacturado != null) lblTotalFacturado.setText("TOTAL: $ 0,00 💰");
        }
    }

    @FXML
    public void limpiarBusqueda() {
        txtBuscarVenta.clear();
        if (cmbCampoBusqueda != null) cmbCampoBusqueda.setValue("Nro. Comprobante");
        cmbMes.setValue("Todos");
        recargarHistorialDesdeCero();
    }

    // Traductor del ComboBox para el Backend
    private int convertirMesANumero(String mes) {
        if (mes == null || mes.equals("Todos")) return 0;
        switch (mes) {
            case "Enero": return 1; case "Febrero": return 2; case "Marzo": return 3;
            case "Abril": return 4; case "Mayo": return 5; case "Junio": return 6;
            case "Julio": return 7; case "Agosto": return 8; case "Septiembre": return 9;
            case "Octubre": return 10; case "Noviembre": return 11; case "Diciembre": return 12;
            default: return 0;
        }
    }

    // 🔥 Sobrecarga para mantener retrocompatibilidad (por si algún botón hace un llamado sin parámetros)
    @FXML
    public void cargarVentas() {
        cargarVentas(paginadorHistorial != null ? paginadorHistorial.getCurrentPageIndex() : 0);
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

    // --- ACCIONES DE LOS BOTONES DE LA TABLA -----------------------

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
            int cantidadCambios = (venta.getHistorialCambios() != null) ? venta.getHistorialCambios().size() : 0;

            // Medida de seguridad extra por si logran clickear el botón bloqueado
            if (cantidadCambios >= 2) {
                Alert error = new Alert(Alert.AlertType.ERROR, "Esta venta ya superó el límite máximo de cambios permitidos.");
                error.showAndWait();
                return;
            }

            // Validación de fechas
            java.time.LocalDateTime fechaVenta = java.time.LocalDateTime.parse(venta.getFechaHora());
            java.time.LocalDateTime fechaLimite = fechaVenta.plusDays(30);
            boolean estaVencido = java.time.LocalDateTime.now().isAfter(fechaLimite);

            // 🛑 Si tiene 1 cambio previo O está vencido, pedimos clave
            if (cantidadCambios == 1 || estaVencido) {

                String mensajeAlerta = (cantidadCambios == 1)
                        ? "ATENCIÓN: Esta venta ya tiene un cambio previo.\nIngrese la contraseña:"
                        : "El plazo de 30 días ha vencido.\nIngrese contraseña de Administrador para forzar el cambio:";

                Dialog<String> dialog = new Dialog<>();
                dialog.setTitle("Autorización Requerida");
                dialog.setHeaderText(mensajeAlerta);

                ButtonType btnAutorizar = new ButtonType("Autorizar", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(btnAutorizar, ButtonType.CANCEL);

                PasswordField txtClave = new PasswordField();
                txtClave.setPromptText("Contraseña...");
                dialog.getDialogPane().setContent(txtClave);

                dialog.setResultConverter(dialogButton -> {
                    if (dialogButton == btnAutorizar) return txtClave.getText();
                    return null;
                });

                dialog.showAndWait().ifPresent(clave -> {
                    // 1. Agarramos el usuario que está usando el sistema ahora mismo (ej: "ad")
                    String usuarioActual = com.nakel.frontend.util.SesionActual.getUsuarioLogueado();

                    // 2. Le preguntamos a tu API si esa clave es correcta
                    com.nakel.frontend.service.UsuarioApiService usuarioApi = new com.nakel.frontend.service.UsuarioApiService();
                    boolean esClaveCorrecta = usuarioApi.login(usuarioActual, clave);

                    if (esClaveCorrecta) {
                        System.out.println("✅ Autorizado por el backend para el usuario: " + usuarioActual);
                        abrirPantallaCambio(venta);
                    } else {
                        Alert error = new Alert(Alert.AlertType.ERROR, "Contraseña incorrecta para el usuario '" + usuarioActual + "'. Operación cancelada.");
                        error.showAndWait();
                    }
                });

            } else {
                // ✅ 0 CAMBIOS Y DENTRO DE LOS 30 DÍAS: Pasa directo
                System.out.println("En regla. Abriendo módulo de cambio...");
                abrirPantallaCambio(venta);
            }

        } catch (Exception e) {
            System.err.println("Error al procesar el cambio: " + e.getMessage());
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