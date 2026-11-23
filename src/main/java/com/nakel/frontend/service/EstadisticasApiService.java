package com.nakel.frontend.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class EstadisticasApiService {

    private static final String API_URL = "http://localhost:8080/api/estadisticas";
    private final HttpClient clienteHttp = HttpClient.newHttpClient();

    // =============== 1. TRAER DATOS DEL DASHBOARD ===============
    public String obtenerDatosDashboard() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/dashboard"))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return respuesta.body(); // Devuelve el JSON con los 6 números
            } else {
                System.out.println("⚠️ Error del servidor. Código: " + respuesta.statusCode());
            }
        } catch (Exception e) {
            System.out.println("❌ Error al traer las estadísticas: " + e.getMessage());
        }
        return null; // Si falla, devolvemos null para que el frontend no explote
    }

    // =============== 2. RESUMEN COMPLETO DE UN PERÍODO ===============
    // periodo: HOY, SEMANA, MES, MES_PASADO o ANIO. Devuelve el JSON, o null si falla.
    // anio: solo para periodo ANIO (puede ser null = año en curso).
    public String obtenerResumen(String periodo, Integer anio) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/resumen?periodo=" + periodo + (anio != null ? "&anio=" + anio : "")))
                    .GET()
                    .build();
            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }
            System.out.println("Error del servidor al pedir estadísticas. Código: " + respuesta.statusCode());
        } catch (Exception e) {
            System.out.println("Error al traer las estadísticas: " + e.getMessage());
        }
        return null;
    }
}