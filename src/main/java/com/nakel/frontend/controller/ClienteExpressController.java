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

    // 🔥 CAMBIO CLAVE: Ahora devuelve un objeto Cliente real, no un String
    public Cliente procesarGuardado() {
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

                // 🔥 Devolvemos el objeto real con su ID intacto
                return pepeHistorico;
            } else {
                // 🆕 NO EXISTE: LO CREAMOS NUEVO
                boolean guardadoExitoso = api.guardarClienteEnBaseDeDatos(null, nombreIngresado, dni, "CONSUMIDOR_FINAL", "", "");

                if (guardadoExitoso) {
                    // 🔥 MAGIA: Lo volvemos a buscar rapidísimo para atrapar el ID que le acaba de dar la base de datos
                    String clienteNuevoJson = api.buscarClientePorCuit(dni);
                    if (clienteNuevoJson != null && !clienteNuevoJson.isBlank()) {
                        return gson.fromJson(clienteNuevoJson, Cliente.class); // ¡Devuelve el cliente con su ID nuevito!
                    }
                } else {
                    mostrarError("Error", "No se pudo guardar el cliente en el servidor.");
                }
                return null;
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