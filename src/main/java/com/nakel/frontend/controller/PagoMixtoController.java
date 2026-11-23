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

    // Cuánto del vale se usó realmente en este cobro (puede ser menos que el valor del vale)
    private double montoValeUsado = 0.0;

    // Vuelto que se convertirá en vale. NO se crea acá: lo crea la pantalla que llamó
    // recién cuando la venta quedó guardada, para no dejar vales sueltos si se cancela.
    private double vueltoPendiente = 0.0;

    public double getMontoValeUsado() { return montoValeUsado; }
    public double getVueltoPendiente() { return vueltoPendiente; }
    public Long getIdCliente() { return idCliente; }

    // Acepta "1500", "1500,50", "1.500,50" y "1500.50" (formato argentino y con punto decimal)
    static double parsearMonto(String texto) {
        String limpio = texto.trim().replace("$", "").replace(" ", "");
        if (limpio.contains(",")) {
            limpio = limpio.replace(".", "").replace(",", ".");
        } else if (limpio.matches("\\d{1,3}(\\.\\d{3})+")) {
            limpio = limpio.replace(".", ""); // "15.000" son quince mil, no quince
        }
        return Double.parseDouble(limpio);
    }

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

        // Si ya no falta cobrar nada, aplicar un vale lo gastaría sin sentido
        if (faltaCobrar < 0.01) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "El pago ya está completo: no hace falta aplicar un vale.");
            alert.showAndWait();
            return;
        }

        Vale vale = valeApi.validarVale(codigo);

        if (vale != null && "ACTIVO".equalsIgnoreCase(vale.getEstado())) {
            this.valeAplicado = vale;

            double montoAUsar = vale.getMonto();
            if (montoAUsar > faltaCobrar) {
                montoAUsar = faltaCobrar;
            }
            this.montoValeUsado = montoAUsar;

            faltaCobrar -= montoAUsar;

            listaPagos.getItems().add("Vale (" + codigo + ") -> $ " + String.format("%.2f", montoAUsar));
            listaPagosClase.add(new Pago("Vale de Cambio", montoAUsar));

            if (lblDescuentoVale != null) {
                double saldoQueQueda = vale.getMonto() - montoAUsar;
                lblDescuentoVale.setText(saldoQueQueda > 0.009
                        ? String.format("Vale aplicado: -$ %.2f (le quedan $ %.2f en el vale)", montoAUsar, saldoQueQueda)
                        : String.format("Vale aplicado: -$ %.2f", montoAUsar));
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

            double monto = parsearMonto(txtMonto.getText());

            if (monto <= 0) return;

            // Si paga con más plata de la que debe, el vuelto queda como vale.
            // El vale NO se crea todavía: se crea cuando la venta queda guardada.
            if (monto > faltaCobrar + 0.009) {
                double saldoAFavor = monto - faltaCobrar;

                Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION);
                confirmar.setTitle("Paga de más");
                confirmar.setHeaderText("Sobran $ " + String.format("%.2f", saldoAFavor));
                confirmar.setContentText("Al confirmar la operación se va a generar un VALE por ese vuelto.\n\n"
                        + "Si preferís dar el vuelto en mano, tocá Cancelar y cargá el monto justo.");
                if (confirmar.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                    return;
                }
                vueltoPendiente += saldoAFavor;

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

        // En el mostrador este modal vive dentro de un diálogo cuya venta se registra con el botón
        // "Emitir Factura". Antes este botón verde solo cerraba la ventana y la venta NO se guardaba:
        // ahora dispara ese mismo botón, así los dos hacen lo mismo.
        if (btnFacturarPadre != null) {
            btnFacturarPadre.fire();
            return;
        }

        // En el flujo de cambios no hay botón padre: cerramos y el que llamó sigue con el cambio
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