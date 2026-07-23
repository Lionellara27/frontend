package com.nakel.frontend.service;

import com.google.gson.Gson;
import com.nakel.frontend.model.Receta;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ProduccionApiService {

    private static final String API_URL = "http://localhost:8080/api/produccion/fabricar";
    private final HttpClient clienteHttp = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    // =============== PROCESAR FABRICACIÓN Y ALTA DE PRODUCTO ===============
    public void registrarFabricacion(Receta receta) throws Exception {

        // Convertimos el objeto Receta completo a un String JSON
        String jsonMandar = gson.toJson(receta);

        System.out.println("📦 Enviando paquete de producción al Backend: " + jsonMandar);

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMandar))
                .build();

        HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

        // Si el backend frena algo (ej: stock insuficiente), devuelve 400 y tiramos el mensaje de error
        if (respuesta.statusCode() == 400 || respuesta.statusCode() == 500) {
            throw new RuntimeException(respuesta.body());
        } else if (respuesta.statusCode() != 200 && respuesta.statusCode() != 201) {
            throw new RuntimeException("Error al comunicarse con el servidor (Código: " + respuesta.statusCode() + ")");
        }
    }
}