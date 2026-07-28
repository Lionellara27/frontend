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
        // Buscar por código o nombre cuando presionan Enter en la pistola láser
        txtCodigoBarras.setOnAction(event -> procesarBusqueda(txtCodigoBarras.getText()));

        // DOBLE CLICK en inventario
        tablaInventario.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Articulo seleccionado = tablaInventario.getSelectionModel().getSelectedItem();
                if (seleccionado != null) agregarAlTicket(seleccionado);
            }
        });

        // ENTER en inventario
        tablaInventario.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Articulo seleccionado = tablaInventario.getSelectionModel().getSelectedItem();
                if (seleccionado != null) agregarAlTicket(seleccionado);
                event.consume();
            }
        });

        // 🔥 NUEVO: Atajo F3 para ir directo a la Lupita del Inventario
        Platform.runLater(() -> {
            if (txtCodigoBarras.getScene() != null) {
                txtCodigoBarras.getScene().setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.F3 && txtBuscarInventario != null) {
                        txtBuscarInventario.requestFocus();
                    }
                });
            }
        });

        // 🔥 NUEVO: Lógica de filtrado en tiempo real para la Lupita
        if (txtBuscarInventario != null) {
            txtBuscarInventario.textProperty().addListener((obs, oldVal, newVal) -> {
                if (filteredInventario != null) {
                    filteredInventario.setPredicate(art -> {
                        if (newVal == null || newVal.isBlank()) return true;
                        String busqueda = newVal.toLowerCase();
                        return art.getNombre().toLowerCase().contains(busqueda) ||
                                art.getCodigo().toLowerCase().contains(busqueda);
                    });
                }
            });
        }
    }

    private void procesarBusqueda(String texto) {
        if (texto == null || texto.isBlank()) return;

        String json = articuloApi.buscarArticuloPorCodigo(texto);

        if (json == null || json.isBlank() || json.equals("null")) {
            System.out.println("❌ Producto no encontrado en el Backend");
            return;
        }

        try {
            Articulo item = gson.fromJson(json, Articulo.class);
            if (item.getNombre() != null) {
                agregarAlTicket(item);
                txtCodigoBarras.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void buscarProducto(ActionEvent event) {
        procesarBusqueda(txtCodigoBarras.getText());
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
        // 🔥 1. SOLUCIÓN COMPROBANTES: Textos claros para la cajera
        cmbTipoFactura.getItems().addAll("Ticket de Venta", "Factura A (Con IVA)", "Presupuesto");
        cmbTipoFactura.setValue("Ticket de Venta");

        cmbMedioPago.getItems().addAll("Efectivo", "Transferencia", "MercadoPago", "Tarjeta de Crédito", "Tarjeta de Débito", "Pago Mixto");
        cmbMedioPago.setValue("Efectivo");

        // 🔥 2. SOLUCIÓN BUSCADOR DE CLIENTES (Autocompletado inteligente)
        cmbCliente.setEditable(true); // Permite escribir adentro del ComboBox

        // Simulación de carga (Nota: acá a futuro podés llamar a tu ClienteApiService)
        clientesMaster.addAll("Consumidor Final", "Marta - 44233111", "Roberto - 11222333", "Pepe - 22333444");
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
            // 🔥 3. SOLUCIÓN LUPITA: Usamos FilteredList en vez del observable pelado
            masterInventario.setAll(inventario);
            filteredInventario = new FilteredList<>(masterInventario, p -> true);
            tablaInventario.setItems(filteredInventario);
            System.out.println("✅ Inventario cargado y filtrable: " + inventario.size() + " productos.");
        } else {
            System.out.println("⚠️ El inventario está vacío.");
        }
    }

    private double obtenerTotalNumerico() {
        return tablaTicket.getItems().stream()
                .mapToDouble(LineaTicket::getSubtotal)
                .sum();
    }

    private void ejecutarProcesoDeCierreDeVenta(List<Pago> listaPagos) {
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

        // 🔥 Blindaje por si eligen Consumidor Final
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
                chkRegalo.isSelected(),
                detalles,
                listaPagos
        );

        VentaApiService apiVentas = new VentaApiService();
        boolean exito = apiVentas.registrarVenta(ventaFinal);

        if (exito) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "¡Venta Exitosa!", "La transacción se registró correctamente.");
            tablaTicket.getItems().clear();
            actualizarTotal();
            chkRegalo.setSelected(false);
            cmbCliente.setValue("Consumidor Final"); // Vuelve a cero
            txtCodigoBarras.requestFocus(); // Foco automático para escanear de nuevo
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
            ejecutarProcesoDeCierreDeVenta(pagos);
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
            controladorModal.inicializarValores(obtenerTotalNumerico(), (javafx.scene.control.Button) botonFacturarNode);

            dialog.getDialogPane().setContent(root);

            dialog.setResultConverter(btn -> {
                if (btn == btnFacturar && controladorModal.isPagoCompleto()) {
                    List<Pago> pagos = controladorModal.getPagosRegistrados();
                    ejecutarProcesoDeCierreDeVenta(pagos);
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