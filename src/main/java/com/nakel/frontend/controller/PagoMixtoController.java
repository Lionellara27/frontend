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

    // Acepta "1500", "1500,50", "1.500,50" y "1500.50". La lectura es la misma en todo el sistema.
    static double parsearMonto(String texto) {
        return com.nakel.frontend.util.FormatoMoneda.leer(texto);
    }

    // Tilde que habilita la sección del vale. Destildado, no se puede escribir ni aplicar un vale.
    @FXML private CheckBox chkUsarVale;
    @FXML private HBox cajaVale;

    @FXML
    public void initialize() {
        cmbMetodo.getItems().addAll("Efectivo", "Transferencia", "MercadoPago", "Tarjeta de Débito", "Tarjeta de Crédito");

        // El monto se formatea solo mientras se escribe: 34000 -> 34.000 ; 34000.04 -> 34.000,04
        com.nakel.frontend.util.FormatoMoneda.aplicar(txtMonto);

        // El código de vale tiene formato fijo: "VALE-" + 5 caracteres. No se puede escribir cualquier cosa.
        com.nakel.frontend.util.FormatoVale.aplicar(txtCodigoVale);

        if (chkUsarVale != null && cajaVale != null) {
            cajaVale.setDisable(true);
            chkUsarVale.selectedProperty().addListener((obs, antes, tildado) -> {
                // Con un vale ya aplicado no se puede destildar: ese vale ya forma parte del cobro
                if (valeAplicado != null) {
                    if (!tildado) chkUsarVale.setSelected(true);
                    return;
                }
                cajaVale.setDisable(!tildado);
                if (tildado) {
                    txtCodigoVale.requestFocus();
                    txtCodigoVale.end(); // cursor después de "VALE-", listo para escribir los 5 caracteres
                } else {
                    txtCodigoVale.clear();
                }
            });
        }
    }

    // 🔥 ARREGLADO: Ahora recibe los 3 argumentos (Total, Botón y el ID del Cliente)
    public void inicializarValores(double total, Button btnFacturar, Long idCliente) {
        this.totalVenta = total;
        this.faltaCobrar = total;
        this.btnFacturarPadre = btnFacturar;
        this.idCliente = idCliente; // Lo guardamos

        lblTotal.setText("Total de la Venta: " + com.nakel.frontend.util.FormatoMoneda.pesos(totalVenta));
        lblFalta.setText("Falta Cobrar: " + com.nakel.frontend.util.FormatoMoneda.pesos(faltaCobrar));
    }

    @FXML
    public void aplicarVale(ActionEvent event) {
        String codigo = txtCodigoVale.getText().trim();

        if (!com.nakel.frontend.util.FormatoVale.esCompleto(codigo)) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "El código del vale está incompleto.\n\nSon 5 letras o números después de \"VALE-\". Ejemplo: VALE-C44F4");
            alert.setTitle("Código de vale incompleto");
            alert.setHeaderText(null);
            alert.showAndWait();
            txtCodigoVale.requestFocus();
            txtCodigoVale.end();
            return;
        }

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

            listaPagos.getItems().add("Vale (" + codigo + ") -> " + com.nakel.frontend.util.FormatoMoneda.pesos(montoAUsar));
            listaPagosClase.add(new Pago("Vale de Cambio", montoAUsar));

            if (lblDescuentoVale != null) {
                double saldoQueQueda = vale.getMonto() - montoAUsar;
                lblDescuentoVale.setText(saldoQueQueda > 0.009
                        ? "Vale aplicado: -" + com.nakel.frontend.util.FormatoMoneda.pesos(montoAUsar)
                            + " (le quedan " + com.nakel.frontend.util.FormatoMoneda.pesos(saldoQueQueda) + " en el vale)"
                        : "Vale aplicado: -" + com.nakel.frontend.util.FormatoMoneda.pesos(montoAUsar));
                lblDescuentoVale.setStyle("-fx-text-fill: #388e3c; -fx-font-weight: bold;");
                lblDescuentoVale.setVisible(true);
                lblDescuentoVale.setManaged(true);
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
                confirmar.setHeaderText("Sobran " + com.nakel.frontend.util.FormatoMoneda.pesos(saldoAFavor));
                confirmar.setContentText("Al confirmar la operación se va a generar un VALE por ese vuelto.\n\n"
                        + "Si preferís dar el vuelto en mano, tocá Cancelar y cargá el monto justo.");
                if (confirmar.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                    return;
                }
                vueltoPendiente += saldoAFavor;

                // Para la caja, registramos que pagó justo lo que faltaba (así cuadra la contabilidad)
                listaPagos.getItems().add(cmbMetodo.getValue() + " -> " + com.nakel.frontend.util.FormatoMoneda.pesos(faltaCobrar) + " (Abonó " + com.nakel.frontend.util.FormatoMoneda.pesos(monto) + ")");
                listaPagosClase.add(new Pago(cmbMetodo.getValue(), faltaCobrar));

                faltaCobrar = 0; // Saldó la cuenta
            } else {
                // Pago normal
                listaPagos.getItems().add(cmbMetodo.getValue() + " -> " + com.nakel.frontend.util.FormatoMoneda.pesos(monto));
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
            // Con el pago completo tampoco tiene sentido seguir cargando vales
            if (cajaVale != null) cajaVale.setDisable(true);
            if (chkUsarVale != null) chkUsarVale.setDisable(true);
            pagoCompleto = true;
            if (btnFacturarPadre != null) btnFacturarPadre.setDisable(false);
        } else {
            lblFalta.setText("Falta Cobrar: " + com.nakel.frontend.util.FormatoMoneda.pesos(faltaCobrar));
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