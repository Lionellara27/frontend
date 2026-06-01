package com.nakel.frontend.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class VentaController {

    // Cabecera AFIP
    @FXML private ComboBox<String> cmbCliente;
    @FXML private ComboBox<String> cmbTipoFactura;

    // Buscador y Tabla
    @FXML private TextField txtCodigoBarras;
    @FXML private TableView<?> tablaTicket;

    // Totales y Cobro
    @FXML private Label lblTotal;
    @FXML private ComboBox<String> cmbMedioPago;

    @FXML
    public void initialize() {
        System.out.println("Terminal de Punto de Venta (POS) Iniciada.");

        // Configuramos la tabla para que no deje espacios grises
        tablaTicket.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Llenamos los datos de prueba
        cmbCliente.getItems().addAll("Consumidor Final", "María López", "Curtiembre San José");
        cmbCliente.setValue("Consumidor Final"); // Por defecto

        cmbTipoFactura.getItems().addAll("Factura C", "Remito (No Válido como Factura)", "Presupuesto");
        cmbTipoFactura.setValue("Factura C"); // Por defecto

        cmbMedioPago.getItems().addAll("Efectivo", "Transferencia", "MercadoPago", "Tarjeta de Crédito");
        cmbMedioPago.setValue("Efectivo"); // Por defecto
    }

    @FXML
    public void buscarProducto(ActionEvent event) {
        String busqueda = txtCodigoBarras.getText();
        System.out.println("Buscando producto en catálogo: " + busqueda);
        // A futuro: Buscar en BD, agregarlo a la tablaTicket y sumar al lblTotal
    }

    @FXML
    public void cobrarVenta(ActionEvent event) {
        System.out.println("=====================================");
        System.out.println("💸 PROCESANDO COBRO Y FACTURACIÓN");
        System.out.println("Cliente: " + cmbCliente.getValue());
        System.out.println("Comprobante: " + cmbTipoFactura.getValue());
        System.out.println("Medio de Pago: " + cmbMedioPago.getValue());
        System.out.println("Generando conexión con AFIP...");
        System.out.println("=====================================");

        // A futuro: Limpiar la tabla y preparar para el siguiente cliente
    }
}