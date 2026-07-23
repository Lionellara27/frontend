package com.nakel.frontend.controller;

import com.nakel.frontend.service.UsuarioApiService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField txtUsuario;

    @FXML
    private PasswordField txtPassword;

    // 🔥 Instanciamos el servicio de conexión
    private final UsuarioApiService usuarioApiService = new UsuarioApiService();

    @FXML
    public void onIngresarClick() {

        String usuario = txtUsuario.getText().trim();
        String password = txtPassword.getText().trim();

        // 🛑 Validación rápida de campos vacíos
        if (usuario.isEmpty() || password.isEmpty()) {
            mostrarAlerta("Error de Acceso", "Por favor, complete ambos campos.");
            return;
        }

        // 🔥 ACÁ SUCEDE LA MAGIA: Le preguntamos a la base de datos a través de la API
        boolean loginExitoso = usuarioApiService.login(usuario, password);

        if (loginExitoso) {
            try {
                // Cargar pantalla principal
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/nakel/frontend/view/main-layout.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 1224, 768);

                // Cargar CSS global
                java.net.URL cssUrl = getClass().getResource("/css/nakel.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    System.out.println("⚠️ OJO: No se encontró el archivo CSS en esa ruta.");
                }

                // Obtener ventana actual
                Stage stage = (Stage) txtUsuario.getScene().getWindow();

                // Configurar ventana
                stage.setScene(scene);
                stage.setTitle("Nakel Software - Mostrador Principal");
                stage.setResizable(true);
                stage.centerOnScreen();

            } catch (IOException e) {
                mostrarAlerta("Error", "No se pudo abrir la pantalla principal: " + e.getMessage());
                e.printStackTrace();
            }

        } else {
            mostrarAlerta("Error de Acceso", "Usuario o contraseña incorrectos.");
        }
    }

    // Método auxiliar para no repetir tanto código de alertas
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}