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
    private final Gson gson = new Gson();

    // =============== 1. TRAER TODOS LOS CLIENTES ===============
    public String obtenerClientes() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }
        } catch (Exception e) {
            System.out.println("Error al traer los clientes: " + e.getMessage());
        }
        return "[]";
    }

    // =============== 2. BUSCADOR PREDICTIVO ===============
    public String buscarClientesPorNombre(String nombre) {
        try {
            String parametro = nombre.replace(" ", "%20");
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/buscar?nombre=" + parametro))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }
        } catch (Exception e) {
            System.out.println("Error en la búsqueda predictiva: " + e.getMessage());
        }
        return "[]";
    }

    // =============== 3. GUARDAR CLIENTE (POST BLINDADO) ===============
    public void guardarClienteEnBaseDeDatos(String nombre, String cuit, String condicionIva, String telefono, String email) throws Exception {

        Map<String, String> datosCliente = new HashMap<>();
        datosCliente.put("nombre", nombre);
        datosCliente.put("cuit", cuit);
        datosCliente.put("condicionIva", condicionIva);
        datosCliente.put("telefono", telefono);
        datosCliente.put("email", email);

        String jsonMandar = gson.toJson(datosCliente);

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMandar))
                .build();

        HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() == 400) {
            throw new RuntimeException(respuesta.body());
        } else if (respuesta.statusCode() != 200 && respuesta.statusCode() != 201) {
            throw new RuntimeException("Error al comunicarse con el servidor (Código: " + respuesta.statusCode() + ")");
        }
    }

    // =============== 3.5. ACTUALIZAR CLIENTE (PUT) ===============
    public void actualizarClienteEnBaseDeDatos(Long id, String nombre, String cuit, String condicionIva, String telefono, String email) throws Exception {

        Map<String, Object> datosCliente = new HashMap<>(); // Usamos Object por si el ID es numérico
        datosCliente.put("id", id);
        datosCliente.put("nombre", nombre);
        datosCliente.put("cuit", cuit);
        datosCliente.put("condicionIva", condicionIva);
        datosCliente.put("telefono", telefono);
        datosCliente.put("email", email);

        String jsonMandar = gson.toJson(datosCliente);

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/" + id)) // ¡Ojo a la URL! Le pasamos el ID
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonMandar)) // Usamos PUT para modificar
                .build();

        HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() == 400 || respuesta.statusCode() == 500) {
            throw new RuntimeException("Error del servidor: " + respuesta.body());
        } else if (respuesta.statusCode() != 200) {
            throw new RuntimeException("Error al comunicarse con el servidor (Código: " + respuesta.statusCode() + ")");
        }
    }

    // =============== 4. VERIFICAR SI EXISTE POR DNI ===============
    public String buscarClientePorCuit(String cuit) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/cuit/" + cuit))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }
        } catch (Exception e) {
            System.out.println("Error al verificar DNI: " + e.getMessage());
        }
        return null;
    }

    // =============== 5. ¡NUEVO! ELIMINAR CLIENTE DE LA BASE DE DATOS ===============
    public void eliminarClienteDeBaseDeDatos(Long id) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/" + id))
                .DELETE()
                .build();

        HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

        // Si falló (código 400 o 500) leemos el mensaje exacto que manda el backend
        if (respuesta.statusCode() != 200 && respuesta.statusCode() != 204) {
            throw new Exception("Error " + respuesta.statusCode() + ": " + respuesta.body());
        }
    }
}