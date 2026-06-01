package com.nakel.frontend.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class CalculadoraController {

    @FXML private ComboBox<String> cmbInsumo;
    @FXML private TextField txtCantidad;
    @FXML private TableView<?> tablaPresupuesto;

    @FXML private Label lblCostoTotal;
    @FXML private TextField txtMargen;
    @FXML private Label lblPrecioFinal;

    @FXML
    public void initialize() {
        System.out.println("¡Calculadora de Producción lista para operar!");
        // A futuro: Cargar los insumos desde la base de datos al ComboBox
    }

    @FXML
    public void agregarInsumoLista(ActionEvent event) {
        String insumo = cmbInsumo.getValue();
        String cantidad = txtCantidad.getText();
        System.out.println("Agregando al presupuesto: " + cantidad + " de " + insumo);
        // A futuro: Agregar fila a la tabla y recalcular lblCostoTotal
    }

    @FXML
    public void guardarPresupuesto(ActionEvent event) {
        System.out.println("Guardando presupuesto final...");
        // A futuro: Guardar como PDF o en el historial
    }
}