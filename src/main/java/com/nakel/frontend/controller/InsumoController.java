package com.nakel.frontend.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class InsumoController {

    @FXML
    private TextField txtBuscarInsumo;

    @FXML
    private TableView<?> tablaInsumos;

    @FXML
    public void initialize() {
        System.out.println("¡Módulo de Gestión de Insumos cargado con éxito!");
    }

    @FXML
    public void buscarInsumo(ActionEvent event) {
        String textoBusqueda = txtBuscarInsumo.getText();
        System.out.println("Buscando insumo en la base de datos: " + textoBusqueda);
    }

    @FXML
    public void abrirModalNuevoInsumo(ActionEvent event) {
        System.out.println("Preparando Pop-up de nuevo insumo...");

        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Nuevo Insumo");
        alerta.setHeaderText(null);
        alerta.setContentText("¡Próximamente! Acá se cargará el nombre, unidad de medida y precio del material.");
        alerta.showAndWait();
    }
}