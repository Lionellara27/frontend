package com.nakel.frontend.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Llamadas de la pestaña "Precios y Dólar": historial de aumentos, deshacer, cotización por internet
 * y actualización de precios según el dólar.
 *
 * Todas devuelven una {@link Respuesta}: si salió bien trae el JSON; si no, el mensaje que explica por qué
 * (el backend ya lo manda en castellano), para poder mostrárselo tal cual a la usuaria.
 */
public class PreciosApiService {

    private static final String API = "http://localhost:8080/api";
    private final HttpClient http = HttpClient.newHttpClient();

    public record Respuesta(boolean ok, String cuerpo) {}

    /** Página del historial de cambios de precios (de a 7, del más nuevo al más viejo). */
    public Respuesta historial(int pagina, int tamanio) {
        return pedir("GET", "/articulos/aumentos?page=" + pagina + "&size=" + tamanio);
    }

    /** Deshace un cambio de precios (el backend solo lo permite dentro de los 7 días). */
    public Respuesta deshacer(long idAumento) {
        return pedir("POST", "/articulos/aumentos/" + idAumento + "/deshacer");
    }

    /** Consulta en internet el dólar del tipo elegido: oficial, blue, tarjeta o bolsa. */
    public Respuesta cotizacionOnline(String tipo) {
        return pedir("GET", "/configuracion/divisa/cotizacion-online?tipo=" + tipo);
    }

    /** Lleva todos los precios al dólar guardado hoy. */
    public Respuesta actualizarPreciosSegunDolar() {
        return pedir("POST", "/configuracion/divisa/actualizar-precios");
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
