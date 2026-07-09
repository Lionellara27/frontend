package com.nakel.frontend.controller;

import com.nakel.frontend.model.Categoria;
import com.nakel.frontend.service.CategoriaApiService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class NuevaCategoriaController {

    @FXML
    private TextField txtNombreCategoria;

    @FXML
    private ComboBox<String> cmbTipoMedicion;

    private final CategoriaApiService categoriaService = new CategoriaApiService();

    @FXML
    public void initialize() {
        // Le damos las 3 opciones lógicas que soporta tu sistema
        cmbTipoMedicion.getItems().addAll("SUPERFICIE", "UNIDAD", "TIEMPO");
    }

    @FXML
    public void guardarCategoria(ActionEvent event) {
        String nombre = txtNombreCategoria.getText();
        String medicion = cmbTipoMedicion.getValue();

        // Validaciones básicas
        if (nombre == null || nombre.trim().isEmpty()) {
            mostrarAlerta("Error", "El nombre de la categoría no puede estar vacío.");
            return;
        }
        if (medicion == null) {
            mostrarAlerta("Error", "Debe seleccionar un tipo de medición.");
            return;
        }

        try {
            // Creamos el objeto (el ID queda nulo porque la BD lo genera solo)
            Categoria nuevaCategoria = new Categoria();
            nuevaCategoria.setNombre(nombre.trim());
            nuevaCategoria.setTipoMedicion(medicion);

            // Lo mandamos al backend
            categoriaService.guardarCategoriaEnBaseDeDatos(nuevaCategoria);

            System.out.println("✅ Categoría guardada con éxito en la BD.");
            cerrarVentana(event);

        } catch (Exception e) {
            mostrarAlerta("Error del Servidor", e.getMessage());
        }
    }

    @FXML
    public void cancelar(ActionEvent event) {
        cerrarVentana(event);
    }

    private void cerrarVentana(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}