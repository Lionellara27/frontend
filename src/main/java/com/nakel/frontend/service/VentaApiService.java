package com.nakel.frontend.service;

import com.google.gson.Gson;
import com.nakel.frontend.model.Venta;
import com.nakel.frontend.util.SesionActual; // 🔥 IMPORTANTE: Traemos la sesión

import java.net.URI;
import java.net.http.*;

public class VentaApiService {
    private final String API_URL = "http://localhost:8080/api/ventas";
    private final Gson gson = new Gson();
    private final HttpClient http = HttpClient.newHttpClient();

    public boolean registrarVenta(Venta venta) {
        try {
            String jsonVenta = gson.toJson(venta);

            // 🔥 1. Agarramos al cajero que está logueado en este momento
            String username = SesionActual.getUsuarioLogueado();

            // 🔥 2. Se lo pegamos al final de la URL para que el Backend sepa de quién es la caja
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "?username=" + username))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonVenta))
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            System.out.println("CÓDIGO DE RESPUESTA: " + respuesta.statusCode());
            System.out.println("MENSAJE DEL BACKEND: " + respuesta.body());

            // 🔥 3. Aceptamos 200 (OK) o 201 (Created) para que no tire error falso
            return respuesta.statusCode() == 200 || respuesta.statusCode() == 201;

        } catch (Exception e) {
            System.out.println("❌ Error de red al registrar la venta: " + e.getMessage());
            return false;
        }
    }

    public String obtenerHistorialVentas() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return respuesta.body(); // Devuelve el JSON con el "content"
            }
        } catch (Exception e) {
            System.out.println("Error al traer historial: " + e.getMessage());
        }
        return null;
    }

    //nuevo
    // 📋 NUEVO: Para el paginador del Historial de Ventas
    public String obtenerHistorialVentasPaginado(int pagina, int cantidadPorPagina) {
        try {
            String url = API_URL + "/historial?page=" + pagina + "&size=" + cantidadPorPagina;
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }
        } catch (Exception e) {
            System.out.println("Error al traer historial paginado: " + e.getMessage());
        }
        return "[]";
    }
}