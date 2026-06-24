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

    //nuevo
    @FXML private TextField txtCuit;
    @FXML private ComboBox<String> cmbIva;
    @FXML private Button btnGuardar;

    private Long idClienteEditando = null;

    // 🔌 1. Instanciamos nuestro servicio de conexión
    private final ClienteApiService apiService = new ClienteApiService();

    @FXML
    public void initialize() {
        // Llenamos los tipos de cliente clásicos
        cmbTipoCliente.getItems().addAll("Consumidor Final", "Mayorista / Revendedor", "Empresa");

        // Dejamos uno seleccionado por defecto para agilizarle el trabajo
        cmbTipoCliente.setValue("Consumidor Final");
    }

    //
    public void cargarDatosParaEditar(com.nakel.frontend.model.Cliente cliente) {
        // 1. Guardamos el ID para saber que estamos actualizando
        this.idClienteEditando = cliente.getId();

        // 2. Llenamos los campos
        txtNombre.setText(cliente.getNombre());
        txtTelefono.setText(cliente.getTelefono());
        txtEmail.setText(cliente.getEmail());

        // 3. Manejo seguro del ComboBox
        if (cliente.getCondicionIva() != null) {
            cmbTipoCliente.setValue(cliente.getCondicionIva());
        }

        // 4. 🔥 BLINDAJE: El DNI/CUIT no se toca en modo edición
        txtDni.setText(cliente.getCuit());
        txtDni.setDisable(true);
        txtDni.setStyle("-fx-opacity: 0.7; -fx-background-color: #f0f0f0;"); // Lo ponemos gris claro para que se note
    }

    @FXML
    public void guardarCliente(ActionEvent event) {
        // 🛡️ 2. Validación temprana: evitamos mandar basura al Backend
        if (txtNombre.getText().isBlank() || txtDni.getText().isBlank()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Datos incompletos", "El Nombre y el DNI/CUIT son obligatorios.");
            return; // Cortamos la ejecución acá
        }

        try {
            System.out.println("Enviando datos al servidor...");

            // 🚀 3. Disparamos los datos reales a la base de datos a través de la API
            apiService.guardarClienteEnBaseDeDatos(
                    txtNombre.getText(),
                    txtDni.getText(),
                    cmbTipoCliente.getValue(),
                    txtTelefono.getText() != null ? txtTelefono.getText() : "",
                    txtEmail.getText() != null ? txtEmail.getText() : ""
            );

            // ✅ 4. Si la línea anterior no tiró error, festejamos
            mostrarAlerta(Alert.AlertType.INFORMATION, "¡Éxito!", "Cliente guardado correctamente en la base de datos.");

            // Una vez guardado, cerramos la ventanita
            cerrarModal(event);

        } catch (Exception e) {
            // 🛑 5. Atajamos el error (Ej: DNI duplicado) que nos manda el Backend
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo guardar", e.getMessage());
        }
    }

    @FXML
    public void cerrarModal(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    // 🛠️ 6. Herramienta para crear cartelitos fáciles sin repetir código
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}