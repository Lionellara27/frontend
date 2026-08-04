package com.nakel.frontend.service;

import com.google.gson.Gson;
import com.nakel.frontend.model.Vale;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ValeApiService {

    private final String API_URL = "http://localhost:8080/api/vales";
    private final Gson gson = new Gson();
    private final HttpClient http = HttpClient.newHttpClient();

    // 🔥 1. Ahora recibe el monto y el idCliente (puede ser null)
    public Vale generarVale(Double monto, Long idCliente) {
        try {
            // Armamos la URL base con el monto
            String url = API_URL + "/generar?monto=" + monto;

            // Si hay un cliente registrado, lo agregamos a la URL
            if (idCliente != null) {
                url += "&idCliente=" + idCliente;
            }

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody()) // No mandamos body, va por parámetro
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return gson.fromJson(respuesta.body(), Vale.class);
            }
        } catch (Exception e) {
            System.out.println("❌ Error al generar vale: " + e.getMessage());
        }
        return null;
    }

    // 2. Valida si el vale existe, no está vencido ni usado
    public Vale validarVale(String codigo) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/validar/" + codigo))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return gson.fromJson(respuesta.body(), Vale.class);
            } else {
                // 🔥 ARREGLADO: En vez de explotar, avisamos por consola y devolvemos null
                System.out.println("⚠️ Vale rechazado por el backend: " + respuesta.body());
                return null;
            }
        } catch (Exception e) {
            System.out.println("❌ Error de red al validar vale: " + e.getMessage());
            // 🔥 ARREGLADO: En vez de lanzar error, devolvemos null para que el frontend muestre la alerta
            return null;
        }
    }

    // 3. Quema el vale cuando la venta se confirma
    public boolean consumirVale(String codigo) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/consumir/" + codigo))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            return respuesta.statusCode() == 200;

        } catch (Exception e) {
            System.out.println("❌ Error al consumir vale: " + e.getMessage());
            return false;
        }
    }
}