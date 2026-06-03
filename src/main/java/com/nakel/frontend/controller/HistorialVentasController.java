package com.nakel.frontend.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class HistorialVentasController {

    @FXML private TextField txtBuscarVenta;
    @FXML private TableView<?> tablaVentas;
    @FXML private Label lblTotalFacturado;

    @FXML
    public void initialize() {
        System.out.println("Historial de Ventas listo.");
        tablaVentas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    @FXML
    public void abrirPuntoDeVenta(ActionEvent event) {
        System.out.println("🚀 Saltando al Mostrador usando el Router...");

        // ¡Magia! Llamamos al Router para que haga el trabajo sucio
        com.nakel.frontend.util.Navegador.cargarVista("/com/nakel/frontend/view/venta-view.fxml");
    }
}