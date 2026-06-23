package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.nakel.frontend.model.Articulo;
import com.nakel.frontend.model.DetalleVenta;
import com.nakel.frontend.model.LineaTicket;
import com.nakel.frontend.service.ArticuloApiService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;

public class VentaController {

    private final ArticuloApiService apiService = new ArticuloApiService();

    // Cabecera AFIP
    @FXML private ComboBox<String> cmbCliente;
    @FXML private ComboBox<String> cmbTipoFactura;

    // Buscador y Tabla
    @FXML private TextField txtCodigoBarras;
    @FXML private TableView<LineaTicket> tablaTicket;
    //
    @FXML
    private TableView<Articulo> tablaInventario;

    // Totales y Cobro
    @FXML private Label lblTotal;
    @FXML private ComboBox<String> cmbMedioPago;
    //check para el regalo
    @FXML private javafx.scene.control.CheckBox chkRegalo;

    @FXML
    private TableView<Articulo> tablaProductosPOS;

    //segunda tabla
    @FXML private TableColumn<Articulo, String> colInvCodigo;
    @FXML private TableColumn<Articulo, String> colInvNombre;
    @FXML private TableColumn<Articulo, Double> colInvPrecio;

    // Instanciamos el servicio y Gson para procesar
    private final ArticuloApiService articuloApi = new ArticuloApiService();
    private final Gson gson = new Gson();

    @FXML private TableColumn<LineaTicket, String> colCodigo;
    @FXML private TableColumn<LineaTicket, String> colNombre;
    @FXML private TableColumn<LineaTicket, Integer> colCantidad;
    @FXML private TableColumn<LineaTicket, Double> colPrecio; // <--- Cambió
    @FXML private TableColumn<LineaTicket, Double> colSubtotal;
    @FXML private TableColumn<LineaTicket, Void> colAccion;

    @FXML
    public void initialize() {

        configurarEventos(); //"enchufa" EJ: el "texfield" del codigo de barra que "ProcesarBUSQUEDA"
        configurarTabla(); // logica visual7
        configurarTablaInventario();
        cargarInventarioCompleto();

        cargarDatosIniciales(); //carga los datos
        cargarInventarioParaVenta();
        System.out.println("Terminal de Punto de Venta (POS) Iniciada.");
    }

    private void configurarEventos() {

        // Buscar por código o nombre cuando presionan Enter
        txtCodigoBarras.setOnAction(event ->
                procesarBusqueda(txtCodigoBarras.getText())
        );

        /* Doble click en un producto del inventario
        tablaInventario.setOnMouseClicked(event -> {

            if (event.getClickCount() == 2) {

                Articulo seleccionado =
                        tablaInventario.getSelectionModel().getSelectedItem();

                if (seleccionado != null) {

                    tablaTicket.getItems().add(seleccionado);

                    actualizarTotal();

                    System.out.println("✅ Agregado al ticket: "
                            + seleccionado.getNombre());
                }
            }
        });*/
        // DOBLE CLICK en inventario
        tablaInventario.setOnMouseClicked(event -> {

            if (event.getClickCount() == 2) {

                Articulo seleccionado =
                        tablaInventario.getSelectionModel().getSelectedItem();

                if (seleccionado != null) {
                    agregarAlTicket(seleccionado);
                }
            }
        });

        // ENTER en inventario
        tablaInventario.setOnKeyPressed(event -> {

            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {

                Articulo seleccionado =
                        tablaInventario.getSelectionModel().getSelectedItem();

                if (seleccionado != null) {
                    agregarAlTicket(seleccionado);
                }

                event.consume(); // evita comportamiento default de TableView
            }
        });
    }

