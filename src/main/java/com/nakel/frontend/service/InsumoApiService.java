package com.nakel.frontend.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.Insumo;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;

public class InsumoApiService {

    private static final String API_URL = "http://localhost:8080/api/insumos";
    private final HttpClient insumoHttp;
    private final Gson gson;

    public InsumoApiService() {
        this.insumoHttp = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    // =============== 1. OBTENER INSUMOS PAGINADOS (¡NUEVO!) ===============
    public String obtenerInsumos(int pagina, int cantidadPorPagina) {
        try {
            // Le pedimos al backend la página exacta (Ojo: Spring Boot empieza a contar desde 0)
            String urlConPaginacion = API_URL + "?page=" + pagina + "&size=" + cantidadPorPagina;

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(urlConPaginacion))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = insumoHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }
        } catch (Exception e) {
            System.out.println("Error de conexión al obtener insumos paginados: " + e.getMessage());
        }
        return "[]";
    }

    // =============== 1.1 OBTENER INSUMOS (Por Defecto) ===============
    // Mantenemos este para que no se rompa tu controlador actual.
    // Por defecto, traerá la Página 0 con 20 elementos.
    public String obtenerInsumos() {
        return obtenerInsumos(0, 20);
    }

    // =============== 2. BUSCADOR PREDICTIVO ===============
    public String buscarInsumosPorNombre(String nombre) {
        try {
            String parametro = nombre.replace(" ", "%20");
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/buscar?nombre=" + parametro))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = insumoHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }
        } catch (Exception e) {
            System.out.println("Error en la búsqueda predictiva de insumos: " + e.getMessage());
        }
        return "[]";
    }

    // =============== 2.1 BUSCADOR PARA EL COMBOBOX DE LA CALCULADORA (independiente) ===============
    // No toca el método buscarInsumosPorNombre() que ya usa el módulo de ver insumos
    public String buscarInsumosParaCombo(String nombre) {
        try {
            String parametro = nombre.replace(" ", "%20");
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/buscar?nombre=" + parametro + "&size=50"))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = insumoHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }
        } catch (Exception e) {
            System.out.println("Error en la búsqueda del combo de insumos: " + e.getMessage());
        }
        return "[]";
    }
    // =============== 2.2 BUSCADOR PAGINADO PARA LA CALCULADORA (¡NUEVO!) ===============
    public List<Insumo> buscarInsumosParaCalculadora(String nombre, int pagina, int cantidadPorPagina) {
        try {
            // Limpiamos y preparamos el texto para la URL
            String parametro = (nombre != null && !nombre.isBlank()) ? nombre.replace(" ", "%20") : "";

            // Armamos la URL exacta con la palabra a buscar, la página y la cantidad
            String url = API_URL + "/buscar?nombre=" + parametro + "&page=" + pagina + "&size=" + cantidadPorPagina;

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = insumoHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                String json = respuesta.body();
                com.google.gson.JsonElement elemento = com.google.gson.JsonParser.parseString(json);
                com.google.gson.JsonArray arrayInsumos;

                // 🧠 Inteligencia del frontend: Si viene paginado (Spring Boot), extrae "content"
                if (elemento.isJsonObject() && elemento.getAsJsonObject().has("content")) {
                    arrayInsumos = elemento.getAsJsonObject().getAsJsonArray("content");
                } else if (elemento.isJsonArray()) {
                    // Si viene como lista directa por algún motivo
                    arrayInsumos = elemento.getAsJsonArray();
                } else {
                    arrayInsumos = new com.google.gson.JsonArray();
                }

                Type tipo = new TypeToken<List<Insumo>>() {}.getType();
                return gson.fromJson(arrayInsumos, tipo);
            } else {
                System.out.println("⚠️ Error del backend al buscar insumos para calculadora: Código " + respuesta.statusCode());
            }
        } catch (Exception e) {
            System.out.println("❌ Excepción en la búsqueda paginada de la calculadora: " + e.getMessage());
            e.printStackTrace();
        }

        // Si algo falla, devolvemos una lista vacía para que no explote el ComboBox
        return Collections.emptyList();
    }
    // =============== 3. GUARDAR INSUMO (POST BLINDADO) ===============
    public void guardarInsumoEnBaseDeDatos(Insumo insumo) throws Exception {

        String jsonMandar = gson.toJson(insumo);

        System.out.println("========== GUARDAR INSUMO (FRONTEND) ==========");
        System.out.println("JSON ENVIADO: " + jsonMandar);

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMandar))
                .build();

        HttpResponse<String> respuesta = insumoHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

        System.out.println("STATUS: " + respuesta.statusCode());
        System.out.println("BODY DEVUELTO: " + respuesta.body());
        System.out.println("===============================================");

        if (respuesta.statusCode() == 400) {
            throw new RuntimeException(respuesta.body());
        } else if (respuesta.statusCode() != 200 && respuesta.statusCode() != 201) {
            throw new RuntimeException("Error al comunicarse con el servidor (Código: " + respuesta.statusCode() + ")");
        }
    }

    // =============== 3.5. ACTUALIZAR INSUMO (PUT) ===============
    public void actualizarInsumoEnBaseDeDatos(Long id, Insumo insumo) throws Exception {

        String jsonMandar = gson.toJson(insumo);

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/" + id))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonMandar))
                .build();

        HttpResponse<String> respuesta = insumoHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() == 400 || respuesta.statusCode() == 500) {
            throw new RuntimeException("Error del servidor: " + respuesta.body());
        } else if (respuesta.statusCode() != 200) {
            throw new RuntimeException("Error al comunicarse con el servidor (Código: " + respuesta.statusCode() + ")");
        }
    }

    // =============== 4. ELIMINAR INSUMO ===============
    public void eliminarInsumoDeBaseDeDatos(Long id) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/" + id))
                .DELETE()
                .build();

        HttpResponse<String> respuesta = insumoHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() != 200 && respuesta.statusCode() != 204) {
            throw new Exception("Error " + respuesta.statusCode() + ": " + respuesta.body());
        }
    }

    // =============== 5. LISTA COMPLETA DIRECTA (Soporte viejo) ===============
    public List<Insumo> obtenerListaInsumos() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/lista"))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = insumoHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                Type tipo = new TypeToken<List<Insumo>>() {}.getType();
                return gson.fromJson(respuesta.body(), tipo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    // =============== 6. OBTENER INSUMOS PAGINADOS YA PARSEADOS (para el ComboBox) ===============
    public List<Insumo> obtenerInsumosPaginadosComoLista(int pagina, int cantidadPorPagina) {
        try {
            String json = obtenerInsumos(pagina, cantidadPorPagina);

            com.google.gson.JsonElement elemento = com.google.gson.JsonParser.parseString(json);
            com.google.gson.JsonArray arrayInsumos;

            // Igual que con clientes: puede venir paginado (con "content") o como lista directa
            if (elemento.isJsonObject() && elemento.getAsJsonObject().has("content")) {
                arrayInsumos = elemento.getAsJsonObject().getAsJsonArray("content");
            } else if (elemento.isJsonArray()) {
                arrayInsumos = elemento.getAsJsonArray();
            } else {
                arrayInsumos = new com.google.gson.JsonArray();
            }

            Type tipo = new TypeToken<List<Insumo>>() {}.getType();
            return gson.fromJson(arrayInsumos, tipo);

        } catch (Exception e) {
            System.out.println("Error al parsear insumos paginados: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    /// =============== 7. BUSCADOR YA PARSEADO (para el ComboBox) ===============
    public List<Insumo> buscarInsumosComoLista(String nombre) {
        try {
            String json = buscarInsumosParaCombo(nombre); // 🔥 usa el método nuevo, no el compartido

            com.google.gson.JsonElement elemento = com.google.gson.JsonParser.parseString(json);
            com.google.gson.JsonArray arrayInsumos;

            if (elemento.isJsonObject() && elemento.getAsJsonObject().has("content")) {
                arrayInsumos = elemento.getAsJsonObject().getAsJsonArray("content");
            } else if (elemento.isJsonArray()) {
                arrayInsumos = elemento.getAsJsonArray();
            } else {
                arrayInsumos = new com.google.gson.JsonArray();
            }

            Type tipo = new TypeToken<List<Insumo>>() {}.getType();
            return gson.fromJson(arrayInsumos, tipo);

        } catch (Exception e) {
            System.out.println("Error al parsear búsqueda de insumos: " + e.getMessage());
        }
        return Collections.emptyList();
    }
}