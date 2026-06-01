package com.nakel.frontend.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class ClienteController {

    // 1. Enlazamos el campo de búsqueda
    @FXML
    private TextField txtBuscarCliente;

    // 2. Enlazamos la tabla (Por ahora le pongo <?> hasta que traigamos tu clase/entidad Cliente)
    @FXML
    private TableView<?> tablaClientes;

    // 3. Este método se ejecuta automáticamente al abrir la pantalla
    @FXML
    public void initialize() {
        System.out.println("¡Módulo de Clientes cargado con éxito!");
        // A futuro: Acá vamos a llamar a la base de datos para cargar la tabla
        // y asegurarnos de que el "Consumidor Final" esté ahí.
    }

    // 4. Acción para el botón "Buscar"
    @FXML
    public void buscarCliente(ActionEvent event) {
        String textoBusqueda = txtBuscarCliente.getText();
        System.out.println("Buscando en la base de datos: " + textoBusqueda);

        // A futuro: Lógica para filtrar la tabla usando SQLite
    }

    // 5. Acción para el botón "➕ Nuevo Cliente"
    @FXML
    public void abrirModalNuevoCliente(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/nuevo-cliente-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage modalStage = new javafx.stage.Stage();
            modalStage.setTitle("Alta de Cliente");
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.setResizable(false);
            modalStage.showAndWait();

        } catch (Exception e) {
            System.err.println("Error al abrir el Pop-up de Clientes.");
            e.printStackTrace();
        }
    }
}