package com.nakel.frontend.controller;

import com.nakel.frontend.service.ClienteApiService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;

public class NuevoClienteController {

    @FXML private TextField txtNombre;
    @FXML private ComboBox<String> cmbTipoCliente;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtDni;
    @FXML private TextField txtEmail;

    // 🔥 NUEVOS CAMPOS DE SALDOS (Asegurate de ponerles el mismo fx:id en el FXML)
    @FXML private TextField txtSaldoAFavor;
    @FXML private TextField txtSaldoPendiente;

    @FXML private TextField txtCuit;
    @FXML private ComboBox<String> cmbIva;
    @FXML private Button btnGuardar;

    private Long idClienteEditando = null;

    private final ClienteApiService apiService = new ClienteApiService();

    @FXML
    public void initialize() {
        cmbTipoCliente.getItems().addAll("Consumidor Final", "Mayorista / Revendedor", "Empresa");
        cmbTipoCliente.setValue("Consumidor Final");
    }

    public void cargarDatosParaEditar(com.nakel.frontend.model.Cliente cliente) {
        this.idClienteEditando = cliente.getId();

        txtNombre.setText(cliente.getNombre());
        txtTelefono.setText(cliente.getTelefono());
        txtEmail.setText(cliente.getEmail());

        if (cliente.getCondicionIva() != null) {
            cmbTipoCliente.setValue(cliente.getCondicionIva());
        }

        txtDni.setText(cliente.getCuit());
        txtDni.setDisable(true);
        txtDni.setStyle("-fx-opacity: 0.7; -fx-background-color: #f0f0f0;");

        // 🔥 CARGAMOS LOS SALDOS AL ABRIR EL LAPICITO
        // Convertimos el double a texto para que entre en el TextField
        if (txtSaldoAFavor != null) {
            txtSaldoAFavor.setText(String.valueOf(cliente.getSaldoAFavor()));
        }
        if (txtSaldoPendiente != null) {
            txtSaldoPendiente.setText(String.valueOf(cliente.getSaldoPendiente()));
        }
    }

    @FXML
    public void guardarCliente(ActionEvent event) {
        if (txtNombre.getText().isBlank() || txtDni.getText().isBlank()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Datos incompletos", "El Nombre y el DNI/CUIT son obligatorios.");
            return;
        }

        try {
            System.out.println("Enviando datos al servidor...");

            // 🔥 Atrapamos los saldos de forma segura (si hay texto en blanco, ponemos 0.0)
            double saldoAFavor = 0.0;
            double saldoPendiente = 0.0;

            // Reemplazamos coma por punto por si el usuario escribe "10,50" en vez de "10.50"
            if (txtSaldoAFavor != null && !txtSaldoAFavor.getText().isBlank()) {
                saldoAFavor = Double.parseDouble(txtSaldoAFavor.getText().replace(",", "."));
            }
            if (txtSaldoPendiente != null && !txtSaldoPendiente.getText().isBlank()) {
                saldoPendiente = Double.parseDouble(txtSaldoPendiente.getText().replace(",", "."));
            }

            // 🔀 SEPARAMOS LOS CAMINOS: ¿Es Nuevo o es Edición?
            if (this.idClienteEditando == null) {
                // ES UN CLIENTE NUEVO (Los saldos arrancan en 0 en la base de datos automáticamente)
                apiService.guardarClienteEnBaseDeDatos(
                        null,
                        txtNombre.getText(),
                        txtDni.getText(),
                        cmbTipoCliente.getValue(),
                        txtTelefono.getText() != null ? txtTelefono.getText() : "",
                        txtEmail.getText() != null ? txtEmail.getText() : ""
                );
            } else {
                // ES UNA EDICIÓN (LAPICITO) - Usamos el método que creamos con los saldos
                apiService.actualizarClienteEnBaseDeDatos(
                        this.idClienteEditando,
                        txtNombre.getText(),
                        txtDni.getText(),
                        cmbTipoCliente.getValue(),
                        txtTelefono.getText() != null ? txtTelefono.getText() : "",
                        txtEmail.getText() != null ? txtEmail.getText() : "",
                        saldoAFavor, // Mandamos la plata a favor
                        saldoPendiente // Mandamos la deuda
                );
            }

            mostrarAlerta(Alert.AlertType.INFORMATION, "¡Éxito!", "Cliente guardado correctamente en la base de datos.");
            cerrarModal(event);

        } catch (NumberFormatException e) {
            // Si el usuario escribe "Hola" en el campo de saldo, atajamos la explosión
            mostrarAlerta(Alert.AlertType.WARNING, "Formato incorrecto", "Los saldos deben ser números válidos (Ej: 1500.50).");
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo guardar", e.getMessage());
        }
    }

    @FXML
    public void cerrarModal(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}