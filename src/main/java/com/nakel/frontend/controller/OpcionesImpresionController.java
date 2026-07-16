package com.nakel.frontend.controller;

import com.nakel.frontend.model.Venta;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class OpcionesImpresionController {

    @FXML private Label lblTitulo;
    @FXML private Label lblCliente;

    private Venta ventaActual;

    public void cargarVenta(Venta venta) {
        this.ventaActual = venta;

        lblTitulo.setText("Opciones de Impresión - Venta #" + String.format("%08d", venta.getId()));

        String nombreCliente = (venta.getCliente() != null) ? venta.getCliente().getNombre() : "Consumidor Final";
        lblCliente.setText("Cliente: " + nombreCliente);
    }

    @FXML
    public void imprimirTicket(ActionEvent event) {
        System.out.println("🖨️ Enviando a impresora térmica local...");
        // TODO: Lógica de Java Print Service API
    }

    @FXML
    public void enviarCorreo(ActionEvent event) {
        System.out.println("📧 Conectando con API de correos...");
        // TODO: Lógica de envío de mail (Backend o JavaMail)
    }

    @FXML
    public void guardarPdf(ActionEvent event) {
        System.out.println("📄 Generando archivo PDF local...");
        // TODO: Lógica de Apache PDFBox / iText
    }

    @FXML
    public void cerrarModal(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}