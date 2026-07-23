package com.nakel.frontend.service;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CajaApiService {

    private static final String API_URL = "http://localhost:8080/api/caja";
    private final HttpClient clienteHttp = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public String obtenerCajaActual() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/actual"))
                    .GET()
                    .build();
            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() == 200) return respuesta.body();
        } catch (Exception e) {
            System.out.println("❌ Error al obtener caja: " + e.getMessage());
        }
        return null;
    }

    public boolean cerrarCaja() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/cerrar"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());
            return respuesta.statusCode() == 200;
        } catch (Exception e) {
            System.out.println("❌ Error al cerrar caja: " + e.getMessage());
            return false;
        }
    }

    public String obtenerHistorial() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/historial"))
                    .GET()
                    .build();
            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() == 200) return respuesta.body();
        } catch (Exception e) {
            System.out.println("❌ Error al obtener historial: " + e.getMessage());
        }
        return "[]";
    }
}