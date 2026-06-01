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
        System.out.println("Preparando Pop-up de nuevo cliente...");

        // Una alerta temporal para comprobar que el botón funciona
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Nuevo Cliente");
        alerta.setHeaderText(null);
        alerta.setContentText("¡Próximamente! Acá se va a abrir la ventanita para cargar Nombre, DNI y Teléfono.");
        alerta.showAndWait();
    }
}