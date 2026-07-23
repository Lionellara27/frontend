package com.nakel.frontend.service;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class UsuarioApiService {

    private static final String API_URL = "http://localhost:8080/api/usuarios";
    private final HttpClient clienteHttp = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    // =============== 1. LOGIN DE USUARIO (POST) ===============
    public boolean login(String username, String password) {
        try {
            // Usamos tu estilo con HashMap para armar el JSON de forma segura
            Map<String, String> credenciales = new HashMap<>();
            credenciales.put("username", username);
            credenciales.put("password", password);

            String jsonMandar = gson.toJson(credenciales);

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonMandar))
                    .build();

            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            // Si el backend devuelve 200 OK, el login es exitoso
            if (respuesta.statusCode() == 200) {
                return true;
            } else {
                System.out.println("⚠️ Intento de login fallido: " + respuesta.body());
                return false;
            }
        } catch (Exception e) {
            System.out.println("❌ Error de red al intentar loguear: " + e.getMessage());
            return false;
        }
    }

    // =============== 2. ACTUALIZAR CREDENCIALES (PUT) ===============
    public boolean actualizarCredenciales(String usernameActual, String nuevoUsuario, String nuevaContrasena) {
        try {
            Map<String, String> datos = new HashMap<>();
            datos.put("usernameActual", usernameActual);
            datos.put("nuevoUsuario", nuevoUsuario);
            datos.put("nuevaContrasena", nuevaContrasena);

            String jsonMandar = gson.toJson(datos);

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "/actualizar"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonMandar))
                    .build();

            HttpResponse<String> respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() == 200) {
                return true;
            } else {
                System.out.println("⚠️ Error al actualizar: " + respuesta.body());
                return false;
            }
        } catch (Exception e) {
            System.out.println("❌ Error de red al actualizar credenciales: " + e.getMessage());
            return false;
        }
    }
}