    private void procesarBusqueda(String texto) {
        if (texto == null || texto.isBlank()) return;

        System.out.println("Buscando: " + texto);

        // Llamamos una sola vez a la API
        String json = articuloApi.buscarArticuloPorCodigo(texto);
        System.out.println("DEBUG - JSON recibido: " + json);

        if (json == null || json.isBlank() || json.equals("null")) {
            System.out.println("❌ Producto no encontrado en el Backend");
            return;
        }

        try {
            Articulo item = gson.fromJson(json, Articulo.class);

            if (item.getNombre() == null) {
                System.out.println("⚠️ Objeto creado, pero el nombre está vacío...");
            } else {
                // Ya no hacemos add directo, llamamos a la función inteligente
                agregarAlTicket(item);
                txtCodigoBarras.clear();
            }
        }catch (Exception e) {
            System.out.println("❌ ERROR FATAL en GSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void buscarProducto(ActionEvent event) {

        procesarBusqueda(txtCodigoBarras.getText());
    }

    private void configurarTabla() {
        tablaTicket.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 1. Datos que están adentro del Articulo, que está adentro de LineaTicket
        colCodigo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getArticulo().getCodigo()));
        colNombre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getArticulo().getNombre()));
        colPrecio.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getArticulo().getPrecio()).asObject());

        // 2. Datos que le pertenecen directamente a la LineaTicket
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal")); // Magia de Java: llama a getSubtotal()

        // 3. 💥 ¡NUEVO! El Botón de Eliminar (El punto de tu PDF)
        colAccion.setCellFactory(param -> new TableCell<LineaTicket, Void>() {
            private final Button btnEliminar = new Button("❌");
            {
                btnEliminar.getStyleClass().add("btn-eliminar"); // Ponele estilo rojo en el CSS
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

        colInvCodigo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getCodigo())
        );

        colInvNombre.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getNombre())
        );

        colInvPrecio.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getPrecio()).asObject()
        );
    }

    private void cargarDatosIniciales() {
        cmbTipoFactura.getItems().addAll("Factura C", "Remito", "Presupuesto");
        cmbTipoFactura.setValue("Factura C");

        cmbMedioPago.getItems().addAll("Efectivo", "Transferencia", "MercadoPago", "Tarjeta de Crédito", "Pago Mixto");
        cmbMedioPago.setValue("Efectivo");
    }

    private void cargarInventarioCompleto() {
        System.out.println("Cargando inventario completo para el POS...");
        // Usamos el mismo servicio que el catálogo, porque es la misma API
        System.out.println("DEBUG - llamando backend...");
        List<Articulo> inventario = apiService.obtenerTodos();
        System.out.println("DEBUG - respuesta: " + inventario);

        if (inventario != null && !inventario.isEmpty()) {
            System.out.println("SIZE: " + inventario.size());

            // Acá podrías cargar una lista oculta o un ComboBox gigante
            // para que la clienta busque/seleccione los productos
            System.out.println("✅ Mostrador listo con " + inventario.size() + " productos.");
        } else {
            System.out.println("⚠️ El inventario está vacío.");
        }
    }

    private void cargarInventarioParaVenta() {

        List<Articulo> inventario = apiService.obtenerTodos();

        if (inventario != null && !inventario.isEmpty()) {
            System.out.println("SIZE: " + inventario.size());

            ObservableList<Articulo> listaObservable =
                    FXCollections.observableArrayList(inventario);

            tablaInventario.setItems(listaObservable);

            System.out.println("✅ Inventario cargado: "
                    + inventario.size() + " productos.");
        } else {
            System.out.println("⚠️ El inventario está vacío.");
        }
    }

    //---------------------------------------------------------------------------------------------------------------------------
    private double obtenerTotalNumerico() {
        // AHORA recorre LineaTicket y suma los subtotales reales (precio * cantidad)
        return tablaTicket.getItems().stream()
                .mapToDouble(LineaTicket::getSubtotal)
                .sum();
    }

    // 2. Así queda tu cobrarVenta modificado
    @FXML
    public void cobrarVenta(ActionEvent event) {
        String medioPago = cmbMedioPago.getValue();

        if ("Pago Mixto".equals(medioPago)) {
            abrirVentanaPagoMixto();
        } else {
            // A. Armamos los detalles
            List<DetalleVenta> detalles = new ArrayList<>();
            for (LineaTicket linea : tablaTicket.getItems()) {
                detalles.add(new com.nakel.frontend.model.DetalleVenta(
                        linea.getCantidad(),
                        linea.getArticulo().getPrecio(),
                        linea.getSubtotal(),
                        linea.getArticulo()
                ));
            }

            // B. Armamos el pago
            List<com.nakel.frontend.model.Pago> pagos = new ArrayList<>();
            pagos.add(new com.nakel.frontend.model.Pago(medioPago, obtenerTotalNumerico()));

            // C. Armamos el objeto Venta (tu molde)
            com.nakel.frontend.model.Venta venta = new com.nakel.frontend.model.Venta(
                    cmbCliente.getValue(),
                    obtenerTotalNumerico(),
                    true, // esFiscal: dejalo en true o vinculalo a tu cmbTipoFactura
                    chkRegalo.isSelected(),
                    detalles,
                    pagos
            );

            // D. Mandamos a la API
            com.nakel.frontend.service.VentaApiService api = new com.nakel.frontend.service.VentaApiService();
            boolean exito = api.registrarVenta(venta);

            if (exito) {
                System.out.println("✅ Venta enviada correctamente al Backend.");
                tablaTicket.getItems().clear();
                actualizarTotal();
            } else {
                System.out.println("❌ Error: No se pudo conectar con el Backend.");
            }
        }
    }

    @FXML
    public void abrirClienteExpress(ActionEvent event) {
        javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();

        // 1. APLICAMOS EL CSS BASE AL CONTENEDOR
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/nakel.css").toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("mostrador-container");

        dialog.setTitle("Alta Exprés");
        dialog.setHeaderText("Cargar nuevo cliente rápido");

        javafx.scene.control.ButtonType btnGuardar = new javafx.scene.control.ButtonType("💾 Guardar", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, javafx.scene.control.ButtonType.CANCEL);

        // 2. APLICAMOS EL CSS AL BOTÓN GUARDAR (Dorado Nakel)
        javafx.scene.Node botonGuardarNode = dialog.getDialogPane().lookupButton(btnGuardar);
        botonGuardarNode.getStyleClass().add("btn-primario");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new javafx.geometry.Insets(20, 20, 10, 10));

        // 3. APLICAMOS EL CSS A LOS CAMPOS DE TEXTO
        javafx.scene.control.TextField txtDni = new javafx.scene.control.TextField();
        txtDni.setPromptText("Ej: 20123456789");
        txtDni.getStyleClass().add("text-field");

        javafx.scene.control.TextField txtNombre = new javafx.scene.control.TextField();
        txtNombre.setPromptText("Nombre y Apellido");
        txtNombre.getStyleClass().add("text-field");

        grid.add(new javafx.scene.control.Label("DNI/CUIT:"), 0, 0);
        grid.add(txtDni, 1, 0);
        grid.add(new javafx.scene.control.Label("Nombre:"), 0, 1);
        grid.add(txtNombre, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // 🧠 4. ACÁ SUCEDE LA MAGIA ANTI-FRICCIÓN
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                String dni = txtDni.getText();
                String nombreIngresado = txtNombre.getText();

                com.nakel.frontend.service.ClienteApiService api = new com.nakel.frontend.service.ClienteApiService();

                // PREGUNTAMOS: ¿Pepe ya vino alguna vez?
                String clienteJson = api.buscarClientePorCuit(dni);

                if (clienteJson != null && !clienteJson.isBlank()) {
                    // 💥 ¡PEPE YA EXISTE! Lo reciclamos
                    com.nakel.frontend.model.Cliente pepeHistorico = gson.fromJson(clienteJson, com.nakel.frontend.model.Cliente.class);
                    String itemCombo = pepeHistorico.getNombre() + " - " + pepeHistorico.getCuit();

                    // Cartelito para avisar al cajero que ahorró tiempo
                    Alert alerta = new Alert(Alert.AlertType.INFORMATION);
                    alerta.setTitle("Cliente Existente");
                    alerta.setHeaderText("¡" + pepeHistorico.getNombre() + " ya estaba registrado!");
                    alerta.setContentText("Se cargará automáticamente en el mostrador.");
                    alerta.showAndWait();

                    // Lo metemos en el ComboBox si no estaba y lo seleccionamos
                    if (!cmbCliente.getItems().contains(itemCombo)) {
                        cmbCliente.getItems().add(itemCombo);
                    }
                    cmbCliente.setValue(itemCombo);

                } else {
                    // 🆕 NO EXISTE: LO CREAMOS NUEVO
                    try {
                        // Recordá: la nueva firma pide 5 datos (nombre, cuit, iva, telefono, email). Mandamos vacío lo que no tenemos.
                        api.guardarClienteEnBaseDeDatos(nombreIngresado, dni, "CONSUMIDOR_FINAL", "", "");

                        String nuevoItem = nombreIngresado + " - " + dni;
                        cmbCliente.getItems().add(nuevoItem);
                        cmbCliente.setValue(nuevoItem);
                        System.out.println("✅ Cliente rápido creado y seleccionado.");

                    } catch (Exception e) {
                        // Si el backend explota por otra cosa (ej. se cortó internet), atajamos el error acá
                        Alert error = new Alert(Alert.AlertType.ERROR);
                        error.setTitle("Error al guardar");
                        error.setHeaderText("No se pudo crear el cliente");
                        error.setContentText(e.getMessage());
                        error.showAndWait();
                    }
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void abrirVentanaPagoMixto() {
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();

        // 1. APLICAMOS EL CSS BASE (¡Esto lo hiciste perfecto!)
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/nakel.css").toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("mostrador-container");

        dialog.setTitle("Cobro Dividido / Mixto");
        dialog.setHeaderText("Agregá los pagos hasta completar el total");

        // Botón para facturar
        javafx.scene.control.ButtonType btnFacturar = new javafx.scene.control.ButtonType("✅ Emitir Factura", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnFacturar, javafx.scene.control.ButtonType.CANCEL);

        // ESTILO BOTÓN FACTURAR: Le ponemos tu clase dorada
        javafx.scene.Node botonFacturarNode = dialog.getDialogPane().lookupButton(btnFacturar);
        botonFacturarNode.getStyleClass().add("btn-primario"); // <-- FALTABA ESTO
        botonFacturarNode.setDisable(true);

        final double[] faltaCobrar = {60000.00};

        javafx.scene.control.Label lblTotal = new javafx.scene.control.Label("Total de la Venta: $ 60000.00");
        lblTotal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        javafx.scene.control.Label lblFalta = new javafx.scene.control.Label("Falta Cobrar: $ 60000.00");
        lblFalta.setStyle("-fx-font-size: 18px; -fx-text-fill: #d32f2f; -fx-font-weight: bold;");

        // ESTILO CAMPO MONTO
        javafx.scene.control.TextField txtMonto = new javafx.scene.control.TextField();
        txtMonto.setPromptText("Ej: 20000");
        txtMonto.getStyleClass().add("text-field"); // <-- FALTABA ESTO

        // ESTILO COMBOBOX Y DEBITO/CREDITO SEPARADO
        javafx.scene.control.ComboBox<String> cmbMetodo = new javafx.scene.control.ComboBox<>();
        cmbMetodo.getItems().addAll("Efectivo", "Transferencia", "MercadoPago", "Tarjeta de Débito", "Tarjeta de Crédito");
        cmbMetodo.getStyleClass().add("combo-pago"); // <-- FALTABA ESTO

        // ESTILO BOTÓN AGREGAR
        javafx.scene.control.Button btnAgregar = new javafx.scene.control.Button("➕ Agregar");
        btnAgregar.getStyleClass().add("btn-secundario"); // <-- FALTABA ESTO

        javafx.scene.layout.HBox cajaAgregar = new javafx.scene.layout.HBox(10, txtMonto, cmbMetodo, btnAgregar);

        javafx.scene.control.ListView<String> listaPagos = new javafx.scene.control.ListView<>();
        listaPagos.setPrefHeight(120);

        btnAgregar.setOnAction(e -> {
            try {
                double monto = Double.parseDouble(txtMonto.getText());
                if (monto > 0 && monto <= faltaCobrar[0] && cmbMetodo.getValue() != null) {
                    listaPagos.getItems().add(cmbMetodo.getValue() + " -> $ " + monto);
                    faltaCobrar[0] -= monto;
                    lblFalta.setText("Falta Cobrar: $ " + faltaCobrar[0]);
                    txtMonto.clear();

                    if (faltaCobrar[0] == 0) {
                        lblFalta.setText("¡Pago Completo, listo para facturar!");
                        lblFalta.setStyle("-fx-font-size: 18px; -fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                        botonFacturarNode.setDisable(false);
                        cajaAgregar.setDisable(true);
                    }
                }
            } catch (Exception ex) {
                System.out.println("El cajero escribió letras en vez de números");
            }
        });

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(15, lblTotal, lblFalta, cajaAgregar, listaPagos);
        vbox.setPadding(new javafx.geometry.Insets(20));
        dialog.getDialogPane().setContent(vbox);

        dialog.setResultConverter(btn -> {
            if (btn == btnFacturar) {
                System.out.println("=====================================");
                System.out.println("💸 FACTURANDO PAGO MIXTO CON ÉXITO");
                System.out.println("=====================================");
            }
            return null;
        });

        dialog.showAndWait();
    }

    // 🧮 Método para sumar los precios de la tabla
    private void actualizarTotal() {
        double total = 0.0;
        for (LineaTicket item : tablaTicket.getItems()) { // AHORA RECORRE LineaTicket
            total += item.getSubtotal();
        }
        lblTotal.setText("Total: $" + String.format("%.2f", total));
    }

    private void agregarAlTicket(Articulo articulo) {
        if (articulo == null) return;

        // Buscamos si el producto ya está en el ticket
        for (LineaTicket linea : tablaTicket.getItems()) {
            if (linea.getArticulo().getCodigo().equals(articulo.getCodigo())) {
                // 🔥 VALIDACIÓN DE STOCK (Deuda del PDF saldada)
                if (linea.getCantidad() + 1 > articulo.getStockActual()) {
                    System.out.println("❌ ¡NO HAY STOCK SUFICIENTE! Quedan: " + articulo.getStockActual());
                    // Acá le podrías meter un Alert de JavaFX a futuro
                    return;
                }

                linea.sumarCantidad();
                tablaTicket.refresh();
                actualizarTotal();
                return;
            }
        }

        // Si es la primera vez que lo escanea, validamos stock igual
        if (articulo.getStockActual() < 1) {
            System.out.println("❌ ESTE PRODUCTO NO TIENE STOCK (0).");
            return;
        }

        // Si no existe, lo metemos adentro de una LineaTicket nueva
        tablaTicket.getItems().add(new LineaTicket(articulo));
        actualizarTotal();
    }
}