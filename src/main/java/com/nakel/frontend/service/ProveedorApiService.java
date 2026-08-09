package com.nakel.frontend.service;

import com.google.gson.Gson;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ProveedorApiService {

    private static final String API_URL = "http://localhost:8080/api/proveedores";
    private final HttpClient clienteHttp = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    // =============== 1. TRAER PROVEEDORES PAGINADOS ===============
    public String obtenerProveedores(int pagina, int cantidadPorPagina) {
        try {
            String url = API_URL + "?page=" + pagina + "&size=" + cantidadPorPagina;

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = clienteHttp.send(
                    peticion,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }

        } catch (Exception e) {
            System.out.println("Error al traer los proveedores paginados: " + e.getMessage());
        }

        return "{\"content\":[],\"totalElements\":0,\"totalPages\":0,\"number\":0,\"size\":"
                + cantidadPorPagina + "}";
    }

    // =============== 1.1 TRAER PROVEEDORES PAGINADOS + BÚSQUEDA GLOBAL ===============
    public String buscarProveedores(
            String texto,
            String campoBusqueda,
            int pagina,
            int cantidadPorPagina) {

        try {
            String parametroTexto = URLEncoder.encode(
                    texto,
                    StandardCharsets.UTF_8
            );

            String parametroCampo = URLEncoder.encode(
                    campoBusqueda,
                    StandardCharsets.UTF_8
            );

            String url = API_URL
                    + "?buscar=" + parametroTexto
                    + "&campo=" + parametroCampo
                    + "&page=" + pagina
                    + "&size=" + cantidadPorPagina;

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = clienteHttp.send(
                    peticion,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }

        } catch (Exception e) {
            System.out.println("Error en la búsqueda de proveedores: " + e.getMessage());
        }

        return "{\"content\":[],\"totalElements\":0,\"totalPages\":0,\"number\":0,\"size\":"
                + cantidadPorPagina + "}";
    }

    // =============== 1.2 TRAER PROVEEDORES POR DEFECTO ===============
    public String obtenerProveedores() {
        return obtenerProveedores(0, 20);
    }

    // =============== 2. GUARDAR PROVEEDORES ===============
    public void guardarProveedoresEnBaseDeDatos(
            String razonSocial,
            String nombreContacto,
            String rubro,
            String cuit,
            String telefono,
            String email,
            BigDecimal saldoFavor,
            BigDecimal saldoContra,
            String comentarios) throws Exception {

        Map<String, Object> datosProveedor = new HashMap<>();
        datosProveedor.put("razonSocial", razonSocial);
        datosProveedor.put("nombreContacto", nombreContacto);
        datosProveedor.put("rubro", rubro);
        datosProveedor.put("cuit", cuit);
        datosProveedor.put("telefono", telefono);
        datosProveedor.put("email", email);
        datosProveedor.put("saldoFavor", saldoFavor);
        datosProveedor.put("saldoContra", saldoContra);
        datosProveedor.put("comentarios", comentarios);

        String jsonMandar = gson.toJson(datosProveedor);

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMandar))
                .build();

        HttpResponse<String> respuesta = clienteHttp.send(
                peticion,
                HttpResponse.BodyHandlers.ofString()
        );

        if (respuesta.statusCode() == 400) {
            throw new RuntimeException(respuesta.body());
        } else if (respuesta.statusCode() != 200 && respuesta.statusCode() != 201) {
            throw new RuntimeException(
                    "Error al comunicarse con el servidor (Código: "
                            + respuesta.statusCode() + ")"
            );
        }
    }

    // =============== 3. ACTUALIZAR PROVEEDORES ===============
    public void actualizarProveedoresEnBaseDeDatos(
            Long id,
            String razonSocial,
            String nombreContacto,
            String rubro,
            String cuit,
            String telefono,
            String email,
            BigDecimal saldoFavor,
            BigDecimal saldoContra,
            String comentarios) throws Exception {

        Map<String, Object> datosProveedor = new HashMap<>();
        datosProveedor.put("id", id);
        datosProveedor.put("razonSocial", razonSocial);
        datosProveedor.put("nombreContacto", nombreContacto);
        datosProveedor.put("rubro", rubro);
        datosProveedor.put("cuit", cuit);
        datosProveedor.put("telefono", telefono);
        datosProveedor.put("email", email);
        datosProveedor.put("saldoFavor", saldoFavor);
        datosProveedor.put("saldoContra", saldoContra);
        datosProveedor.put("comentarios", comentarios);

        String jsonMandar = gson.toJson(datosProveedor);

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/" + id))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonMandar))
                .build();

        HttpResponse<String> respuesta = clienteHttp.send(
                peticion,
                HttpResponse.BodyHandlers.ofString()
        );

        if (respuesta.statusCode() == 400 || respuesta.statusCode() == 500) {
            throw new RuntimeException(
                    "Error del servidor: " + respuesta.body()
            );
        } else if (respuesta.statusCode() != 200) {
            throw new RuntimeException(
                    "Error al comunicarse con el servidor (Código: "
                            + respuesta.statusCode() + ")"
            );
        }
    }

    // =============== 4. ELIMINAR PROVEEDOR ===============
    public void eliminarProveedorDeBaseDeDatos(Long id) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/" + id))
                .DELETE()
                .build();

        HttpResponse<String> respuesta = clienteHttp.send(
                peticion,
                HttpResponse.BodyHandlers.ofString()
        );

        if (respuesta.statusCode() != 200 && respuesta.statusCode() != 204) {
            throw new Exception(
                    "Error " + respuesta.statusCode() + ": " + respuesta.body()
            );
        }
    }

    // =============== 5. BÚSQUEDA PREDICTIVA GLOBAL ===============
    // Mantiene el nombre del método viejo para no romper el código existente.
    // Ahora la búsqueda se hace sobre TODA LA BASE DE DATOS.
    public String buscarProveedorPorNombre(String nombre) {
        return buscarProveedorPorNombre(nombre, 0, 20);
    }

    // =============== 5.1 BÚSQUEDA GLOBAL + PAGINACIÓN ===============
    public String buscarProveedorPorNombre(
            String nombre,
            int pagina,
            int cantidadPorPagina) {

        return buscarProveedores(
                nombre,
                "Empresa",
                pagina,
                cantidadPorPagina
        );
    }

    // =============== 6. BUSCAR POR CUIT EXACTO ===============
    public String buscarProveedorPorCuit(String cuit) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/cuit/" + cuit))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = clienteHttp.send(
                    peticion,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (respuesta.statusCode() == 200) {
                return respuesta.body();
            }

        } catch (Exception e) {
            System.out.println("Error al verificar CUIT: " + e.getMessage());
        }

        return null;
    }
}
