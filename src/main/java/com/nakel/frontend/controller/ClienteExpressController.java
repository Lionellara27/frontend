package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.nakel.frontend.model.Cliente;
import com.nakel.frontend.service.ClienteApiService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

public class ClienteExpressController {

    @FXML private TextField txtDni;
    @FXML private TextField txtNombre;

    private final ClienteApiService api = new ClienteApiService();
    private final Gson gson = new Gson();

    // 🔥 Este método hace la magia y devuelve el texto para el ComboBox
    public String procesarGuardado() {
        String dni = txtDni.getText();
        String nombreIngresado = txtNombre.getText();

        if (dni.isBlank() || nombreIngresado.isBlank()) {
            mostrarError("Datos incompletos", "El DNI y el Nombre son obligatorios.");
            return null;
        }

        try {
            String clienteJson = api.buscarClientePorCuit(dni);

            if (clienteJson != null && !clienteJson.isBlank()) {
                // 💥 ¡PEPE YA EXISTE!
                Cliente pepeHistorico = gson.fromJson(clienteJson, Cliente.class);

                Alert alerta = new Alert(Alert.AlertType.INFORMATION);
                alerta.setTitle("Cliente Existente");
                alerta.setHeaderText("¡" + pepeHistorico.getNombre() + " ya estaba registrado!");
                alerta.setContentText("Se cargará automáticamente en el mostrador.");
                alerta.showAndWait();

                return pepeHistorico.getNombre() + " - " + pepeHistorico.getCuit();
            } else {
                // 🆕 NO EXISTE: LO CREAMOS NUEVO
                api.guardarClienteEnBaseDeDatos(nombreIngresado, dni, "CONSUMIDOR_FINAL", "", "");
                return nombreIngresado + " - " + dni;
            }
        } catch (Exception e) {
            mostrarError("Error al conectar", "No se pudo crear el cliente: " + e.getMessage());
            return null;
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setTitle(titulo);
        error.setHeaderText(null);
        error.setContentText(mensaje);
        error.showAndWait();
    }
}