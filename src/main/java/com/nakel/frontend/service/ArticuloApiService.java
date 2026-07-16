package com.nakel.frontend.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken; // Importante para las listas
import com.nakel.frontend.model.Articulo;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class ArticuloApiService {

    private static final String API_URL = "http://localhost:8080/api/articulos";
    private final HttpClient http = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    // 🔫 El método que usa la Pistola Láser (mostrador)
    public String buscarArticuloPorCodigo(String codigoBarras) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/codigo/" + codigoBarras))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }
        } catch (Exception e) {
            System.out.println("Error al buscar artículo: " + e.getMessage());
        }
        return null;
    }

    // 📋 NUEVO: Para llenar la tabla del catálogo
    public List<Articulo> obtenerTodos() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                // Truco de GSON para leer una lista completa (Array JSON a List Java)
                Type listType = new TypeToken<ArrayList<Articulo>>(){}.getType();
                return gson.fromJson(respuesta.body(), listType);
            }
        } catch (Exception e) {
            System.out.println("Error al obtener el catálogo: " + e.getMessage());
        }
        return new ArrayList<>(); // Devuelve lista vacía si hay error para que no explote la tabla
    }

    // 💾 NUEVO: Para el botón "➕ Nuevo Artículo"
    public boolean guardarArticulo(Articulo articulo) {
        try {
            String jsonArticulo = gson.toJson(articulo);

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json") // Fundamental
                    .POST(HttpRequest.BodyPublishers.ofString(jsonArticulo))
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            // Aceptamos 200 OK o 201 Created como éxito
            return respuesta.statusCode() == 200 || respuesta.statusCode() == 201;

        } catch (Exception e) {
            System.out.println("Error al guardar el artículo: " + e.getMessage());
            return false;
        }
    }
    // 🗑️ NUEVO: Eliminar artículo de la base de datos
    public void eliminarArticuloDeBaseDeDatos(Long id) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/" + id))
                .DELETE()
                .build();

        HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() != 200 && respuesta.statusCode() != 204) {
            throw new Exception("Error " + respuesta.statusCode() + " del servidor al eliminar el artículo.");
        }
    }

    // 🔍 NUEVO: Buscar producto para la pantalla de Cambios (por código)
    public String buscarProducto(String busqueda) throws Exception {
        // Nota: Si en tu backend (Spring Boot) tenés una ruta específica para buscar
        // por nombre, podés cambiar esta URL. Por ahora, usamos la de código que ya tenés:
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/codigo/" + busqueda))
                .GET()
                .build();

        HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() == 200) {
            return respuesta.body(); // Devuelve el JSON exitosamente
        } else if (respuesta.statusCode() == 404) {
            return null; // No lo encontró, le avisamos al controlador para que tire cartel rojo
        } else {
            throw new Exception("Error del servidor: " + respuesta.statusCode());
        }
    }
}