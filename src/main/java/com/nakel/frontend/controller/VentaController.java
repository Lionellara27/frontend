package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.nakel.frontend.model.Articulo;
import com.nakel.frontend.model.DetalleVenta;
import com.nakel.frontend.service.ArticuloApiService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;

public class VentaController {

    // Cabecera AFIP
    @FXML private ComboBox<String> cmbCliente;
    @FXML private ComboBox<String> cmbTipoFactura;

    // Buscador y Tabla
    @FXML private TextField txtCodigoBarras;
    @FXML private TableView<Articulo> tablaTicket;

    // Totales y Cobro
    @FXML private Label lblTotal;
    @FXML private ComboBox<String> cmbMedioPago;
    //check para el regalo
    @FXML private javafx.scene.control.CheckBox chkRegalo;

    // Instanciamos el servicio y Gson para procesar
    private final ArticuloApiService articuloApi = new ArticuloApiService();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {

        configurarEventos();
        configurarTabla();
        cargarDatosIniciales();

        System.out.println("Terminal de Punto de Venta (POS) Iniciada.");
    }

    private void configurarEventos() {

        txtCodigoBarras.setOnAction(event ->
                procesarBusqueda(txtCodigoBarras.getText())
        );
    }

    private void procesarBusqueda(String texto) {

        if (texto == null || texto.isBlank()) {
            return;
        }

        System.out.println("Buscando: " + texto);

        String jsonArticulo =
                articuloApi.buscarArticuloPorCodigo(texto);

        if (jsonArticulo == null || jsonArticulo.isBlank()) {
            System.out.println("❌ Producto no encontrado");
            return;
        }

        Articulo item = gson.fromJson(jsonArticulo, Articulo.class);

        tablaTicket.getItems().add(item);

        actualizarTotal();

        txtCodigoBarras.clear();

        System.out.println("✅ Producto agregado: "
                + item.getNombre());
    }

    @FXML
    public void buscarProducto(ActionEvent event) {

        procesarBusqueda(txtCodigoBarras.getText());
    }

    private void configurarTabla() {

        tablaTicket.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void cargarDatosIniciales() {
        cmbTipoFactura.getItems().addAll("Factura C", "Remito", "Presupuesto");
        cmbTipoFactura.setValue("Factura C");

        cmbMedioPago.getItems().addAll("Efectivo", "Transferencia", "MercadoPago", "Tarjeta de Crédito", "Pago Mixto");
        cmbMedioPago.setValue("Efectivo");
    }

    //---------------------------------------------------------------------------------------------------------------------------
    private double obtenerTotalNumerico() {
        return tablaTicket.getItems().stream().mapToDouble(Articulo::getPrecio).sum();
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
            for (Articulo art : tablaTicket.getItems()) {
                // Asumimos cantidad 1 por ahora, ajustá si tenés lógica de cantidad
                detalles.add(new com.nakel.frontend.model.DetalleVenta(1, art.getPrecio(), art.getPrecio(), art));
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

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                com.nakel.frontend.service.ClienteApiService api = new com.nakel.frontend.service.ClienteApiService();
                boolean exito = api.guardarClienteEnBaseDeDatos(txtNombre.getText(), txtDni.getText(), "CONSUMIDOR_FINAL", "");

                if(exito) {
                    cmbCliente.getItems().add(txtNombre.getText() + " - " + txtDni.getText());
                    cmbCliente.getSelectionModel().selectLast();
                    System.out.println("Cliente rápido guardado y seleccionado.");
                }
                return null;
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

        // Recorremos todos los elementos que hay en la tabla
        for (Articulo item : tablaTicket.getItems()) {
            total += item.getPrecio(); // Sumamos el precio de cada artículo
        }

        // Actualizamos el label con el nuevo total formateado
        lblTotal.setText("Total: $" + String.format("%.2f", total));
    }

}