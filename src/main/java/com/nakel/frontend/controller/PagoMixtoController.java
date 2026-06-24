package com.nakel.frontend.controller;

import com.nakel.frontend.model.Pago;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import java.util.ArrayList;
import java.util.List;

public class PagoMixtoController {

    @FXML private Label lblTotal;
    @FXML private Label lblFalta;
    @FXML private TextField txtMonto;
    @FXML private ComboBox<String> cmbMetodo;
    @FXML private HBox cajaAgregar;
    @FXML private ListView<String> listaPagos;

    private double totalVenta;
    private double faltaCobrar;
    private final List<Pago> listaPagosClase = new ArrayList<>();
    private boolean pagoCompleto = false;
    private Button btnFacturarPadre; // Referencia al botón del diálogo

    @FXML
    public void initialize() {
        cmbMetodo.getItems().addAll("Efectivo", "Transferencia", "MercadoPago", "Tarjeta de Débito", "Tarjeta de Crédito");
    }

    // 🔥 EL TRUCO: Método para inyectarle el total real desde el Mostrador
    public void inicializarValores(double total, Button btnFacturar) {
        this.totalVenta = total;
        this.faltaCobrar = total;
        this.btnFacturarPadre = btnFacturar;

        lblTotal.setText("Total de la Venta: $ " + String.format("%.2f", totalVenta));
        lblFalta.setText("Falta Cobrar: $ " + String.format("%.2f", faltaCobrar));
    }

    @FXML
    private void agregarPago() {
        try {
            if (txtMonto.getText().isEmpty() || cmbMetodo.getValue() == null) return;

            double monto = Double.parseDouble(txtMonto.getText());

            if (monto <= 0) return;

            // Protección: No dejar cobrar de más
            if (monto > faltaCobrar) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "El monto ingresado supera lo que falta cobrar.");
                alert.showAndWait();
                return;
            }

            // Registrar visualmente
            listaPagos.getItems().add(cmbMetodo.getValue() + " -> $ " + String.format("%.2f", monto));

            // Registrar en la lista de objetos puros que irán al Backend
            listaPagosClase.add(new Pago(cmbMetodo.getValue(), monto));

            faltaCobrar -= monto;
            txtMonto.clear();

            // Verificamos si liquidó la cuenta
            if (Math.abs(faltaCobrar) < 0.01) { // Usamos margen de error decimal por los dobles
                lblFalta.setText("¡Pago Completo, listo para facturar!");
                lblFalta.setStyle("-fx-font-size: 18px; -fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                cajaAgregar.setDisable(true);
                pagoCompleto = true;
                if (btnFacturarPadre != null) btnFacturarPadre.setDisable(false); // Habilitamos el botón de confirmación
            } else {
                lblFalta.setText("Falta Cobrar: $ " + String.format("%.2f", faltaCobrar));
            }

        } catch (NumberFormatException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Por favor, ingrese un monto numérico válido.");
            alert.showAndWait();
        }
    }

    public List<Pago> getPagosRegistrados() {
        return listaPagosClase;
    }

    public boolean isPagoCompleto() {
        return pagoCompleto;
    }
}