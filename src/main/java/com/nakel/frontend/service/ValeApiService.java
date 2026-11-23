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

    // Usuario de la sesión, listo para ir en una URL (puede tener espacios)
    private static String usuarioCodificado() {
        String usuario = com.nakel.frontend.util.SesionActual.getUsuarioLogueado();
        return java.net.URLEncoder.encode(usuario != null ? usuario : "", java.nio.charset.StandardCharsets.UTF_8);
    }

    // 🔥 1. Ahora recibe el monto y el idCliente (puede ser null)
    public Vale generarVale(Double monto, Long idCliente) {
        return generarVale(monto, idCliente, null);
    }

    // Motivo del último rechazo al generar un vale, para mostrarlo en pantalla
    private String ultimoError;

    public String getUltimoError() {
        return ultimoError;
    }

    // Cantidad de páginas del último listado pedido
    private int ultimasPaginas = 1;

    public int getUltimasPaginas() {
        return ultimasPaginas <= 0 ? 1 : ultimasPaginas;
    }

    // Vales vigentes de un cliente: el detalle de su saldo a favor. null si no se pudo consultar.
    public java.util.List<Vale> valesVigentesDeCliente(Long idCliente) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder().uri(URI.create(API_URL + "/cliente/" + idCliente)).GET().build();
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() == 200) {
                java.lang.reflect.Type tipo = new com.google.gson.reflect.TypeToken<java.util.List<Vale>>() {}.getType();
                java.util.List<Vale> vales = gson.fromJson(respuesta.body(), tipo);
                return vales != null ? vales : new java.util.ArrayList<>();
            }
        } catch (Exception e) {
            System.out.println("Error al consultar los vales del cliente: " + e.getMessage());
        }
        return null;
    }

    // Listado para la pantalla de Vales. soloVigentes = activos que todavía no vencieron.
    public java.util.List<Vale> listarVales(int pagina, int cantidadPorPagina, boolean soloVigentes) {
        try {
            String url = API_URL + "?page=" + pagina + "&size=" + cantidadPorPagina + "&soloVigentes=" + soloVigentes;
            HttpRequest peticion = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                com.google.gson.JsonObject raiz = com.google.gson.JsonParser.parseString(respuesta.body()).getAsJsonObject();
                ultimasPaginas = raiz.has("totalPages") ? raiz.get("totalPages").getAsInt() : 1;
                java.lang.reflect.Type tipo = new com.google.gson.reflect.TypeToken<java.util.List<Vale>>() {}.getType();
                java.util.List<Vale> vales = gson.fromJson(raiz.getAsJsonArray("content"), tipo);
                return vales != null ? vales : new java.util.ArrayList<>();
            }
        } catch (Exception e) {
            System.out.println("Error al listar vales: " + e.getMessage());
        }
        ultimasPaginas = 1;
        return new java.util.ArrayList<>();
    }

    // motivo: por qué se emite el vale (queda guardado y se ve en la caja del día)
    public Vale generarVale(Double monto, Long idCliente, String motivo) {
        ultimoError = null;
        try {
            // Armamos la URL base con el monto
            String url = API_URL + "/generar?monto=" + String.format(java.util.Locale.US, "%.2f", monto);

            // Si hay un cliente registrado, lo agregamos a la URL
            if (idCliente != null) {
                url += "&idCliente=" + idCliente;
            }
            url += "&username=" + usuarioCodificado(); // para asentarlo en la caja del día
            if (motivo != null && !motivo.isBlank()) {
                url += "&motivo=" + java.net.URLEncoder.encode(motivo.trim(), java.nio.charset.StandardCharsets.UTF_8);
            }

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody()) // No mandamos body, va por parámetro
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return gson.fromJson(respuesta.body(), Vale.class);
            }
            String cuerpo = respuesta.body();
            ultimoError = (cuerpo != null && !cuerpo.isBlank() && !cuerpo.trim().startsWith("{"))
                    ? cuerpo.trim()
                    : "El servidor rechazó el vale (código " + respuesta.statusCode() + ").";
        } catch (Exception e) {
            System.out.println("❌ Error al generar vale: " + e.getMessage());
            ultimoError = "No se pudo conectar con el servidor del sistema.";
        }
        return null;
    }

    // 2. Valida si el vale existe, no está vencido ni usado
    public Vale validarVale(String codigo) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/validar/" + java.net.URLEncoder.encode(codigo.trim(), java.nio.charset.StandardCharsets.UTF_8)))
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
        return consumirVale(codigo, null);
    }

    // montoUsado: cuánto del vale se usó en esta compra. Si es menos que el valor del vale,
    // el backend lo deja ACTIVO con el saldo restante en vez de quemarlo entero.
    public boolean consumirVale(String codigo, Double montoUsado) {
        try {
            String url = API_URL + "/consumir/" + java.net.URLEncoder.encode(codigo.trim(), java.nio.charset.StandardCharsets.UTF_8)
                    + "?username=" + usuarioCodificado(); // para asentarlo en la caja del día
            if (montoUsado != null) {
                url += "&montoUsado=" + String.format(java.util.Locale.US, "%.2f", montoUsado);
            }

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
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