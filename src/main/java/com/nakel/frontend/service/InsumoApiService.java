package com.nakel.frontend.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.Insumo;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;

public class InsumoApiService {

    private static final String API_URL = "http://localhost:8080/api/insumos";
    private final HttpClient insumoHttp;
    private final Gson gson;

    public InsumoApiService() {
        this.insumoHttp = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    // =============== 1. OBTENER TODOS LOS INSUMOS ===============
    public String obtenerInsumos() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = insumoHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }
        } catch (Exception e) {
            System.out.println("Error de conexión al obtener insumos: " + e.getMessage());
        }
        return "[]";
    }

    // =============== 2. BUSCADOR PREDICTIVO ===============
    public String buscarInsumosPorNombre(String nombre) {
        try {
            String parametro = nombre.replace(" ", "%20");
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/buscar?nombre=" + parametro))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = insumoHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }
        } catch (Exception e) {
            System.out.println("Error en la búsqueda predictiva de insumos: " + e.getMessage());
        }
        return "[]";
    }

    // =============== 3. GUARDAR INSUMO (POST BLINDADO) ===============
    public void guardarInsumoEnBaseDeDatos(Insumo insumo) throws Exception {

        // Gson convierte automáticamente el objeto Insumo a JSON
        String jsonMandar = gson.toJson(insumo);

        // 🔥 MICRÓFONO ACTIVADO: Vemos qué sale del Frontend
        System.out.println("========== GUARDAR INSUMO (FRONTEND) ==========");
        System.out.println("JSON ENVIADO: " + jsonMandar);

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMandar))
                .build();

        HttpResponse<String> respuesta = insumoHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

        // 🔥 MICRÓFONO ACTIVADO: Vemos qué devuelve el Backend
        System.out.println("STATUS: " + respuesta.statusCode());
        System.out.println("BODY DEVUELTO: " + respuesta.body());
        System.out.println("===============================================");

        if (respuesta.statusCode() == 400) {
            throw new RuntimeException(respuesta.body());
        } else if (respuesta.statusCode() != 200 && respuesta.statusCode() != 201) {
            throw new RuntimeException("Error al comunicarse con el servidor (Código: " + respuesta.statusCode() + ")");
        }
    }

    // =============== 3.5. ACTUALIZAR INSUMO (PUT) ===============
    public void actualizarInsumoEnBaseDeDatos(Long id, Insumo insumo) throws Exception {

        String jsonMandar = gson.toJson(insumo);

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/" + id)) // ¡Ojo a la URL! Le pasamos el ID
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonMandar))
                .build();

        HttpResponse<String> respuesta = insumoHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() == 400 || respuesta.statusCode() == 500) {
            throw new RuntimeException("Error del servidor: " + respuesta.body());
        } else if (respuesta.statusCode() != 200) {
            throw new RuntimeException("Error al comunicarse con el servidor (Código: " + respuesta.statusCode() + ")");
        }
    }

    // =============== 4. ELIMINAR INSUMO ===============
    public void eliminarInsumoDeBaseDeDatos(Long id) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/" + id))
                .DELETE()
                .build();

        HttpResponse<String> respuesta = insumoHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

        // Si falló (código 400 o 500) leemos el mensaje exacto que manda el backend
        if (respuesta.statusCode() != 200 && respuesta.statusCode() != 204) {
            throw new Exception("Error " + respuesta.statusCode() + ": " + respuesta.body());
        }
    }
    //---------------------
    public List<Insumo> obtenerListaInsumos() {

        try {

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/lista"))
                    .GET()
                    .build();

            HttpResponse<String> respuesta =
                    insumoHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {

                Type tipo = new TypeToken<List<Insumo>>() {}.getType();

                return gson.fromJson(respuesta.body(), tipo);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }
}