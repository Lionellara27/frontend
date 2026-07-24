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

    // 🔥 Agregamos String username
    public String obtenerCajaActual(String username) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/actual?username=" + username))
                    .GET()
                    .build();
            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() == 200) return respuesta.body();
        } catch (Exception e) {
            System.out.println("❌ Error al obtener caja: " + e.getMessage());
        }
        return null;
    }

    // 🔥 Agregamos String username
    public boolean cerrarCaja(String username) {
        try {
            // 🔥 1. Codificamos el nombre por si tiene espacios (Ej: "Lio " -> "Lio%20")
            String nombreSeguro = java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8);

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/cerrar?username=" + nombreSeguro))
                    .header("Accept", "application/json") // Le decimos al backend que hablemos claro
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            // 🔥 2. IMPRIMIMOS LO QUE SE ESTÁN DICIENDO
            System.out.println("STATUS CIERRE: " + respuesta.statusCode());
            System.out.println("BODY CIERRE: " + respuesta.body());

            // 3. Si el status es 200, fue un éxito rotundo
            return respuesta.statusCode() == 200;

        } catch (Exception e) {
            System.out.println("❌ Error CRÍTICO al cerrar caja: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Este queda igual porque el backend no pide usuario para leer el historial
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