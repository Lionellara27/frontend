package com.nakel.frontend.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;

public class NuevoProveedorController {

    @FXML private TextField txtEmpresa;
    @FXML private TextField txtContacto;
    @FXML private ComboBox<String> cmbRubro;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;

    @FXML
    public void initialize() {
        // Llenamos rubros clásicos de marroquinería/taller
        cmbRubro.getItems().addAll("Cueros y Telas", "Herrajes", "Hilos y Cierres", "Maquinaria", "Varios");
    }

    @FXML
    public void guardarProveedor(ActionEvent event) {
        System.out.println("💾 GUARDANDO PROVEEDOR:");
        System.out.println("Empresa: " + txtEmpresa.getText());
        System.out.println("Contacto: " + txtContacto.getText());
        System.out.println("Rubro: " + cmbRubro.getValue());
        System.out.println("Tel: " + txtTelefono.getText() + " | Email: " + txtEmail.getText());

        // A futuro: lógica de guardado en base de datos

        cerrarModal(event);
    }

    @FXML
    public void cerrarModal(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}