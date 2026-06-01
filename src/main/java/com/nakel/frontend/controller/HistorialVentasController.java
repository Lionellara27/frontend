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
        System.out.println("🚀 Saltando al Mostrador desde el Historial...");

        try {
            // Lógica para cambiar la vista principal al Punto de Venta
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/venta-view.fxml"));
            javafx.scene.Parent root = loader.load();

            // Acá asumimos que tenés forma de inyectarlo en tu panel central.
            // Si tenés un gestor de vistas o un BorderPane estático, lo llamás acá.
            // Ejemplo: MainController.panelCentral.setCenter(root);

        } catch (Exception e) {
            System.err.println("Error al saltar al POS.");
            e.printStackTrace();
        }
    }
}