package com.nakel.frontend.service;

import com.google.gson.Gson;
import com.nakel.frontend.model.Categoria; // Asegurate de que importe tu clase Categoria del front

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CategoriaApiService {

    // 🔥 Le pega directo al controlador de categorías que hicimos en el backend
    private static final String API_URL = "http://localhost:8080/api/categorias-insumo";
    private final HttpClient httpClient;
    private final Gson gson;

    public CategoriaApiService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    // =============== 1. OBTENER TODAS (Para el ComboBox) ===============
    public String obtenerCategorias() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = httpClient.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }
        } catch (Exception e) {
            System.out.println("Error de conexión al obtener categorías: " + e.getMessage());
        }
        return "[]";
    }

    // =============== 2. GUARDAR NUEVA (Para el modal de alta rápida) ===============
    public void guardarCategoriaEnBaseDeDatos(Categoria categoria) throws Exception {
        String jsonMandar = gson.toJson(categoria);

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMandar))
                .build();

        HttpResponse<String> respuesta = httpClient.send(peticion, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() == 400) {
            throw new RuntimeException(respuesta.body());
        } else if (respuesta.statusCode() != 200 && respuesta.statusCode() != 201) {
            throw new RuntimeException("Error al comunicarse con el servidor (Código: " + respuesta.statusCode() + ")");
        }
    }
}