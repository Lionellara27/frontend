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
                    .uri(URI.create(API_URL + "/actual?username="
                            + java.net.URLEncoder.encode(username != null ? username : "", java.nio.charset.StandardCharsets.UTF_8)))
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
    // =============== 3. TRAER HISTORIAL PAGINADO (¡NUEVO!) ===============
    public String obtenerHistorial(int pagina, int cantidadPorPagina) {
        return obtenerHistorial(pagina, cantidadPorPagina, 0, 0, null);
    }

    // Total de ingresos de TODAS las cajas del período (no solo de la página). null si no se pudo consultar.
    public java.math.BigDecimal obtenerTotalHistorial(int mes, int anio) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/historial/total?mes=" + mes + "&anio=" + anio))
                    .GET()
                    .build();
            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() == 200) {
                return new java.math.BigDecimal(respuesta.body().trim());
            }
        } catch (Exception e) {
            System.out.println("Error al obtener el total del historial de cajas: " + e.getMessage());
        }
        return null;
    }

    // mes: 1-12 (0 = todos). anio: ej 2026 (0 = todos). orden: "campo,asc|desc" (null = más recientes primero)
    public String obtenerHistorial(int pagina, int cantidadPorPagina, int mes, int anio, String orden) {
        try {
            String url = API_URL + "/historial?page=" + pagina + "&size=" + cantidadPorPagina
                    + "&mes=" + mes + "&anio=" + anio
                    + (orden != null && !orden.isBlank() ? "&sort=" + orden : "");
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }
        } catch (Exception e) {
            System.out.println("❌ Error al obtener historial paginado: " + e.getMessage());
        }
        return "[]";
    }

    // =============== 3.1 TRAER HISTORIAL (Por Defecto) ===============
    // Mantenemos este para no romper código viejo si lo usás en otro lado
    public String obtenerHistorial() {
        return obtenerHistorial(0, 20);
    }
}