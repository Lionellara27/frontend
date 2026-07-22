package com.nakel.frontend.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.Categoria;
import com.nakel.frontend.model.Material;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class ParametrosApiService {

    private static final String API_URL_CAT = "http://localhost:8080/api/categorias";
    private static final String API_URL_MAT = "http://localhost:8080/api/materiales";

    private final HttpClient http = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    // ==========================================
    // CATEGORÍAS
    // ==========================================

    public List<Categoria> obtenerCategorias() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL_CAT))
                    .GET()
                    .build();

            HttpResponse<String> respuesta =
                    http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                Type listType = new TypeToken<ArrayList<Categoria>>(){}.getType();
                return gson.fromJson(respuesta.body(), listType);
            }

            System.out.println("Error al obtener categorías. Código: " + respuesta.statusCode());
            System.out.println(respuesta.body());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    public Categoria guardarCategoria(Categoria cat) {

        try {

            String json = gson.toJson(cat);

            System.out.println("========== GUARDAR CATEGORÍA ==========");
            System.out.println("POST -> " + API_URL_CAT);
            System.out.println("JSON ENVIADO:");
            System.out.println(json);

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL_CAT))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> respuesta =
                    http.send(peticion, HttpResponse.BodyHandlers.ofString());

            System.out.println("STATUS: " + respuesta.statusCode());
            System.out.println("BODY:");
            System.out.println(respuesta.body());

            if (respuesta.statusCode() == 200 || respuesta.statusCode() == 201) {
                return gson.fromJson(respuesta.body(), Categoria.class);
            }

            throw new RuntimeException(
                    "Error HTTP " + respuesta.statusCode() + " -> " + respuesta.body());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void eliminarCategoria(Long id) throws Exception {

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL_CAT + "/" + id))
                .DELETE()
                .build();

        HttpResponse<String> respuesta =
                http.send(peticion, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() != 200 && respuesta.statusCode() != 204) {
            throw new Exception("Error " + respuesta.statusCode() + ": " + respuesta.body());
        }
    }

    // ==========================================
    // MATERIALES
    // ==========================================

    public List<Material> obtenerMateriales() {

        try {

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL_MAT))
                    .GET()
                    .build();

            HttpResponse<String> respuesta =
                    http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                Type listType = new TypeToken<ArrayList<Material>>(){}.getType();
                return gson.fromJson(respuesta.body(), listType);
            }

            System.out.println("Error al obtener materiales. Código: " + respuesta.statusCode());
            System.out.println(respuesta.body());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    public Material guardarMaterial(Material mat) {

        try {

            String json = gson.toJson(mat);

            System.out.println("========== GUARDAR MATERIAL ==========");
            System.out.println("POST -> " + API_URL_MAT);
            System.out.println("JSON ENVIADO:");
            System.out.println(json);

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL_MAT))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> respuesta =
                    http.send(peticion, HttpResponse.BodyHandlers.ofString());

            System.out.println("STATUS: " + respuesta.statusCode());
            System.out.println("BODY:");
            System.out.println(respuesta.body());

            if (respuesta.statusCode() == 200 || respuesta.statusCode() == 201) {
                return gson.fromJson(respuesta.body(), Material.class);
            }

            throw new RuntimeException(
                    "Error HTTP " + respuesta.statusCode() + " -> " + respuesta.body());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void eliminarMaterial(Long id) throws Exception {

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL_MAT + "/" + id))
                .DELETE()
                .build();

        HttpResponse<String> respuesta =
                http.send(peticion, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() != 200 && respuesta.statusCode() != 204) {
            throw new Exception("Error " + respuesta.statusCode() + ": " + respuesta.body());
        }
    }
}