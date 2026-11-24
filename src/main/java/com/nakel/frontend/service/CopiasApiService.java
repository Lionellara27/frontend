package com.nakel.frontend.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Llamadas de la pestaña "Copias de seguridad": ver las copias que hay, hacer una ahora y restaurar.
 *
 * Todas devuelven una {@link Respuesta}: si salió bien trae el JSON; si no, el mensaje que explica por qué
 * (el backend ya lo manda en castellano), para poder mostrárselo tal cual a la usuaria.
 */
public class CopiasApiService {

    private static final String API = "http://localhost:8080/api/copias";
    private final HttpClient http = HttpClient.newHttpClient();

    public record Respuesta(boolean ok, String cuerpo) {}

    /** Las copias guardadas, si ya se hizo la de hoy y si hay algo raro con la base. */
    public Respuesta listar() {
        return pedir("GET", "");
    }

    /** Hace la copia de hoy en este momento (pisa la que hubiera de este día de la semana). */
    public Respuesta hacerAhora() {
        return pedir("POST", "/ahora");
    }

    /** Vuelve la base a como estaba en la copia de ese día (LUNES ... DOMINGO, o ANTES_DE_RESTAURAR). */
    public Respuesta restaurar(String dia) {
        return pedir("POST", "/" + dia + "/restaurar");
    }

    private Respuesta pedir(String metodo, String ruta) {
        try {
            HttpRequest.Builder peticion = HttpRequest.newBuilder().uri(URI.create(API + ruta));
            if ("POST".equals(metodo)) peticion.POST(HttpRequest.BodyPublishers.noBody());
            else peticion.GET();

            HttpResponse<String> respuesta = http.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
            String cuerpo = respuesta.body() == null ? "" : respuesta.body();
            if (respuesta.statusCode() == 200) return new Respuesta(true, cuerpo);

            // Los rechazos del backend vienen como texto simple; un error inesperado viene como JSON técnico
            boolean esTecnico = cuerpo.trim().startsWith("{");
            return new Respuesta(false, esTecnico || cuerpo.isBlank()
                    ? "El sistema no pudo completar la operación (código " + respuesta.statusCode() + ")."
                    : cuerpo);
        } catch (Exception e) {
            return new Respuesta(false, "No se pudo conectar con el sistema. Revisá que esté iniciado y probá de nuevo.");
        }
    }
}
