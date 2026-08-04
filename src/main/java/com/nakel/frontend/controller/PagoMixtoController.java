package com.nakel.frontend.controller;

import com.nakel.frontend.model.Pago;
import com.nakel.frontend.model.Vale;
import javafx.event.ActionEvent;
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

    // 🔥 COMPONENTES PARA EL VALE
    @FXML private TextField txtCodigoVale;
    @FXML private Label lblDescuentoVale;
    @FXML private Button btnAplicarVale;

    @FXML
    private javafx.scene.control.Button btnConfirmar;

    private double totalVenta;
    private double faltaCobrar;
    private final List<Pago> listaPagosClase = new ArrayList<>();
    private boolean pagoCompleto = false;
    private Button btnFacturarPadre;

    // 🔥 NUEVO: Acá guardamos el ID del cliente para los vales nuevos
    private Long idCliente;

    // 🔥 SERVICIO Y ESTADO DEL VALE
    private final com.nakel.frontend.service.ValeApiService valeApi = new com.nakel.frontend.service.ValeApiService();
    private Vale valeAplicado = null;

    @FXML
    public void initialize() {
        cmbMetodo.getItems().addAll("Efectivo", "Transferencia", "MercadoPago", "Tarjeta de Débito", "Tarjeta de Crédito");
    }

    // 🔥 ARREGLADO: Ahora recibe los 3 argumentos (Total, Botón y el ID del Cliente)
    public void inicializarValores(double total, Button btnFacturar, Long idCliente) {
        this.totalVenta = total;
        this.faltaCobrar = total;
        this.btnFacturarPadre = btnFacturar;
        this.idCliente = idCliente; // Lo guardamos

        lblTotal.setText("Total de la Venta: $ " + String.format("%.2f", totalVenta));
        lblFalta.setText("Falta Cobrar: $ " + String.format("%.2f", faltaCobrar));
    }

    @FXML
    public void aplicarVale(ActionEvent event) {
        String codigo = txtCodigoVale.getText().trim();

        if (codigo.isEmpty()) return;

        Vale vale = valeApi.validarVale(codigo);

        if (vale != null && "ACTIVO".equalsIgnoreCase(vale.getEstado())) {
            this.valeAplicado = vale;

            double montoAUsar = vale.getMonto();
            if (montoAUsar > faltaCobrar) {
                montoAUsar = faltaCobrar;
            }

            faltaCobrar -= montoAUsar;

            listaPagos.getItems().add("Vale (" + codigo + ") -> $ " + String.format("%.2f", montoAUsar));
            listaPagosClase.add(new Pago("Vale de Cambio", montoAUsar));

            if (lblDescuentoVale != null) {
                lblDescuentoVale.setText(String.format("Vale aplicado: -$ %.2f", vale.getMonto()));
                lblDescuentoVale.setStyle("-fx-text-fill: #388e3c; -fx-font-weight: bold;");
                lblDescuentoVale.setVisible(true);
            }

            txtCodigoVale.setDisable(true);
            if (btnAplicarVale != null) btnAplicarVale.setDisable(true);

            verificarPagoCompleto();

        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "El código ingresado no existe, ya fue utilizado o está vencido.");
            alert.showAndWait();
            txtCodigoVale.selectAll();
        }
    }

    @FXML
    private void agregarPago() {
        try {
            if (txtMonto.getText().isEmpty() || cmbMetodo.getValue() == null) return;

            double monto = Double.parseDouble(txtMonto.getText());

            if (monto <= 0) return;

            // 🔥 NUEVO: Si paga con más plata de la que debe, generamos el vuelto en Vale
            if (monto > faltaCobrar) {
                double saldoAFavor = monto - faltaCobrar;

                // Llamamos al Backend para crear el vale, pasándole el ID del cliente
                Vale nuevoVale = valeApi.generarVale(saldoAFavor, this.idCliente);

                if (nuevoVale != null) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("¡Vale Generado!");
                    alert.setHeaderText("Saldo a favor por Vuelto");
                    alert.setContentText("Se generó un vale por $ " + String.format("%.2f", saldoAFavor) + "\n\nCÓDIGO: " + nuevoVale.getCodigo());
                    alert.showAndWait();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error al intentar generar el vale por el vuelto en el servidor.");
                    alert.showAndWait();
                    return; // Si falla, frenamos acá
                }

                // Para la caja, registramos que pagó justo lo que faltaba (así cuadra la contabilidad)
                listaPagos.getItems().add(cmbMetodo.getValue() + " -> $ " + String.format("%.2f", faltaCobrar) + " (Abonó $" + String.format("%.2f", monto) + ")");
                listaPagosClase.add(new Pago(cmbMetodo.getValue(), faltaCobrar));

                faltaCobrar = 0; // Saldó la cuenta
            } else {
                // Pago normal
                listaPagos.getItems().add(cmbMetodo.getValue() + " -> $ " + String.format("%.2f", monto));
                listaPagosClase.add(new Pago(cmbMetodo.getValue(), monto));
                faltaCobrar -= monto;
            }

            txtMonto.clear();
            verificarPagoCompleto();

        } catch (NumberFormatException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Por favor, ingrese un monto numérico válido.");
            alert.showAndWait();
        }
    }

    private void verificarPagoCompleto() {
        if (Math.abs(faltaCobrar) < 0.01) {
            lblFalta.setText("¡Pago Completo, listo para facturar!");
            lblFalta.setStyle("-fx-font-size: 18px; -fx-text-fill: #2e7d32; -fx-font-weight: bold;");
            cajaAgregar.setDisable(true);
            pagoCompleto = true;
            if (btnFacturarPadre != null) btnFacturarPadre.setDisable(false);
        } else {
            lblFalta.setText("Falta Cobrar: $ " + String.format("%.2f", faltaCobrar));
        }
    }

    // 🔥 ACÁ ESTÁ EL MÉTODO NUEVO QUE CIERRA LA VENTANA
    @FXML
    private void confirmarPagos() {
        if (!pagoCompleto) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Todavía falta cobrar un saldo para poder confirmar.");
            alert.showAndWait();
            return;
        }

        // Magia para cerrar este modal y devolverle el control al Modal 1
        javafx.stage.Stage stage = (javafx.stage.Stage) btnConfirmar.getScene().getWindow();
        stage.close();
    }

    public List<Pago> getPagosRegistrados() {
        return listaPagosClase;
    }

    public boolean isPagoCompleto() {
        return pagoCompleto;
    }

    public Vale getValeAplicado() {
        return valeAplicado;
    }
}