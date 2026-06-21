package com.nakel.frontend.service;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class ClienteApiService {

    private static final String API_URL = "http://localhost:8080/api/clientes";
    private final HttpClient clienteHttp = HttpClient.newHttpClient();
    private final Gson gson = new Gson(); // Instanciamos Gson una sola vez para toda la clase

    // =============== 1. TRAER CLIENTES (GET) ===============
    // Este método lo usás al abrir la ventana para cargar el ComboBox
    public String obtenerClientes() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return respuesta.body(); // Devuelve el JSON con todos los clientes
            }
        } catch (Exception e) {
            System.out.println("Error al traer los clientes: " + e.getMessage());
        }
        return "[]"; // Si falla, devuelve una lista vacía para que no explote nada
    }

    // =============== 2. GUARDAR CLIENTE (POST BLINDADO) ===============
    public boolean guardarClienteEnBaseDeDatos(String nombre, String documento, String condicionIva, String telefono) {
        try {
            // 1. Armamos el "diccionario" de datos (Las claves deben coincidir con tu Backend)
            Map<String, String> datosCliente = new HashMap<>();
            datosCliente.put("nombre", nombre);
            datosCliente.put("documento", documento);
            datosCliente.put("condicionIva", condicionIva);
            datosCliente.put("telefono", telefono);

            // 2. Gson hace la magia: convierte el Mapa en un JSON inquebrantable
            String jsonMandar = gson.toJson(datosCliente);

            // 3. Preparamos el envío
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonMandar))
                    .build();

            // 4. Mandamos el paquete
            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            // Si respondió 200 (OK) o 201 (Creado), es un éxito
            return respuesta.statusCode() == 200 || respuesta.statusCode() == 201;

        } catch (Exception e) {
            System.out.println("Error al mandar el cliente: " + e.getMessage());
            return false;
        }
    }
}