package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.nakel.frontend.model.Articulo;
import com.nakel.frontend.model.DetalleVenta;
import com.nakel.frontend.model.LineaTicket;
import com.nakel.frontend.model.Pago;
import com.nakel.frontend.service.ArticuloApiService;
import com.nakel.frontend.service.VentaApiService;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.List;

public class VentaController {

    private final ArticuloApiService apiService = new ArticuloApiService();

    // Cabecera AFIP
// 🔥 ¡ATENCIÓN! Cambiar de <String> a <Cliente>
    @FXML private ComboBox<com.nakel.frontend.model.Cliente> cmbCliente;

    // 🔒 Variables para la paginación del ComboBox de Clientes
    private javafx.animation.PauseTransition debounceCliente;
    private int paginaActualCliente = 0;
    private String busquedaActualCliente = "";
    private boolean actualizandoCliente = false;
    //parte new arriba
    @FXML private ComboBox<String> cmbTipoFactura;

    // Buscador y Tabla Principal
    @FXML private TextField txtCodigoBarras;
    @FXML private TableView<LineaTicket> tablaTicket;

    // 🔥 NUEVO: La barra de búsqueda de la lupita (¡Asegurate de ponerle el fx:id en SceneBuilder!)
    @FXML private TextField txtBuscarInventario;
    @FXML private TableView<Articulo> tablaInventario;

    // Totales y Cobro
    @FXML private Label lblTotal;
    @FXML private ComboBox<String> cmbMedioPago;
    @FXML private javafx.scene.control.CheckBox chkRegalo;

    // Segunda tabla
    @FXML private TableColumn<Articulo, String> colInvCodigo;
    @FXML private TableColumn<Articulo, String> colInvNombre;
    @FXML private TableColumn<Articulo, Double> colInvPrecio;

    private final ArticuloApiService articuloApi = new ArticuloApiService();
    private final Gson gson = new Gson();

    @FXML private TableColumn<LineaTicket, String> colCodigo;
    @FXML private TableColumn<LineaTicket, String> colNombre;
    @FXML private TableColumn<LineaTicket, Integer> colCantidad;
    @FXML private TableColumn<LineaTicket, Double> colPrecio;
    @FXML private TableColumn<LineaTicket, Double> colSubtotal;
    @FXML private TableColumn<LineaTicket, Void> colAccion;

    @FXML private Pagination paginadorInventario;

    // 🔥 NUEVO: Listas para el buscador del inventario
    private ObservableList<Articulo> masterInventario = FXCollections.observableArrayList();
    private FilteredList<Articulo> filteredInventario;

    // 🔥 NUEVO: Lista para el buscador de clientes
    private ObservableList<String> clientesMaster = FXCollections.observableArrayList();

    // 🔥 NUEVO: Guardamos los objetos originales para sacarles el ID cuando hagamos el Vale
    private List<com.nakel.frontend.model.Cliente> listaClientesReales = new ArrayList<>();

    @FXML
    public void initialize() {
        configurarEventos();
        configurarTabla();
        configurarTablaInventario();
        cargarDatosIniciales();

        // 🔥 CONFIGURAMOS EL PAGINADOR NATIVO DEL MOSTRADOR
        if (paginadorInventario != null) {
            paginadorInventario.setPageFactory(paginaIndex -> {
                String textoBusqueda = txtCodigoBarras.getText() != null ? txtCodigoBarras.getText().trim() : "";
                cargarInventarioParaVenta(textoBusqueda, paginaIndex);

                // JavaFX exige devolver un nodo visual, pasamos una caja invisible
                return new javafx.scene.layout.VBox();
            });
        } else {
            // Fallback por si el FXML no inicializó el componente todavía
            cargarInventarioParaVenta("", 0);
        }

        System.out.println("Terminal de Punto de Venta (POS) Iniciada.");
    }

