package com.nakel.frontend.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
                Type listType = new TypeToken<ArrayList<Articulo>>(){}.getType();
                return gson.fromJson(respuesta.body(), listType);
            }
        } catch (Exception e) {
            System.out.println("Error al obtener el catálogo: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    // 💾 NUEVO: Para el botón "➕ Nuevo Artículo"
    public boolean guardarArticulo(Articulo articulo) {
        try {
            String jsonArticulo = gson.toJson(articulo);

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonArticulo))
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            return respuesta.statusCode() == 200 || respuesta.statusCode() == 201;

        } catch (Exception e) {
            System.out.println("Error al guardar el artículo: " + e.getMessage());
            return false;
        }
    }

    // 🔄 NUEVO: Actualizar artículo (Para el botón Editar ✏️)
    public boolean actualizarArticulo(Articulo articulo) {
        try {
            String jsonArticulo = gson.toJson(articulo);
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/" + articulo.getId())) // Usa el ID para actualizar
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonArticulo)) // Usamos PUT para modificar
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            return respuesta.statusCode() == 200 || respuesta.statusCode() == 201;
        } catch (Exception e) {
            System.out.println("Error al actualizar el artículo: " + e.getMessage());
            return false;
        }
    }

    // 🔥 ELIMINAR TOTALMENTE BLINDADO (Sin throws Exception, atrapa los errores)
    public boolean eliminarArticuloDeBaseDeDatos(Long id) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/" + id))
                    .DELETE()
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            // 200 OK o 204 No Content significan que lo borró con éxito
            if (respuesta.statusCode() == 200 || respuesta.statusCode() == 204) {
                return true;
            } else {
                // Si devuelve cualquier otro código (ej: 500 por clave foránea, o 404 si no existe)
                System.out.println("⚠️ El servidor rechazó el borrado. Código: " + respuesta.statusCode());
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error interno al intentar eliminar: " + e.getMessage());
            return false;
        }
    }

    // 🔍 NUEVO: Buscar producto para la pantalla de Cambios (por código)
    public String buscarProducto(String busqueda) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/codigo/" + busqueda))
                .GET()
                .build();

        HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() == 200) {
            return respuesta.body();
        } else if (respuesta.statusCode() == 404) {
            return null;
        } else {
            throw new Exception("Error del servidor: " + respuesta.statusCode());
        }
    }

    // 🔥 Suma stock (cuando el cliente devuelve algo)
    public boolean restaurarStock(Long idArticulo, int cantidad) {
        try {
            String url = API_URL + "/" + idArticulo + "/restaurar-stock?cantidad=" + cantidad;
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            return respuesta.statusCode() == 200;
        } catch (Exception e) {
            System.out.println("❌ Error al restaurar stock: " + e.getMessage());
            return false;
        }
    }

    // 🔥 Resta stock (cuando el cliente se lleva algo nuevo por el cambio)
    public boolean descontarStock(Long idArticulo, int cantidad) {
        try {
            String url = API_URL + "/" + idArticulo + "/descontar-stock?cantidad=" + cantidad;
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            return respuesta.statusCode() == 200;
        } catch (Exception e) {
            System.out.println("❌ Error al descontar stock: " + e.getMessage());
            return false;
        }
    }
}