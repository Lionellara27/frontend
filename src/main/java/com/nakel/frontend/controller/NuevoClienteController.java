package com.nakel.frontend.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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

    @FXML
    public void initialize() {
        // Llenamos los tipos de cliente clásicos
        cmbTipoCliente.getItems().addAll("Consumidor Final", "Mayorista / Revendedor", "Empresa");

        // Dejamos uno seleccionado por defecto para agilizarle el trabajo
        cmbTipoCliente.setValue("Consumidor Final");
    }

    @FXML
    public void guardarCliente(ActionEvent event) {
        System.out.println("💾 GUARDANDO NUEVO CLIENTE:");
        System.out.println("Nombre: " + txtNombre.getText());
        System.out.println("Tipo: " + cmbTipoCliente.getValue());
        System.out.println("Tel: " + txtTelefono.getText());
        System.out.println("DNI/CUIT: " + txtDni.getText());
        System.out.println("Email: " + txtEmail.getText());
        System.out.println("----------------------------------");

        // A futuro: Acá mandamos el objeto a SQLite

        // Una vez guardado, cerramos la ventanita
        cerrarModal(event);
    }

    @FXML
    public void cerrarModal(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
