package com.nakel.frontend.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ProveedorController {

    @FXML private TextField txtBuscarProveedor;
    @FXML private TableView<?> tablaProveedores;

    @FXML
    public void initialize() {
        System.out.println("¡Módulo de Proveedores cargado!");
    }

    @FXML
    public void buscarProveedor(ActionEvent event) {
        System.out.println("Buscando proveedor: " + txtBuscarProveedor.getText());
        tablaProveedores.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    @FXML
    public void abrirModalNuevoProveedor(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nakel/frontend/view/nuevo-proveedor-modal.fxml"));
            Parent root = loader.load();

            Stage modalStage = new Stage();
            modalStage.setTitle("Cargar Proveedor");
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setScene(new Scene(root));
            modalStage.setResizable(false);
            modalStage.showAndWait();

        } catch (Exception e) {
            System.err.println("Error al abrir el modal de Proveedores.");
            e.printStackTrace();
        }
    }
}