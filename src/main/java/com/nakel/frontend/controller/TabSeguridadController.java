package com.nakel.frontend.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;

public class TabSeguridadController {

    @FXML private PasswordField txtPassActual;
    @FXML private PasswordField txtPassNueva;
    @FXML private PasswordField txtPassRepetir;

    @FXML
    public void initialize() {
        System.out.println("Pestaña Seguridad Iniciada.");
    }

    @FXML
    public void cambiarContrasena(ActionEvent event) {
        String actual = txtPassActual.getText();
        String nueva = txtPassNueva.getText();
        String repetir = txtPassRepetir.getText();

        if (actual.isEmpty() || nueva.isEmpty() || repetir.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Completá todos los campos.").showAndWait();
            return;
        }

        if (!nueva.equals(repetir)) {
            new Alert(Alert.AlertType.ERROR, "Las contraseñas nuevas no coinciden.").showAndWait();
            return;
        }

        System.out.println("Mandando a cambiar contraseña al backend...");
        new Alert(Alert.AlertType.INFORMATION, "Contraseña cambiada exitosamente (Simulación).").showAndWait();

        txtPassActual.clear();
        txtPassNueva.clear();
        txtPassRepetir.clear();
    }
}