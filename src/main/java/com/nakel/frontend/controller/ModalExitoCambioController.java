package com.nakel.frontend.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

public class ModalExitoCambioController {

    @FXML private Label lblTitulo;
    @FXML private Label lblMensajePrincipal;
    @FXML private Label lblSubtitulo;
    @FXML private Label lblDetalleVale;

    // 🔥 Variables para guardar los datos en memoria y usarlos al imprimir
    private String clienteActual;
    private double saldoActual;
    private String codigoValeActual;

    public void inicializarDatos(String nombreCliente, double saldoAFavor, String codigoVale) {
        this.clienteActual = nombreCliente;
        this.saldoActual = saldoAFavor;
        this.codigoValeActual = codigoVale;

        lblTitulo.setText("¡Cambio Registrado con Éxito!");
        if (codigoVale != null && !codigoVale.isEmpty()) {
            lblMensajePrincipal.setText("Saldo a favor del cliente: $" + String.format("%.2f", saldoAFavor));
            lblSubtitulo.setText("Código de Vale generado:");
            lblDetalleVale.setText(codigoVale);
        } else {
            // Sin vale no hay saldo a favor: antes decía "acreditado en la cuenta corriente", y no se acreditaba nada
            lblMensajePrincipal.setText("El cambio quedó registrado y el stock actualizado.");
            lblSubtitulo.setText("Cliente:");
            lblDetalleVale.setText(nombreCliente);
        }
    }

    @FXML
    public void onImprimirClick(ActionEvent event) {
        System.out.println("🖨️ Enviando orden a la impresora térmica...");

        // 1. Armamos el texto plano
        StringBuilder texto = new StringBuilder();
        texto.append("------ NAKEL SOFTWARE ------\n");
        texto.append("   COMPROBANTE DE CAMBIO\n");
        texto.append("----------------------------\n");
        texto.append("Cliente: ").append(clienteActual).append("\n");
        texto.append("Saldo a Favor: $").append(String.format("%.2f", saldoActual)).append("\n");

        if(codigoValeActual != null && !codigoValeActual.isEmpty()) {
            texto.append("----------------------------\n");
            texto.append("TU CODIGO DE VALE:\n");
            texto.append(">>> ").append(codigoValeActual).append(" <<<\n");
        }

        texto.append("----------------------------\n");
        texto.append("   !Gracias por su visita!\n");
        texto.append("\n\n\n\n\n"); // 5 saltos de línea para que el papel suba y se pueda cortar

        // 2. Disparamos a la impresora térmica
        //    (antes, si no había impresora o fallaba, no avisaba nada)
        try {
            boolean enviado = com.nakel.frontend.service.ImpresoraService.imprimirTexto(texto.toString());
            if (enviado) {
                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Comprobante enviado");
                ok.setHeaderText(null);
                ok.setContentText("El comprobante se envió a la impresora: "
                        + com.nakel.frontend.service.ImpresoraService.getImpresoraGuardada());
                ok.showAndWait();
            }
        } catch (Exception e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Error de impresión");
            error.setHeaderText(null);
            error.setContentText(e.getMessage());
            error.showAndWait();
        }
    }

    @FXML
    public void onPdfClick(ActionEvent event) {
        System.out.println("📄 Generando comprobante en PDF...");

        // 1. Abrimos la ventanita nativa de Windows para guardar el archivo
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Comprobante PDF");

        // Le ponemos un nombre lindo por defecto (ej: "Cambio_Consumidor_Final.pdf")
        String nombreSugerido = "Cambio_" + clienteActual.replace(" ", "_") + ".pdf";
        fileChooser.setInitialFileName(nombreSugerido);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));

        Stage stage = (Stage) lblTitulo.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        // 2. Si la clienta no canceló y eligió donde guardarlo...
        if (file != null) {
            com.nakel.frontend.service.GeneradorPdfService.generarComprobanteCambio(
                    file.getAbsolutePath(),
                    clienteActual,
                    saldoActual,
                    codigoValeActual
            );

            // Avisito visual
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("¡PDF Generado!");
            alert.setContentText("El comprobante está listo para enviar por WhatsApp en:\n" + file.getAbsolutePath());
            alert.showAndWait();
        }
    }

    @FXML
    public void onOkClick(ActionEvent event) {
        Stage stage = (Stage) lblTitulo.getScene().getWindow();
        stage.close();
    }
}