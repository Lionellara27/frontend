package com.nakel.frontend.service;

import com.google.gson.Gson;
import com.nakel.frontend.model.Venta;
import com.nakel.frontend.util.SesionActual;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class VentaApiService {

    private static final String API_URL = "http://localhost:8080/api/ventas";
    private final Gson gson = new Gson();
    private final HttpClient http = HttpClient.newHttpClient();

    public boolean registrarVenta(Venta venta) {
        try {
            String jsonVenta = gson.toJson(venta);
            String username = SesionActual.getUsuarioLogueado();

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "?username=" + username))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonVenta))
                    .build();

            HttpResponse<String> respuesta = http.send(
                    peticion,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("CÓDIGO DE RESPUESTA: " + respuesta.statusCode());
            System.out.println("MENSAJE DEL BACKEND: " + respuesta.body());

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

            HttpResponse<String> respuesta = http.send(
                    peticion,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }

            System.out.println("⚠️ Error al obtener historial. Código: " + respuesta.statusCode());

        } catch (Exception e) {
            System.out.println("❌ Error al traer historial: " + e.getMessage());
        }

        return null;
    }

    // 🔥 ACTUALIZADO: Ahora recibe la búsqueda y el mes
    public String obtenerHistorialVentasPaginado(int pagina, int cantidadPorPagina, String busqueda, String criterio, int mes) {
        try {
            String textoBusqueda = (busqueda != null) ? busqueda.trim() : "";
            // 🔥 Agregamos el criterio con un valor por defecto por las dudas
            String textoCriterio = (criterio != null) ? criterio : "Nro. Comprobante";

            // 🔥 Sumamos el criterio a la URL codificándolo igual que la búsqueda
            String url = API_URL
                    + "/historial?page=" + pagina
                    + "&size=" + cantidadPorPagina
                    + "&buscar=" + java.net.URLEncoder.encode(textoBusqueda, java.nio.charset.StandardCharsets.UTF_8)
                    + "&criterio=" + java.net.URLEncoder.encode(textoCriterio, java.nio.charset.StandardCharsets.UTF_8)
                    + "&mes=" + mes;

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = http.send(
                    peticion,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }

            System.out.println("⚠️ Error al obtener historial paginado. Código: " + respuesta.statusCode());

        } catch (Exception e) {
            System.out.println("❌ Error al traer historial paginado: " + e.getMessage());
        }

        return "[]";
    }

    // 🔥 NUEVO: Le pide a la base de datos la suma global de plata
    public double obtenerTotalGlobal(String busqueda, String criterio, int mes) {
        try {
            String textoBusqueda = (busqueda != null) ? busqueda.trim() : "";
            // 🔥 Agregamos el criterio con un valor por defecto
            String textoCriterio = (criterio != null) ? criterio : "Nro. Comprobante";

            // 🔥 Sumamos el criterio a la URL codificándolo igual que la búsqueda
            String url = API_URL
                    + "/historial/total?buscar=" + java.net.URLEncoder.encode(textoBusqueda, java.nio.charset.StandardCharsets.UTF_8)
                    + "&criterio=" + java.net.URLEncoder.encode(textoCriterio, java.nio.charset.StandardCharsets.UTF_8)
                    + "&mes=" + mes;

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = http.send(
                    peticion,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (respuesta.statusCode() == 200) {
                return Double.parseDouble(respuesta.body());
            }

        } catch (Exception e) {
            System.out.println("❌ Error al obtener el total global: " + e.getMessage());
        }
        return 0.0;
    }
}