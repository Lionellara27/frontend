package com.nakel.frontend.service;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MovimientoCajaApiService {

    private static final String API_URL = "http://localhost:8080/api/movimientos-caja";
    private final HttpClient clienteHttp = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    // 🟢 Busca todos los movimientos de una caja para mostrar en el Modal
    public String obtenerMovimientosPorCaja(Long cajaId) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/caja/" + cajaId))
                    .GET()
                    .build();
            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() == 200) return respuesta.body();
        } catch (Exception e) {
            System.out.println("❌ Error al obtener movimientos: " + e.getMessage());
        }
        return "[]";
    }
}