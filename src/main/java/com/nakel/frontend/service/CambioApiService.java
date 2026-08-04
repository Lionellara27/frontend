package com.nakel.frontend.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.Cambio;
import com.nakel.frontend.model.ArticuloInfoDTO; // 🔥 Asegurate de tener este DTO creado en el front

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class CambioApiService {

    private final String API_URL = "http://localhost:8080/api/cambios";
    private final Gson gson = new Gson();
    private final HttpClient http = HttpClient.newHttpClient();

    // 🔥 1. Guardar un nuevo historial de cambio
    public Cambio registrarCambio(Long idVentaOriginal, Cambio nuevoCambio) {
        try {
            String jsonBody = gson.toJson(nuevoCambio);

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/venta/" + idVentaOriginal))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return gson.fromJson(respuesta.body(), Cambio.class);
            } else {
                System.out.println("❌ Error del Backend: " + respuesta.body());
            }
        } catch (Exception e) {
            System.out.println("❌ Error de red al registrar cambio: " + e.getMessage());
        }
        return null;
    }

    // 🔥 2. Obtener el historial completo para mostrarlo en el detalle de la venta
    public List<Cambio> obtenerHistorialPorVenta(Long idVentaOriginal) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/venta/" + idVentaOriginal))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                Type listType = new TypeToken<List<Cambio>>(){}.getType();
                return gson.fromJson(respuesta.body(), listType);
            }
        } catch (Exception e) {
            System.out.println("❌ Error al obtener historial de cambios: " + e.getMessage());
        }
        return null;
    }

    // 🔥 3. NUEVO: Obtener el "Ticket Vivo" (artículos que el cliente tiene actualmente tras los cambios)
    public List<ArticuloInfoDTO> obtenerArticulosActuales(Long idVentaOriginal) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/disponibles-actuales/" + idVentaOriginal))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                Type listType = new TypeToken<List<ArticuloInfoDTO>>(){}.getType();
                return gson.fromJson(respuesta.body(), listType);
            } else {
                System.out.println("❌ Error del Backend al traer ticket vivo: " + respuesta.body());
            }
        } catch (Exception e) {
            System.out.println("❌ Error de red al obtener artículos actuales: " + e.getMessage());
        }
        return new ArrayList<>(); // Retornamos lista vacía por seguridad para la UI
    }
}