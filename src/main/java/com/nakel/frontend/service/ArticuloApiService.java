package com.nakel.frontend.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.Articulo;

import java.lang.reflect.Type;
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

    // 📋 NUEVO: Para llenar la tabla del catálogo
    public List<Articulo> obtenerTodos() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                Type listType = new TypeToken<ArrayList<Articulo>>(){}.getType();
                return gson.fromJson(respuesta.body(), listType);
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
                    .uri(URI.create(API_URL + "/" + articulo.getId())) // Usa el ID para actualizar
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonArticulo)) // Usamos PUT para modificar
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

            // 200 OK o 204 No Content significan que lo borró con éxito
            if (respuesta.statusCode() == 200 || respuesta.statusCode() == 204) {
                return true;
            } else {
                // Si devuelve cualquier otro código (ej: 500 por clave foránea, o 404 si no existe)
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

    // 📋 NUEVO: Para el paginador del catálogo (Devuelve el JSON completo con páginas)
    // 📋 Catálogo paginado + filtros
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

            // 🔍 Buscar por nombre o código
            if (buscar != null && !buscar.isBlank()) {
                url.append("&buscar=")
                        .append(java.net.URLEncoder.encode(
                                buscar.trim(),
                                java.nio.charset.StandardCharsets.UTF_8
                        ));
            }

            // 🏷️ Filtrar por categoría
            if (categoriaId != null) {
                url.append("&categoriaId=").append(categoriaId);
            }

            // 🧵 Filtrar por material
            if (materialId != null) {
                url.append("&materialId=").append(materialId);
            }

            // 🔥 Filtrar por origen
            if (origen != null && !origen.isBlank()) {
                url.append("&origen=")
                        .append(java.net.URLEncoder.encode(
                                origen.trim(),
                                java.nio.charset.StandardCharsets.UTF_8
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

            System.out.println(
                    "⚠️ Error al obtener catálogo. Código HTTP: "
                            + respuesta.statusCode()
            );

            System.out.println("Respuesta: " + respuesta.body());

        } catch (Exception e) {
            System.out.println(
                    "❌ Error al obtener catálogo paginado: "
                            + e.getMessage()
            );
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

            // 🔎 Texto: nombre o código
            if (buscar != null && !buscar.isBlank()) {
                url.append("&buscar=")
                        .append(java.net.URLEncoder.encode(
                                buscar.trim(),
                                java.nio.charset.StandardCharsets.UTF_8
                        ));
            }

            // 🏷️ Categoría
            if (categoriaId != null) {
                url.append("&categoriaId=").append(categoriaId);
            }

            // 🧵 Material
            if (materialId != null) {
                url.append("&materialId=").append(materialId);
            }

            // 🔥 Origen
            if (origen != null && !origen.isBlank()) {
                url.append("&origen=")
                        .append(java.net.URLEncoder.encode(
                                origen,
                                java.nio.charset.StandardCharsets.UTF_8
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

            System.out.println(
                    "⚠️ Error al buscar artículos. Código: "
                            + respuesta.statusCode()
            );

        } catch (Exception e) {
            System.out.println(
                    "❌ Error al buscar artículos: "
                            + e.getMessage()
            );
        }

        return "[]";
    }
}