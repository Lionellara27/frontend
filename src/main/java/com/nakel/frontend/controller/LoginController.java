package com.nakel.frontend.controller;

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

    @FXML
    public void onIngresarClick() {

        String usuario = txtUsuario.getText();
        String password = txtPassword.getText();

        if (usuario.equals("admin") && password.equals("1234")) {

            try {

                // Cargar pantalla principal
                FXMLLoader fxmlLoader = new FXMLLoader(
                        getClass().getResource(
                                "/com/nakel/frontend/view/main-layout.fxml"
                        )
                );

                Scene scene = new Scene(
                        fxmlLoader.load(),
                        1024,
                        768
                );

                // Cargar CSS global (¡Con la ruta corta y correcta!)
                java.net.URL cssUrl = getClass().getResource("/css/nakel.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                } else {
                    System.out.println("⚠️ OJO: No se encontró el archivo CSS en esa ruta.");
                }

                // Obtener ventana actual
                Stage stage = (Stage) txtUsuario
                        .getScene()
                        .getWindow();

                // Configurar ventana
                stage.setScene(scene);
                stage.setTitle("Nakel Software - Mostrador Principal");
                stage.setResizable(true);
                stage.centerOnScreen();

            } catch (IOException e) {

                Alert alerta = new Alert(Alert.AlertType.ERROR);
                alerta.setTitle("Error");
                alerta.setHeaderText("No se pudo abrir la pantalla principal");
                alerta.setContentText(e.getMessage());
                alerta.showAndWait();

                e.printStackTrace();
            }

        } else {

            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Error de Acceso");
            alerta.setHeaderText(null);
            alerta.setContentText("Usuario o contraseña incorrectos.");
            alerta.showAndWait();
        }
    }
}