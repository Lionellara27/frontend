package com.nakel.frontend.service;

import com.google.gson.Gson;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class ProveedorApiService {

    private static final String API_URL = "http://localhost:8080/api/proveedores";
    private final HttpClient clienteHttp = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    // =============== 1. TRAER TODOS LOS PROVEEDORES ===============
    public String obtenerProveedores() {
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
            System.out.println("Error al traer los proveedores: " + e.getMessage());
        }
        return "[]";
    }

    // =============== 2. GUARDAR PROVEEDORES (POST BLINDADO) ===============
    public void guardarProveedoresEnBaseDeDatos(String razonSocial, String nombreContacto, String rubro, String cuit, String telefono, String email, BigDecimal saldoPendiente) throws Exception {

        Map<String, Object> datosProveedor = new HashMap<>();
        datosProveedor.put("razonSocial", razonSocial);
        datosProveedor.put("nombreContacto", nombreContacto);
        datosProveedor.put("rubro", rubro);
        datosProveedor.put("cuit", cuit);
        datosProveedor.put("telefono", telefono);
        datosProveedor.put("email", email);
        datosProveedor.put("saldoPendiente", saldoPendiente);

        String jsonMandar = gson.toJson(datosProveedor);

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

    // =============== 3. ACTUALIZAR PROVEEDORES (PUT) ===============
    public void actualizarProveedoresEnBaseDeDatos(Long id, String razonSocial, String nombreContacto, String rubro, String cuit, String telefono, String email, BigDecimal saldoPendiente) throws Exception {

        Map<String, Object> datosProveedor = new HashMap<>();
        datosProveedor.put("id", id);
        datosProveedor.put("razonSocial", razonSocial);
        datosProveedor.put("nombreContacto", nombreContacto);
        datosProveedor.put("rubro", rubro);
        datosProveedor.put("cuit", cuit);
        datosProveedor.put("telefono", telefono);
        datosProveedor.put("email", email);
        datosProveedor.put("saldoPendiente", saldoPendiente);

        String jsonMandar = gson.toJson(datosProveedor);

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/" + id))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonMandar))
                .build();

        HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() == 400 || respuesta.statusCode() == 500) {
            throw new RuntimeException("Error del servidor: " + respuesta.body());
        } else if (respuesta.statusCode() != 200) {
            throw new RuntimeException("Error al comunicarse con el servidor (Código: " + respuesta.statusCode() + ")");
        }
    }

    // =============== 4. ELIMINAR PROVEEDORES DE LA BASE DE DATOS ===============
    public void eliminarProveedorDeBaseDeDatos(Long id) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/" + id))
                .DELETE()
                .build();

        HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() != 200 && respuesta.statusCode() != 204) {
            throw new Exception("Error " + respuesta.statusCode() + ": " + respuesta.body());
        }
    }

    // =============== 5. BUSCADOR PREDICTIVO (POR NOMBRE/RAZÓN SOCIAL) ===============
    public String buscarProveedorPorNombre(String nombre) {
        try {
            // Reemplazamos los espacios por %20 para que la URL no se rompa al viajar por internet
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
            System.out.println("Error en la búsqueda de proveedores: " + e.getMessage());
        }
        return "[]"; // Si falla, devolvemos una lista vacía para que la tabla no explote
    }

    // =============== 6. BUSCAR POR CUIT EXACTO ===============
    public String buscarProveedorPorCuit(String cuit) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/cuit/" + cuit))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return respuesta.body(); // Devuelve el JSON del proveedor encontrado
            }
        } catch (Exception e) {
            System.out.println("Error al verificar CUIT: " + e.getMessage());
        }
        return null; // Si no lo encuentra o tira error (Ej: 404 Not Found), devuelve null
    }
}