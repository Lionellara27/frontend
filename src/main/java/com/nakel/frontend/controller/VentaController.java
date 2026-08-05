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
    @FXML private ComboBox<String> cmbCliente;
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

        cargarDatosIniciales(); // 🔥 Ahora carga los nuevos comprobantes y el buscador de clientes
        cargarInventarioParaVenta(); // 🔥 Ahora carga la Lupita inteligente

        System.out.println("Terminal de Punto de Venta (POS) Iniciada.");
    }

    private void configurarEventos() {
        // 🔍 1. FILTRADO EN TIEMPO REAL A MEDIDA QUE ESCRIBE (Nombre, Código, Categoría, Material)
        txtCodigoBarras.textProperty().addListener((obs, oldVal, newVal) -> {
            if (filteredInventario != null) {
                filteredInventario.setPredicate(art -> {
                    if (newVal == null || newVal.isBlank()) return true;

                    String busqueda = newVal.toLowerCase().trim();

                    // Coincidencia por Código / SKU
                    boolean coincideCodigo = art.getCodigo() != null && art.getCodigo().toLowerCase().contains(busqueda);
                    // Coincidencia por Nombre
                    boolean coincideNombre = art.getNombre() != null && art.getNombre().toLowerCase().contains(busqueda);
                    // Coincidencia por Categoría
                    boolean coincideCat = art.getCategoria() != null && art.getCategoria().getNombre().toLowerCase().contains(busqueda);
                    // Coincidencia por Material
                    boolean coincideMat = art.getMaterial() != null && art.getMaterial().getNombre().toLowerCase().contains(busqueda);

                    return coincideCodigo || coincideNombre || coincideCat || coincideMat;
                });
            }
        });

        // 🔫 2. AL PRESIONAR ENTER (Pistola láser o tipeo manual)
        txtCodigoBarras.setOnAction(event -> procesarBusquedaInventario());

        // 🖱️ 3. DOBLE CLICK en la tabla de inventario filtrada
        tablaInventario.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Articulo seleccionado = tablaInventario.getSelectionModel().getSelectedItem();
                if (seleccionado != null) {
                    agregarAlTicket(seleccionado);
                }
            }
        });

        // ⌨️ 4. ENTER estando parado en la tabla de inventario
        tablaInventario.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Articulo seleccionado = tablaInventario.getSelectionModel().getSelectedItem();
                if (seleccionado != null) {
                    agregarAlTicket(seleccionado);
                }
                event.consume();
            }
        });

        // ⌨️ 5. ESCAPAR (ESC) limpia el buscador al instante
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

    // ⚡ LÓGICA DE BÚSQUEDA AL DAR ENTER
    private void procesarBusquedaInventario() {
        String texto = txtCodigoBarras.getText();
        if (texto == null || texto.isBlank()) return;

        String busquedaLimpia = texto.trim();

        // A) ¿Es un CÓDIGO DE BARRAS / SKU EXACTO? (Ej: disparó con la pistola láser)
        Articulo codigoExacto = masterInventario.stream()
                .filter(a -> a.getCodigo() != null && a.getCodigo().equalsIgnoreCase(busquedaLimpia))
                .findFirst()
                .orElse(null);

        if (codigoExacto != null) {
            // ¡PUM! Directo al ticket y limpiamos la barra para la próxima
            agregarAlTicket(codigoExacto);
            txtCodigoBarras.clear();
            return;
        }

        // B) Si NO es un código exacto, pero la tabla filtrada tiene EXACTAMENTE 1 solo resultado
        if (filteredInventario != null && filteredInventario.size() == 1) {
            agregarAlTicket(filteredInventario.get(0));
            txtCodigoBarras.clear();
            return;
        }

        // C) Si hay varios resultados (ej: buscó "mate" y hay 3 mates), la tabla de la izquierda ya se los muestra.
        // Se puede hacer foco en la tabla para que la cajera elija con las flechas o haga doble clic.
        if (filteredInventario != null && !filteredInventario.isEmpty()) {
            tablaInventario.requestFocus();
            tablaInventario.getSelectionModel().selectFirst();
        } else {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin Coincidencias", "No se encontró ningún producto que coincida con: " + texto);
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
        if (filteredInventario != null) {
            filteredInventario.setPredicate(art -> true); // Muestra todo de nuevo
        }
        txtCodigoBarras.requestFocus(); // Devuelve el foco a la barra para seguir escaneando
    }

    private void configurarTabla() {
        tablaTicket.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colCodigo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getArticulo().getCodigo()));
        colNombre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getArticulo().getNombre()));
        colPrecio.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getArticulo().getPrecio()).asObject());

        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colCantidad.setCellFactory(param -> new TableCell<LineaTicket, Integer>() {
            private final Button btnMenos = new Button("-");
            private final Label lblCantidad = new Label();
            private final Button btnMas = new Button("+");
            private final javafx.scene.layout.HBox panel = new javafx.scene.layout.HBox(8, btnMenos, lblCantidad, btnMas);

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
                if (empty || item == null) setGraphic(null);
                else {
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
                    tablaTicket.getItems().remove(linea);
                    actualizarTotal();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnEliminar);
            }
        });
    }

    private void configurarTablaInventario() {
        tablaInventario.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colInvCodigo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCodigo()));
        colInvNombre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombre()));
        colInvPrecio.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getPrecio()).asObject());
    }

    private void cargarDatosIniciales() {
        // 🔥 1. SOLUCIÓN COMPROBANTES Y MEDIOS DE PAGO
        cmbTipoFactura.getItems().addAll("Ticket de Venta", "Factura A (Con IVA)", "Presupuesto");
        cmbTipoFactura.setValue("Ticket de Venta");

        cmbMedioPago.getItems().addAll("Efectivo", "Transferencia", "MercadoPago", "Tarjeta de Crédito", "Tarjeta de Débito", "Pago Mixto");
        cmbMedioPago.setValue("Efectivo");

        // 🔥 2. CLIENTES DE VERDAD (Con parche para Paginación de Spring Boot)
        cmbCliente.setEditable(true);
        clientesMaster.clear();

        // Siempre dejamos a Consumidor Final como primera opción
        clientesMaster.add("Consumidor Final");

        try {
            com.nakel.frontend.service.ClienteApiService clienteApi = new com.nakel.frontend.service.ClienteApiService();
            String jsonClientes = clienteApi.obtenerClientes();

            if (jsonClientes != null && !jsonClientes.equals("[]")) {

                // 1. Leemos el texto de Spring Boot
                com.google.gson.JsonElement elemento = com.google.gson.JsonParser.parseString(jsonClientes);
                com.google.gson.JsonArray arrayClientes;

                // 2. ¿Viene adentro de un "Page" (paginado) o viene la lista suelta?
                if (elemento.isJsonObject() && elemento.getAsJsonObject().has("content")) {
                    arrayClientes = elemento.getAsJsonObject().getAsJsonArray("content"); // Sacamos la lista del cajón "content"
                } else if (elemento.isJsonArray()) {
                    arrayClientes = elemento.getAsJsonArray(); // Es una lista directa
                } else {
                    arrayClientes = new com.google.gson.JsonArray();
                }

                // 3. Ahora sí, convertimos el array puro a nuestra lista de Java
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<ArrayList<com.nakel.frontend.model.Cliente>>(){}.getType();
                List<com.nakel.frontend.model.Cliente> clientesReales = gson.fromJson(arrayClientes, listType);

                this.listaClientesReales = clientesReales;

                // 4. Llenamos el ComboBox
                for (com.nakel.frontend.model.Cliente c : clientesReales) {
                    clientesMaster.add(c.getNombre() + " - " + c.getCuit());
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Advertencia: No se pudieron cargar los clientes del servidor: " + e.getMessage());
        }

        FilteredList<String> clientesFiltrados = new FilteredList<>(clientesMaster, p -> true);
        cmbCliente.setItems(clientesFiltrados);
        cmbCliente.setValue("Consumidor Final");

        // Escucha lo que la cajera escribe y filtra en tiempo real
        cmbCliente.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            final String selected = cmbCliente.getSelectionModel().getSelectedItem();
            if (selected != null && selected.equals(cmbCliente.getEditor().getText())) {
                return; // Evita bugs si hace clic en una opción
            }
            clientesFiltrados.setPredicate(item -> {
                if (newVal == null || newVal.isBlank()) return true;
                return item.toLowerCase().contains(newVal.toLowerCase());
            });
            cmbCliente.show(); // Despliega la lista automáticamente al tipear
        });
    }

    private void cargarInventarioParaVenta() {
        List<Articulo> inventario = apiService.obtenerTodos();

        if (inventario != null && !inventario.isEmpty()) {

            // 🔥 LA MAGIA: Filtramos la lista para que al Mostrador
            // SOLO lleguen los artículos que tienen 1 o más de stock.
            List<Articulo> soloConStock = inventario.stream()
                    .filter(art -> art.getStockActual() > 0)
                    .toList();

            // Usamos la lista limpia para llenar la tabla
            masterInventario.setAll(soloConStock);
            filteredInventario = new FilteredList<>(masterInventario, p -> true);
            tablaInventario.setItems(filteredInventario);

            System.out.println("✅ Mostrador listo: " + soloConStock.size() + " productos disponibles para vender.");
        } else {
            System.out.println("⚠️ El inventario está vacío.");
        }
    }

    private double obtenerTotalNumerico() {
        return tablaTicket.getItems().stream()
                .mapToDouble(LineaTicket::getSubtotal)
                .sum();
    }

    // 🔥 1. Agregamos el Vale como segundo parámetro
    private void ejecutarProcesoDeCierreDeVenta(List<Pago> listaPagos, com.nakel.frontend.model.Vale valeUsado) {
        if (tablaTicket.getItems().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Ticket vacío", "No hay productos en el mostrador para cobrar.");
            return;
        }

        List<DetalleVenta> detalles = new ArrayList<>();
        for (LineaTicket linea : tablaTicket.getItems()) {
            detalles.add(new DetalleVenta(
                    linea.getCantidad(),
                    linea.getArticulo().getPrecio(),
                    linea.getSubtotal(),
                    linea.getArticulo()
            ));
        }

        String clienteSeleccionado = cmbCliente.getValue();
        com.nakel.frontend.model.Cliente clienteParaBackend = new com.nakel.frontend.model.Cliente();

        if (clienteSeleccionado != null && clienteSeleccionado.contains(" - ")) {
            String[] partes = clienteSeleccionado.split(" - ");
            clienteParaBackend.setCuit(partes[1].trim());
        } else {
            clienteParaBackend.setCuit("00000000"); // Consumidor Final genérico
        }

        com.nakel.frontend.model.Venta ventaFinal = new com.nakel.frontend.model.Venta(
                clienteParaBackend,
                obtenerTotalNumerico(),
                true,
                false, // 🔥 ACÁ PONÉ UN FALSE GIGANTE, NUNCA CHKREGALO
                detalles,
                listaPagos
        );

        ventaFinal.setEsParaRegalo(chkRegalo.isSelected());

// Y si querías guardar si era regalo o no para imprimir el papelito extra,
// a lo sumo se lo metés al campo 'tipoComprobante' (que lo agregaste hoy).
        if (chkRegalo.isSelected()) {
            ventaFinal.setTipoComprobante("TICKET_REGALO");
        } else {
            ventaFinal.setTipoComprobante("TICKET_NORMAL");
        }

        VentaApiService apiVentas = new VentaApiService();
        boolean exito = apiVentas.registrarVenta(ventaFinal);

        if (exito) {
            // 🔥 ACÁ ESTÁ LA MAGIA: Si la venta se guardó y había un vale, lo quemamos
            if (valeUsado != null) {
                com.nakel.frontend.service.ValeApiService valeApi = new com.nakel.frontend.service.ValeApiService();
                valeApi.consumirVale(valeUsado.getCodigo());
                System.out.println("✅ Vale " + valeUsado.getCodigo() + " consumido con éxito.");
            }

            mostrarAlerta(Alert.AlertType.INFORMATION, "¡Venta Exitosa!", "La transacción se registró correctamente.");
            tablaTicket.getItems().clear();
            actualizarTotal();
            chkRegalo.setSelected(false);
            cmbCliente.setValue("Consumidor Final");
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
                    if (!cmbCliente.getItems().contains(resultado)) {
                        clientesMaster.add(resultado); // 🔥 Agrega a la lista master
                    }
                    cmbCliente.setValue(resultado);
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

            // 🔥 NUEVO: Rescatamos el ID del cliente seleccionado para el Vale
            String clienteSeleccionado = cmbCliente.getValue();
            Long idClienteParaVale = null;

            if (clienteSeleccionado != null && clienteSeleccionado.contains(" - ")) {
                String cuitBuscado = clienteSeleccionado.split(" - ")[1].trim();
                for (com.nakel.frontend.model.Cliente c : listaClientesReales) {
                    if (c.getCuit().equals(cuitBuscado)) {
                        idClienteParaVale = c.getId(); // ¡Acá atrapamos el ID!
                        break;
                    }
                }
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