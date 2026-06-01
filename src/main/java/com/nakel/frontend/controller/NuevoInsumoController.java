package com.nakel.frontend.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.Node;

public class NuevoInsumoController {

    // 1. Declaramos TODOS los elementos del FXML
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private TextField txtDescripcion;

    // Los campos nuevos que arrancan invisibles
    @FXML private Label lblMedidas;
    @FXML private HBox boxMedidas;
    @FXML private TextField txtAnchoPlancha;
    @FXML private TextField txtLargoPlancha;

    @FXML private ComboBox<String> cmbUnidad;
    @FXML private TextField txtCosto;

    @FXML
    public void initialize() {
        // Llenamos las opciones de los desplegables
        cmbCategoria.getItems().addAll("Cuero / Tela", "Forrería", "Herrajes", "Mano de Obra", "Otros");
        cmbUnidad.getItems().addAll("Metros", "Unidades", "Horas");

        // 🎧 EL LISTENER: Le decimos a Java que vigile el ComboBox de Categoría
        cmbCategoria.valueProperty().addListener((observable, valorViejo, valorNuevo) -> {
            if (valorNuevo != null) {
                activarMagiaDinamica(valorNuevo);
            }
        });
    }

    // Este es el método que hace aparecer y desaparecer las cosas
    private void activarMagiaDinamica(String categoriaSeleccionada) {

        // Primero, volvemos todo a la normalidad (escondemos medidas y habilitamos descripción)
        lblMedidas.setVisible(false);
        lblMedidas.setManaged(false);
        boxMedidas.setVisible(false);
        boxMedidas.setManaged(false);

        txtDescripcion.setDisable(false);
        txtDescripcion.clear(); // Limpiamos lo que haya escrito antes por las dudas

        // Ahora evaluamos qué eligió:
        if (categoriaSeleccionada.equals("Cuero / Tela")) {
            // Hacemos aparecer Ancho y Largo
            lblMedidas.setVisible(true);
            lblMedidas.setManaged(true);
            boxMedidas.setVisible(true);
            boxMedidas.setManaged(true);

            // Sugerimos la unidad
            cmbUnidad.setValue("Metros");

        } else if (categoriaSeleccionada.equals("Mano de Obra")) {
            // Bloqueamos la descripción, la autocompletamos y seteamos "Horas"
            txtDescripcion.setText("Valor Hora Taller / Confección");
            txtDescripcion.setDisable(true); // Lo pone en gris para que no lo edite
            cmbUnidad.setValue("Horas");
        }
    }

    @FXML
    public void guardarInsumo(ActionEvent event) {
        String categoria = cmbCategoria.getValue();
        String descripcion = txtDescripcion.getText();
        String unidad = cmbUnidad.getValue();
        String costo = txtCosto.getText();

        // Atrapamos las medidas (si no puso nada, mandamos un "0")
        String ancho = (txtAnchoPlancha.getText() != null && !txtAnchoPlancha.getText().isEmpty()) ? txtAnchoPlancha.getText() : "0";
        String largo = (txtLargoPlancha.getText() != null && !txtLargoPlancha.getText().isEmpty()) ? txtLargoPlancha.getText() : "0";

        System.out.println("💾 GUARDANDO EN BASE DE DATOS:");
        System.out.println("Categoría: " + categoria);
        System.out.println("Descripción: " + descripcion);
        if (categoria.equals("Cuero / Tela")) {
            System.out.println("Medidas Plancha: " + ancho + "cm x " + largo + "cm");
        }
        System.out.println("Unidad: " + unidad + " | Costo: $" + costo);
        System.out.println("----------------------------------");

        cerrarModal(event);
    }

    @FXML
    public void cerrarModal(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}