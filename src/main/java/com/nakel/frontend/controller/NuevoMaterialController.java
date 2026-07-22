package com.nakel.frontend.controller;

import com.nakel.frontend.model.Material;
import com.nakel.frontend.service.ParametrosApiService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class NuevoMaterialController {

    @FXML
    private TextField txtNombreMaterial;

    // Usamos el servicio que ya se conecta al backend
    private final ParametrosApiService parametrosService = new ParametrosApiService();

    @FXML
    public void guardarMaterial(ActionEvent event) {
        String nombre = txtNombreMaterial.getText();

        if (nombre == null || nombre.trim().isEmpty()) {
            mostrarAlerta("Atención", "El nombre del material no puede estar vacío.", Alert.AlertType.WARNING);
            return;
        }

        try {
            Material nuevoMaterial = new Material();
            nuevoMaterial.setNombre(nombre.trim());

            // ⚠️ IMPORTANTE: Asegurate de que tu ParametrosApiService tenga este método creado
            // para enviar el POST a http://localhost:8080/api/materiales
            parametrosService.guardarMaterial(nuevoMaterial);

            mostrarAlerta("¡Excelente!", "El material se guardó correctamente.", Alert.AlertType.INFORMATION);
            cerrarModal(event);

        } catch (Exception e) {
            mostrarAlerta("Error del sistema", "No se pudo guardar el material:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void cancelar(ActionEvent event) {
        cerrarModal(event);
    }

    private void cerrarModal(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}