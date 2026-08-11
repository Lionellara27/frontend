package com.nakel.frontend.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.Articulo;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class ArticuloApiService {

    private static final String API_URL = "http://localhost:8080/api/articulos";
    private final HttpClient http = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    // 🔥 Variable de memoria para el paginador del mostrador
    private int ultimasPaginasMostrador = 1;

    public int getUltimasPaginasMostrador() {
        return ultimasPaginasMostrador == 0 ? 1 : ultimasPaginasMostrador;
    }

    // 🔫 El método que usa la Pistola Láser (mostrador)
    public String buscarArticuloPorCodigo(String codigoBarras) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/codigo/" + codigoBarras))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }
        } catch (Exception e) {
            System.out.println("Error al buscar artículo: " + e.getMessage());
        }
        return null;
    }

    // 📋 NUEVO: Para llenar la tabla del catálogo (Ahora soporta Paginación)
    public List<Articulo> obtenerTodos() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                com.google.gson.JsonElement elementoParseado = com.google.gson.JsonParser.parseString(respuesta.body());
                com.google.gson.JsonArray arregloArticulos;

                if (elementoParseado.isJsonObject() && elementoParseado.getAsJsonObject().has("content")) {
                    arregloArticulos = elementoParseado.getAsJsonObject().getAsJsonArray("content");
                } else if (elementoParseado.isJsonArray()) {
                    arregloArticulos = elementoParseado.getAsJsonArray();
                } else {
                    return new ArrayList<>();
                }

                Type listType = new TypeToken<ArrayList<Articulo>>(){}.getType();
                return gson.fromJson(arregloArticulos, listType);
            }
        } catch (Exception e) {
            System.out.println("Error al obtener el catálogo: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    // 💾 NUEVO: Para el botón "➕ Nuevo Artículo"
    public boolean guardarArticulo(Articulo articulo) {
        try {
            String jsonArticulo = gson.toJson(articulo);

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonArticulo))
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            return respuesta.statusCode() == 200 || respuesta.statusCode() == 201;

        } catch (Exception e) {
            System.out.println("Error al guardar el artículo: " + e.getMessage());
            return false;
        }
    }

    // 🔄 NUEVO: Actualizar artículo (Para el botón Editar ✏️)
    public boolean actualizarArticulo(Articulo articulo) {
        try {
            String jsonArticulo = gson.toJson(articulo);
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/" + articulo.getId()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonArticulo))
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            return respuesta.statusCode() == 200 || respuesta.statusCode() == 201;
        } catch (Exception e) {
            System.out.println("Error al actualizar el artículo: " + e.getMessage());
            return false;
        }
    }

    // 🔥 ELIMINAR TOTALMENTE BLINDADO (Sin throws Exception, atrapa los errores)
    public boolean eliminarArticuloDeBaseDeDatos(Long id) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/" + id))
                    .DELETE()
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200 || respuesta.statusCode() == 204) {
                return true;
            } else {
                System.out.println("⚠️ El servidor rechazó el borrado. Código: " + respuesta.statusCode());
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error interno al intentar eliminar: " + e.getMessage());
            return false;
        }
    }

    // 🔍 NUEVO: Buscar producto para la pantalla de Cambios (por código)
    public String buscarProducto(String busqueda) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/codigo/" + busqueda))
                .GET()
                .build();

        HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

        if (respuesta.statusCode() == 200) {
            return respuesta.body();
        } else if (respuesta.statusCode() == 404) {
            return null;
        } else {
            throw new Exception("Error del servidor: " + respuesta.statusCode());
        }
    }

    // 🔥 Suma stock (cuando el cliente devuelve algo)
    public boolean restaurarStock(Long idArticulo, int cantidad) {
        try {
            String url = API_URL + "/" + idArticulo + "/restaurar-stock?cantidad=" + cantidad;
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            return respuesta.statusCode() == 200;
        } catch (Exception e) {
            System.out.println("❌ Error al restaurar stock: " + e.getMessage());
            return false;
        }
    }

    // 🔥 Resta stock (cuando el cliente se lleva algo nuevo por el cambio)
    public boolean descontarStock(Long idArticulo, int cantidad) {
        try {
            String url = API_URL + "/" + idArticulo + "/descontar-stock?cantidad=" + cantidad;
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            return respuesta.statusCode() == 200;
        } catch (Exception e) {
            System.out.println("❌ Error al descontar stock: " + e.getMessage());
            return false;
        }
    }

    // 📋 CATÁLOGO PAGINADO + FILTROS
    public String obtenerArticulosPaginados(
            int pagina,
            int cantidadPorPagina,
            String buscar,
            Long categoriaId,
            Long materialId,
            String origen) {

        try {
            StringBuilder url = new StringBuilder(API_URL);
            url.append("?page=").append(pagina);
            url.append("&size=").append(cantidadPorPagina);

            if (buscar != null && !buscar.isBlank()) {
                url.append("&buscar=")
                        .append(java.net.URLEncoder.encode(
                                buscar.trim(),
                                "UTF-8"
                        ));
            }

            if (categoriaId != null) {
                url.append("&categoriaId=").append(categoriaId);
            }

            if (materialId != null) {
                url.append("&materialId=").append(materialId);
            }

            if (origen != null && !origen.isBlank()) {
                url.append("&origen=")
                        .append(java.net.URLEncoder.encode(
                                origen.trim(),
                                "UTF-8"
                        ));
            }

            System.out.println("🌐 GET artículos: " + url);

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = http.send(
                    peticion,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }

            System.out.println("⚠️ Error al obtener catálogo. Código HTTP: " + respuesta.statusCode());

        } catch (Exception e) {
            System.out.println("❌ Error al obtener catálogo paginado: " + e.getMessage());
        }

        return "[]";
    }

    // 🔍 BUSCAR Y FILTRAR ARTÍCULOS PAGINADOS
    public String buscarArticulos(
            String buscar,
            Long categoriaId,
            Long materialId,
            String origen,
            int pagina,
            int cantidadPorPagina) {

        try {
            StringBuilder url = new StringBuilder(API_URL);
            url.append("?page=").append(pagina);
            url.append("&size=").append(cantidadPorPagina);

            if (buscar != null && !buscar.isBlank()) {
                url.append("&buscar=")
                        .append(java.net.URLEncoder.encode(
                                buscar.trim(),
                                "UTF-8"
                        ));
            }

            if (categoriaId != null) {
                url.append("&categoriaId=").append(categoriaId);
            }

            if (materialId != null) {
                url.append("&materialId=").append(materialId);
            }

            if (origen != null && !origen.isBlank()) {
                url.append("&origen=")
                        .append(java.net.URLEncoder.encode(
                                origen,
                                "UTF-8"
                        ));
            }

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .GET()
                    .build();

            HttpResponse<String> respuesta =
                    http.send(
                            peticion,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }

            System.out.println("⚠️ Error al buscar artículos. Código: " + respuesta.statusCode());

        } catch (Exception e) {
            System.out.println("❌ Error al buscar artículos: " + e.getMessage());
        }

        return "[]";
    }

    // 🔍 BÚSQUEDA PARA EL MOSTRADOR (Paginada, con stock y memoria de páginas)
    public List<Articulo> buscarParaVenta(String busqueda, int pagina, int size) {
        try {
            String textoBusqueda = (busqueda != null) ? busqueda.trim() : "";

            String url = API_URL
                    + "?page=" + pagina
                    + "&size=" + size
                    + "&buscar=" + java.net.URLEncoder.encode(textoBusqueda, "UTF-8")
                    + "&stockDisponible=true";

            System.out.println("🌐 GET búsqueda Mostrador paginada: " + url);

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = http.send(
                    peticion,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (respuesta.statusCode() == 200) {
                com.google.gson.JsonElement elementoParseado =
                        com.google.gson.JsonParser.parseString(respuesta.body());

                if (elementoParseado.isJsonObject()) {
                    com.google.gson.JsonObject json = elementoParseado.getAsJsonObject();

                    // 🔥 LA MAGIA: Guardamos el total de páginas
                    if (json.has("totalPages")) {
                        this.ultimasPaginasMostrador = json.get("totalPages").getAsInt();
                    }

                    if (json.has("content")) {
                        Type listType = new TypeToken<ArrayList<Articulo>>() {}.getType();
                        return gson.fromJson(json.getAsJsonArray("content"), listType);
                    }
                } else if (elementoParseado.isJsonArray()) {
                    this.ultimasPaginasMostrador = 1;
                    Type listType = new TypeToken<ArrayList<Articulo>>() {}.getType();
                    return gson.fromJson(elementoParseado.getAsJsonArray(), listType);
                }
            }

            System.out.println("⚠️ Error al buscar productos para venta. Código: " + respuesta.statusCode());

        } catch (Exception e) {
            System.out.println("❌ Error en búsqueda para venta: " + e.getMessage());
        }

        return new ArrayList<>();
    }

    // 🔥 AUMENTO MASIVO DE PRECIOS
// categoriaId == null → aumenta TODOS los artículos
// categoriaId != null → aumenta solamente esa categoría
    public int aumentarPrecios(Long categoriaId, BigDecimal porcentaje) {
        try {
            StringBuilder url = new StringBuilder(
                    API_URL + "/aumentar-precios?porcentaje=" + porcentaje
            );

            if (categoriaId != null) {
                url.append("&categoriaId=").append(categoriaId);
            }

            System.out.println("========== AUMENTO MASIVO ==========");
            System.out.println("📦 Categoría ID: " + categoriaId);
            System.out.println("📈 Porcentaje: " + porcentaje);
            System.out.println("🌐 URL: " + url);

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> respuesta = http.send(
                    peticion,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("📥 HTTP: " + respuesta.statusCode());
            System.out.println("📥 BODY: " + respuesta.body());

            if (respuesta.statusCode() == 200) {
                try {
                    return Integer.parseInt(respuesta.body().trim());
                } catch (NumberFormatException e) {
                    System.out.println(
                            "⚠️ El backend respondió correctamente, " +
                                    "pero no devolvió una cantidad válida."
                    );
                    return -1;
                }
            }

            System.out.println(
                    "⚠️ El servidor rechazó el aumento. Código: "
                            + respuesta.statusCode()
            );

            return -1;

        } catch (Exception e) {
            System.out.println(
                    "❌ Error al aplicar aumento masivo: "
                            + e.getMessage()
            );
            e.printStackTrace();
            return -1;
        }
    }

    // 📦 Objeto que representa lo que enviamos al backend
//    private static class AumentoPreciosRequest {
//
//        private final Long categoriaId;
//        private final double porcentaje;
//
//        public AumentoPreciosRequest(Long categoriaId, double porcentaje) {
//            this.categoriaId = categoriaId;
//            this.porcentaje = porcentaje;
//        }
//    }
    public static class AumentoPreciosRequest {
        public Long categoriaId;
        public BigDecimal porcentaje;

        public AumentoPreciosRequest(Long categoriaId, BigDecimal porcentaje) {
            this.categoriaId = categoriaId;
            this.porcentaje = porcentaje;
        }
    }
}