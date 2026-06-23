package com.nakel.frontend.service;

import com.google.gson.Gson;
import com.nakel.frontend.model.AjustesDivisa;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ConfiguracionApiService {

    private static final String API_URL = "http://localhost:8080/api/configuracion/divisa";
    private final HttpClient http = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    // 📥 TRAER CONFIGURACIÓN ACTUAL
    public AjustesDivisa obtenerAjustesDivisa() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return gson.fromJson(respuesta.body(), AjustesDivisa.class);
            }
        } catch (Exception e) {
            System.out.println("Error al obtener config: " + e.getMessage());
        }
        // Si el backend no responde, devolvemos un objeto por defecto para que no explote
        return new AjustesDivisa(1000.0, false);
    }

    // 📤 GUARDAR NUEVA CONFIGURACIÓN
    public boolean guardarAjustesDivisa(AjustesDivisa ajustes) {
        try {
            String json = gson.toJson(ajustes);

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json)) // Usamos PUT porque actualiza
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            return respuesta.statusCode() == 200;

        } catch (Exception e) {
            System.out.println("Error al guardar config: " + e.getMessage());
            return false;
        }
    }
}