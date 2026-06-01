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
        try {
            // 1. Cargamos el diseño del Modal
            // OJO: Ajustá la ruta a donde tengas el archivo nuevo-insumo-modal.fxml
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/nuevo-insumo-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            // 2. Creamos una nueva ventana (Stage)
            javafx.stage.Stage modalStage = new javafx.stage.Stage();
            modalStage.setTitle("Cargar Insumo");

            // 3. Bloqueamos la ventana de atrás (APPLICATION_MODAL) para que no puedan tocar el sistema hasta cerrar esto
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            // 4. Le pasamos el diseño a la ventana y la mostramos
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.setResizable(false); // Para que no la puedan achicar/agrandar y rompan el diseño
            modalStage.showAndWait(); // showAndWait hace que espere a que se cierre para continuar

        } catch (Exception e) {
            System.err.println("Error al abrir el Pop-up de Insumos.");
            e.printStackTrace();
        }
    }
}