    private void configurarEventos() {
        // 🔥 1. BÚSQUEDA DINÁMICA (RAM): Filtra al instante la página actual mientras escribe
        txtCodigoBarras.textProperty().addListener((observable, oldValue, newValue) -> {
            if (filteredInventario != null) {
                filteredInventario.setPredicate(articulo -> {
                    if (newValue == null || newValue.isBlank()) return true;

                    String busqueda = newValue.toLowerCase().trim();
                    return (articulo.getNombre() != null && articulo.getNombre().toLowerCase().contains(busqueda)) ||
                            (articulo.getCodigo() != null && articulo.getCodigo().toLowerCase().contains(busqueda)) ||
                            (articulo.getCategoria() != null && articulo.getCategoria().getNombre() != null && articulo.getCategoria().getNombre().toLowerCase().contains(busqueda)) ||
                            (articulo.getMaterial() != null && articulo.getMaterial().getNombre() != null && articulo.getMaterial().getNombre().toLowerCase().contains(busqueda));
                });
            }
        });

        // 🔥 2. BÚSQUEDA GLOBAL (BACKEND): ENTER para ir a buscar a la Base de Datos
        txtCodigoBarras.setOnAction(event -> {
            String texto = txtCodigoBarras.getText() != null ? txtCodigoBarras.getText().trim() : "";
            // Reseteamos visualmente a la página 1 (índice 0) y disparamos la búsqueda
            if (paginadorInventario != null) {
                paginadorInventario.setCurrentPageIndex(0);
            }
            cargarInventarioParaVenta(texto, 0);
        });

        // 3. Doble click → agregar
        tablaInventario.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Articulo seleccionado = tablaInventario.getSelectionModel().getSelectedItem();
                if (seleccionado != null) {
                    agregarAlTicket(seleccionado);
                }
            }
        });

        // 4. ENTER en tabla → agregar
        tablaInventario.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Articulo seleccionado = tablaInventario.getSelectionModel().getSelectedItem();
                if (seleccionado != null) {
                    agregarAlTicket(seleccionado);
                }
                event.consume();
            }
        });

        // 5. ESC → limpiar
        Platform.runLater(() -> {
            if (txtCodigoBarras.getScene() != null) {
                txtCodigoBarras.getScene().setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        limpiarBusquedaInventario(null);
                    }
                });
            }
        });
    }

    private void procesarBusquedaInventario() {
        String texto = txtCodigoBarras.getText();

        if (texto == null || texto.isBlank()) {
            return;
        }

        String busqueda = texto.trim();
        System.out.println("🕵️‍♂️ [DEBUG] Pistola Láser / Lupa disparó la búsqueda en el Backend: '" + busqueda + "'");

        try {
            List<Articulo> resultados = articuloApi.buscarParaVenta(busqueda, 0, 100);

            System.out.println("🕵️‍♂️ [DEBUG] Búsqueda finalizada. Resultados: " + (resultados != null ? resultados.size() : "null"));

            if (resultados == null || resultados.isEmpty()) {
                if (paginadorInventario != null) paginadorInventario.setPageCount(1);
                mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Sin Coincidencias",
                        "No se encontró ningún producto disponible que coincida con: " + texto
                );
                return;
            }

            masterInventario.setAll(resultados);

            // 🔥 ACTUALIZAMOS EL PAGINADOR TAMBIÉN AL BUSCAR CON LA LUPA/PISTOLA
            int paginasReales = articuloApi.getUltimasPaginasMostrador();
            if (paginadorInventario != null) {
                paginadorInventario.setPageCount(paginasReales);
            }

            filteredInventario = new FilteredList<>(masterInventario, p -> true);
            tablaInventario.setItems(filteredInventario);

            if (resultados.size() == 1) {
                agregarAlTicket(resultados.get(0));
                txtCodigoBarras.clear();
                return;
            }

            tablaInventario.requestFocus();
            tablaInventario.getSelectionModel().selectFirst();

        } catch (Exception e) {
            System.out.println("❌ Error al buscar productos para venta: " + e.getMessage());

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error de búsqueda",
                    "No se pudo realizar la búsqueda del producto."
            );
        }
    }

    @FXML
    public void buscarProducto(ActionEvent event) {
        procesarBusquedaInventario();
    }
    // 🧹 Botón para limpiar la búsqueda del inventario
    @FXML
    public void limpiarBusquedaInventario(ActionEvent event) {
        txtCodigoBarras.clear();

        cargarInventarioParaVenta("", 0);

        txtCodigoBarras.requestFocus();
    }

    private void configurarTabla() {
        tablaTicket.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colCodigo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getArticulo().getCodigo()));

        colNombre.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getArticulo().getNombre()));

        colPrecio.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getArticulo().getPrecio()).asObject());

        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));

        colCantidad.setCellFactory(param -> new TableCell<LineaTicket, Integer>() {
            private final Button btnMenos = new Button("-");
            private final Label lblCantidad = new Label();
            private final Button btnMas = new Button("+");
            private final javafx.scene.layout.HBox panel =
                    new javafx.scene.layout.HBox(8, btnMenos, lblCantidad, btnMas);

            {
                panel.setAlignment(javafx.geometry.Pos.CENTER);

                btnMenos.getStyleClass().add("btn-cantidad-accion");
                btnMas.getStyleClass().add("btn-cantidad-accion");

                btnMas.setOnAction(e -> {
                    LineaTicket linea = getTableView().getItems().get(getIndex());

                    if (linea.getCantidad() < linea.getArticulo().getStockActual()) {
                        linea.setCantidad(linea.getCantidad() + 1);
                        getTableView().refresh();
                        actualizarTotal();
                    }
                });

                btnMenos.setOnAction(e -> {
                    LineaTicket linea = getTableView().getItems().get(getIndex());

                    if (linea.getCantidad() > 1) {
                        linea.setCantidad(linea.getCantidad() - 1);
                        getTableView().refresh();
                        actualizarTotal();
                    }
                });
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    lblCantidad.setText(String.valueOf(item));
                    setGraphic(panel);
                }
            }
        });

        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        colAccion.setCellFactory(param -> new TableCell<LineaTicket, Void>() {
            private final Button btnEliminar = new Button("❌");

            {
                btnEliminar.getStyleClass().add("btn-eliminar");

                btnEliminar.setOnAction(e -> {
                    LineaTicket linea = getTableView().getItems().get(getIndex());

                    if (linea != null) {
                        tablaTicket.getItems().remove(linea);
                        actualizarTotal();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnEliminar);
                }
            }
        });
    }

    // 🧮 Método para sumar los precios de la tabla del ticket
    private double obtenerTotalNumerico() {
        // Recorre LineaTicket y suma los subtotales reales (precio * cantidad)
        return tablaTicket.getItems().stream()
                .mapToDouble(LineaTicket::getSubtotal)
                .sum();
    }


    private void configurarTablaInventario() {
        tablaInventario.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colInvCodigo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getCodigo()));

        colInvNombre.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getNombre()));

        colInvPrecio.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getPrecio()).asObject());
    }

    private void cargarDatosIniciales() {
        // 1. SOLUCIÓN COMPROBANTES Y MEDIOS DE PAGO
        cmbTipoFactura.getItems().addAll("Ticket de Venta", "Factura A (Con IVA)", "Presupuesto");
        cmbTipoFactura.setValue("Ticket de Venta");

        cmbMedioPago.getItems().addAll("Efectivo", "Transferencia", "MercadoPago", "Tarjeta de Crédito", "Tarjeta de Débito", "Pago Mixto");
        cmbMedioPago.setValue("Efectivo");

        // 2. Encendemos el buscador inteligente de Clientes
        configurarBuscadorCliente();
    }

    private void configurarBuscadorCliente() {
        cmbCliente.setEditable(true);
        cmbCliente.setVisibleRowCount(10);

        // 1. Convertidor visual: Para que en pantalla diga "Juan - 20334455"
        cmbCliente.setConverter(new javafx.util.StringConverter<com.nakel.frontend.model.Cliente>() {
            @Override
            public String toString(com.nakel.frontend.model.Cliente c) {
                if (c == null) return "";
                if (c.getId() != null && c.getId() == -999L) return c.getNombre(); // Botón fantasma
                return c.getNombre() + (c.getCuit() != null && !c.getCuit().isBlank() ? " - " + c.getCuit() : "");
            }
            @Override
            public com.nakel.frontend.model.Cliente fromString(String string) {
                return cmbCliente.getSelectionModel().getSelectedItem();
            }
        });

        debounceCliente = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));

        // 2. Escuchador de tipeo en vivo
        cmbCliente.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (actualizandoCliente) return;

            if (newVal == null || newVal.trim().isEmpty()) {
                cmbCliente.hide();
                return;
            }

            debounceCliente.stop();
            debounceCliente.setOnFinished(e -> {
                busquedaActualCliente = newVal.trim();
                paginaActualCliente = 0; // Volvemos a la página 0 al buscar algo nuevo

                actualizandoCliente = true;
                cmbCliente.getItems().clear();
                actualizandoCliente = false;

                buscarYMostrarCliente(busquedaActualCliente, paginaActualCliente);
            });
            debounceCliente.playFromStart();
        });

        // 3. Escuchador de Selección (El Botón Fantasma)
        cmbCliente.setOnAction(event -> {
            if (actualizandoCliente) return;

            com.nakel.frontend.model.Cliente seleccionado = cmbCliente.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {

                // 🔥 SI TOCÓ EL BOTÓN FALSO DE "SIGUIENTE"
                if (seleccionado.getId() != null && seleccionado.getId() == -999L) {
                    paginaActualCliente++;

                    actualizandoCliente = true;
                    cmbCliente.getItems().remove(seleccionado);
                    actualizandoCliente = false;

                    buscarYMostrarCliente(busquedaActualCliente, paginaActualCliente);
                    return;
                }

                actualizandoCliente = true;
                cmbCliente.getEditor().setText(seleccionado.getNombre());
                actualizandoCliente = false;
            }
        });

        // 4. Carga inicial: Consumidor Final
        com.nakel.frontend.model.Cliente consumidorFinal = new com.nakel.frontend.model.Cliente();
        consumidorFinal.setId(0L);
        consumidorFinal.setNombre("Consumidor Final");
        consumidorFinal.setCuit("00000000");

        cmbCliente.getItems().add(consumidorFinal);
        cmbCliente.setValue(consumidorFinal);
    }

    private void buscarYMostrarCliente(String texto, int pagina) {
        // 📡 Viajamos al Backend (Asegurate de tener este método en ClienteApiService)
        com.nakel.frontend.service.ClienteApiService clienteApi = new com.nakel.frontend.service.ClienteApiService();
        java.util.List<com.nakel.frontend.model.Cliente> resultados = clienteApi.buscarClientesPaginados(texto, pagina, 20);

        actualizandoCliente = true; // 🔒 CERRAMOS
        try {
            if (resultados != null && !resultados.isEmpty()) {
                cmbCliente.getItems().addAll(resultados);

                // Inyectamos el Botón Fantasma si vinieron 20 exactos
                if (resultados.size() == 20) {
                    com.nakel.frontend.model.Cliente btnSiguiente = new com.nakel.frontend.model.Cliente();
                    btnSiguiente.setId(-999L);
                    btnSiguiente.setNombre("--- Cargar otros 20 --->");
                    cmbCliente.getItems().add(btnSiguiente);
                }

                cmbCliente.show();
            } else if (pagina == 0) {
                cmbCliente.hide();
            }

            cmbCliente.getEditor().setText(texto);
            cmbCliente.getEditor().positionCaret(texto.length());

        } finally {
            actualizandoCliente = false; // 🔓 ABRIMOS
        }
    }

    private void cargarInventarioParaVenta(String busqueda, int pagina) {
        System.out.println("🕵️‍♂️ [DEBUG] Mostrador pidiendo página " + pagina + " al Backend (Búsqueda: '" + busqueda + "')...");

        try {
            // Llamamos al método del API service enviando la búsqueda, la página actual y el tamaño por página (100)
            List<Articulo> inventario = articuloApi.buscarParaVenta(busqueda, pagina, 100);

            masterInventario.clear();

            if (inventario != null && !inventario.isEmpty()) {
                masterInventario.setAll(inventario);

                // 🔥 LA MAGIA VISUAL: Le avisamos al componente cuántas páginas existen
                int paginasReales = articuloApi.getUltimasPaginasMostrador();
                if (paginadorInventario != null) {
                    paginadorInventario.setPageCount(paginasReales);
                }

                System.out.println("✅ Mostrador listo: " + inventario.size() + " productos en pantalla (Total de páginas: " + paginasReales + ").");
            } else {
                if (paginadorInventario != null) {
                    paginadorInventario.setPageCount(1);
                }
                System.out.println("⚠️ El inventario está vacío en esta página.");
            }

            filteredInventario = new FilteredList<>(masterInventario, p -> true);
            tablaInventario.setItems(filteredInventario);

        } catch (Exception e) {
            System.out.println("❌ Error al cargar inventario paginado en mostrador: " + e.getMessage());
        }
    }

    // 🔥 1. Agregamos el Vale como segundo parámetro
    private void ejecutarProcesoDeCierreDeVenta(List<Pago> listaPagos, com.nakel.frontend.model.Vale valeUsado) {
        if (tablaTicket.getItems().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Ticket vacío", "No hay productos en el mostrador para cobrar.");
            return;
        }

        // 1. Convertimos los ítems de la tabla visual a DetalleVenta
        List<DetalleVenta> detalles = new ArrayList<>();
        for (LineaTicket linea : tablaTicket.getItems()) {
            detalles.add(new DetalleVenta(
                    linea.getCantidad(),
                    linea.getArticulo().getPrecio(),
                    linea.getSubtotal(),
                    linea.getArticulo()
            ));
        }

        // 2. Atrapar el Cliente directo del ComboBox (¡Chau split de Strings!)
        com.nakel.frontend.model.Cliente clienteSeleccionado = cmbCliente.getValue();
        com.nakel.frontend.model.Cliente clienteParaBackend;

        if (clienteSeleccionado != null && clienteSeleccionado.getId() != null && clienteSeleccionado.getId() > 0) {
            // Es un cliente real traído de la base de datos
            clienteParaBackend = clienteSeleccionado;
        } else {
            // Fallback para Consumidor Final genérico
            clienteParaBackend = new com.nakel.frontend.model.Cliente();
            clienteParaBackend.setNombre("Consumidor Final");
            clienteParaBackend.setCuit("00000000");
        }

        // 3. Armar la entidad Venta
        com.nakel.frontend.model.Venta ventaFinal = new com.nakel.frontend.model.Venta(
                clienteParaBackend,
                obtenerTotalNumerico(),
                true,  // esFiscal
                false, // esTicketCambio (es venta original)
                detalles,
                listaPagos
        );

        // 4. Configurar banderas de regalo / comprobante
        boolean esRegalo = chkRegalo.isSelected();
        ventaFinal.setEsParaRegalo(esRegalo);
        ventaFinal.setTipoComprobante(esRegalo ? "TICKET_REGALO" : "TICKET_NORMAL");

        // 5. Registrar en Backend
        VentaApiService apiVentas = new VentaApiService();
        boolean exito = apiVentas.registrarVenta(ventaFinal);

        if (exito) {
            // Si la venta se guardó bien y se usó un vale de cambio, lo quemamos
            if (valeUsado != null) {
                com.nakel.frontend.service.ValeApiService valeApi = new com.nakel.frontend.service.ValeApiService();
                valeApi.consumirVale(valeUsado.getCodigo());
                System.out.println("✅ Vale " + valeUsado.getCodigo() + " consumido con éxito.");
            }

            mostrarAlerta(Alert.AlertType.INFORMATION, "¡Venta Exitosa!", "La transacción se registró correctamente.");

            // 6. Limpiar mostrador para la siguiente venta
            tablaTicket.getItems().clear();
            actualizarTotal();
            chkRegalo.setSelected(false);

            // Reseteamos la selección del combo de clientes
            cmbCliente.getSelectionModel().clearSelection();
            cmbCliente.getEditor().clear();

            txtCodigoBarras.requestFocus();
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo guardar la venta en la Base de Datos.");
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    @FXML
    public void cobrarVenta(ActionEvent event) {
        String medioPago = cmbMedioPago.getValue();

        if ("Pago Mixto".equals(medioPago)) {
            abrirVentanaPagoMixto();
        } else {
            List<Pago> pagos = new ArrayList<>();
            pagos.add(new Pago(medioPago, obtenerTotalNumerico()));
            // 🔥 Pasamos "null" porque en un pago simple no se usan vales
            ejecutarProcesoDeCierreDeVenta(pagos, null);
        }
    }

    @FXML
    public void abrirClienteExpress(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/cliente-express-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Alta Exprés");
            dialog.getDialogPane().setContent(root);

            javafx.scene.control.ButtonType btnGuardar = new javafx.scene.control.ButtonType("💾 Guardar", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, javafx.scene.control.ButtonType.CANCEL);
            dialog.getDialogPane().lookupButton(btnGuardar).getStyleClass().add("btn-primario");

            ClienteExpressController controladorModal = loader.getController();

            dialog.setResultConverter(btn -> {
                if (btn == btnGuardar) return controladorModal.procesarGuardado();
                return null;
            });

            dialog.showAndWait().ifPresent(resultado -> {
                if (resultado != null) {
                    // Como el resultado que devuelve el modal de alta exprés es un String (o un Cliente,
                    // dependiendo de cómo devuelva el resultado, aquí lo adaptamos al tipo de tu ComboBox).
                    // Si cmbCliente es de tipo ComboBox<Cliente>, necesitamos pasarlo a objeto Cliente
                    // o agregarlo correctamente a los items del ComboBox:

                    // Creamos o buscamos el objeto Cliente correspondiente al texto/resultado recibido:
                    com.nakel.frontend.model.Cliente nuevoCliente = new com.nakel.frontend.model.Cliente();
                    nuevoCliente.setNombre(resultado);

                    if (!cmbCliente.getItems().contains(nuevoCliente)) {
                        cmbCliente.getItems().add(nuevoCliente);
                    }
                    cmbCliente.setValue(nuevoCliente);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void abrirVentanaPagoMixto() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/pago-mixto-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Cobro Dividido / Mixto");

            javafx.scene.control.ButtonType btnFacturar = new javafx.scene.control.ButtonType("✅ Emitir Factura", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnFacturar, javafx.scene.control.ButtonType.CANCEL);

            javafx.scene.Node botonFacturarNode = dialog.getDialogPane().lookupButton(btnFacturar);
            botonFacturarNode.getStyleClass().add("btn-primario");
            botonFacturarNode.setDisable(true);

            PagoMixtoController controladorModal = loader.getController();

            // 🔥 NUEVO: Atrapamos el ID directo desde el objeto Cliente (¡Chau split y chau For!)
            com.nakel.frontend.model.Cliente clienteSeleccionado = cmbCliente.getValue();
            Long idClienteParaVale = null;

            if (clienteSeleccionado != null && clienteSeleccionado.getId() != null && clienteSeleccionado.getId() > 0) {
                idClienteParaVale = clienteSeleccionado.getId(); // ¡Atrapamos el ID directo del objeto!
            }

            // 🔥 NUEVO: Le pasamos el idClienteParaVale al controlador del modal
            controladorModal.inicializarValores(obtenerTotalNumerico(), (javafx.scene.control.Button) botonFacturarNode, idClienteParaVale);

            dialog.getDialogPane().setContent(root);

            dialog.setResultConverter(btn -> {
                if (btn == btnFacturar && controladorModal.isPagoCompleto()) {
                    List<Pago> pagos = controladorModal.getPagosRegistrados();
                    com.nakel.frontend.model.Vale valeAplicado = controladorModal.getValeAplicado();
                    ejecutarProcesoDeCierreDeVenta(pagos, valeAplicado);
                }
                return null;
            });

            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void actualizarTotal() {
        double total = 0.0;
        for (LineaTicket item : tablaTicket.getItems()) {
            total += item.getSubtotal();
        }
        lblTotal.setText("Total: $" + String.format("%.2f", total));
    }

    private void agregarAlTicket(Articulo articulo) {
        if (articulo == null) return;

        for (LineaTicket linea : tablaTicket.getItems()) {
            if (linea.getArticulo().getCodigo().equals(articulo.getCodigo())) {
                if (linea.getCantidad() + 1 > articulo.getStockActual()) {
                    mostrarAlerta(Alert.AlertType.WARNING, "Stock Insuficiente", "Solo quedan " + articulo.getStockActual() + " unidades de " + articulo.getNombre());
                    return;
                }
                linea.sumarCantidad();
                tablaTicket.refresh();
                actualizarTotal();
                return;
            }
        }

        if (articulo.getStockActual() < 1) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin Stock", "El producto " + articulo.getNombre() + " no tiene stock disponible.");
            return;
        }

        tablaTicket.getItems().add(new LineaTicket(articulo));
        actualizarTotal();
    }
}