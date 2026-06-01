package com.nakel.frontend.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;

public class NuevoInsumoController {

    @FXML private TextField txtDescripcion;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ComboBox<String> cmbUnidad;
    @FXML private TextField txtCosto;

    @FXML
    public void initialize() {
        // Llenamos las opciones de Categoría
        cmbCategoria.getItems().addAll("Cuero / Tela", "Forrería", "Herrajes", "Mano de Obra", "Otros");

        // Llenamos las opciones de Medida
        cmbUnidad.getItems().addAll("Metros", "Unidades", "Horas");
    }

    @FXML
    public void guardarInsumo(ActionEvent event) {
        String descripcion = txtDescripcion.getText();
        String categoria = cmbCategoria.getValue();
        String unidad = cmbUnidad.getValue();
        String costo = txtCosto.getText();

        System.out.println("Guardando -> " + descripcion + " | " + categoria + " | " + unidad + " | $" + costo);

        // A futuro: Acá mandamos los datos a la base de datos (SQLite)

        // Una vez guardado, cerramos la ventanita
        cerrarModal(event);
    }

    @FXML
    public void cerrarModal(ActionEvent event) {
        // Lógica de JavaFX para agarrar la ventana actual y cerrarla
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}