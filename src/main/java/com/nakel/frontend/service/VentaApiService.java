package com.nakel.frontend.service;

import com.google.gson.Gson;
import com.nakel.frontend.model.Venta;

import java.net.URI;
import java.net.http.*;

public class VentaApiService {
    private final String API_URL = "http://localhost:8080/api/ventas";
    private final Gson gson = new Gson();
    private final HttpClient http = HttpClient.newHttpClient();

    public boolean registrarVenta(Venta venta) {
        try {
            String jsonVenta = gson.toJson(venta);
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonVenta))
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            return respuesta.statusCode() == 201; // 201 Created
        } catch (Exception e) {
            return false;
        }
    }
}