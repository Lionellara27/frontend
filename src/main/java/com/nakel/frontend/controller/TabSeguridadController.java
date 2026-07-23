package com.nakel.frontend.controller;

import com.nakel.frontend.service.UsuarioApiService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

public class TabSeguridadController {

    // Campos de texto para el Usuario
    @FXML private TextField txtUsuarioActual;
    @FXML private TextField txtNuevoUsuario;

    // 🔥 Campos de contraseña 1 (Actual)
    @FXML private PasswordField txtPassActual;
    @FXML private TextField txtPassActualVisible;
    @FXML private ToggleButton btnVerActual;

    // 🔥 Campos de contraseña 2 (Nueva)
    @FXML private PasswordField txtPassNueva;
    @FXML private TextField txtPassNuevaVisible;
    @FXML private ToggleButton btnVerNueva;

    // 🔥 Campos de contraseña 3 (Repetir)
    @FXML private PasswordField txtPassRepetir;
    @FXML private TextField txtPassRepetirVisible;
    @FXML private ToggleButton btnVerRepetir;

    // Servicio de API
    private final UsuarioApiService usuarioApiService = new UsuarioApiService();

    @FXML
    public void initialize() {
        System.out.println("✅ Pestaña Seguridad Iniciada con función 'Ojito'.");

        // 🔥 Vinculamos mágicamente los campos ocultos con los visibles
        configurarOjito(txtPassActual, txtPassActualVisible, btnVerActual);
        configurarOjito(txtPassNueva, txtPassNuevaVisible, btnVerNueva);
        configurarOjito(txtPassRepetir, txtPassRepetirVisible, btnVerRepetir);
    }

    // Método que hace el truco del ojito
    private void configurarOjito(PasswordField oculto, TextField visible, ToggleButton boton) {
        // Vincula el texto de ambos en tiempo real para que sean idénticos
        visible.textProperty().bindBidirectional(oculto.textProperty());

        // Si el botón está apretado, muestra el texto plano. Si no, muestra los puntitos.
        visible.visibleProperty().bind(boton.selectedProperty());
        oculto.visibleProperty().bind(boton.selectedProperty().not());
    }

    @FXML
    public void cambiarContrasena(ActionEvent event) {
        // Leemos desde los PasswordField (como están vinculados, siempre tienen el texto correcto)
        String usuarioActual = txtUsuarioActual.getText().trim();
        String nuevoUsuario = txtNuevoUsuario.getText().trim();
        String actual = txtPassActual.getText().trim();
        String nueva = txtPassNueva.getText().trim();
        String repetir = txtPassRepetir.getText().trim();

        // 1. Validar campos vacíos
        if (usuarioActual.isEmpty() || nuevoUsuario.isEmpty() || actual.isEmpty() || nueva.isEmpty() || repetir.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Completá todos los campos.");
            return;
        }

        // 2. Validar que la contraseña nueva se repita correctamente
        if (!nueva.equals(repetir)) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Las contraseñas nuevas no coinciden.");
            return;
        }

        System.out.println("Mandando a cambiar credenciales al backend...");

        // 3. Pegarle al Backend
        boolean exito = usuarioApiService.actualizarCredenciales(usuarioActual, nuevoUsuario, nueva);

        if (exito) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "¡Usuario y contraseña actualizados correctamente!");
            limpiarCampos();
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrió un problema al guardar las credenciales.");
        }
    }

    // Método auxiliar para limpiar los campos después del éxito
    private void limpiarCampos() {
        txtUsuarioActual.clear();
        txtNuevoUsuario.clear();
        txtPassActual.clear();
        txtPassNueva.clear();
        txtPassRepetir.clear();

        // 🔥 Desmarcamos los ojitos por si la clienta los dejó apretados
        btnVerActual.setSelected(false);
        btnVerNueva.setSelected(false);
        btnVerRepetir.setSelected(false);
    }

    // Método auxiliar para no repetir código de alertas
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo, mensaje);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.showAndWait();
    }
}