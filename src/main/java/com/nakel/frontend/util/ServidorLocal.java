package com.nakel.frontend.util;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Arranca y apaga el "motor" del sistema (el backend) junto con la ventana.
 *
 * Antes lo hacía un archivo .bat que lanzaba el backend, esperaba 5 segundos fijos y abría la ventana:
 * si la computadora tardaba más, el login decía que no podía conectar; y al cerrar la ventana el backend
 * quedaba corriendo escondido. Ahora la propia aplicación lo lanza, espera a que responda de verdad
 * y lo apaga al salir.
 *
 * Cuando se corre desde el editor de código (sin el backend al lado), no hace nada: ahí el backend
 * lo levanta el programador a mano, como siempre.
 */
public final class ServidorLocal {

    private static final String URL_PRUEBA = "http://localhost:8080/api/categorias";
    private static final int SEGUNDOS_DE_ESPERA = 150; // una PC vieja puede tardar bastante en arrancar

    private static Process proceso;      // el backend, si lo arrancamos nosotros
    private static Path registro;        // dónde queda escrito lo que dice el backend al arrancar

    private ServidorLocal() {
    }

    /** ¿El backend ya está contestando? */
    public static boolean responde() {
        // Mientras el backend no abrió su puerto, preguntar por HTTP tarda más de un segundo en fallar.
        // Mirar el puerto directo contesta al instante.
        try (java.net.Socket puerto = new java.net.Socket()) {
            puerto.connect(new java.net.InetSocketAddress("127.0.0.1", 8080), 250);
        } catch (Exception cerrado) {
            return false;
        }
        try {
            HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(1200)).build();
            HttpRequest peticion = HttpRequest.newBuilder().uri(URI.create(URL_PRUEBA))
                    .timeout(Duration.ofMillis(2500)).GET().build();
            return cliente.send(peticion, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /** El archivo del backend instalado al lado de esta aplicación, o null si no está (modo desarrollo). */
    public static File archivoDelBackend() {
        try {
            File aqui = new File(ServidorLocal.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File carpeta = aqui.isFile() ? aqui.getParentFile() : null; // si no es un .jar, estamos en el editor
            if (carpeta == null) return null;
            File[] candidatos = carpeta.listFiles((dir, nombre) ->
                    nombre.toLowerCase().endsWith(".jar") && nombre.toLowerCase().contains("backend"));
            return candidatos != null && candidatos.length > 0 ? candidatos[0] : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Se asegura de que el backend esté andando. Puede tardar: hay que llamarlo fuera del hilo de la pantalla.
     *
     * @param avisar recibe textos cortos para mostrar mientras se espera ("Iniciando el sistema...")
     * @return null si quedó andando; si no, un texto que explica qué pasó
     */
    public static String asegurar(Consumer<String> avisar) {
        String problema = lanzar();
        if (problema != null) return problema;
        if (proceso == null) return null; // ya estaba levantado, o estamos en modo desarrollo
        return esperar(avisar);
    }

    /** ¿Lo lanzó esta aplicación y todavía hay que esperarlo? */
    public static boolean arrancando() {
        return proceso != null && proceso.isAlive();
    }

    /**
     * Lanza el backend si hace falta y vuelve enseguida, sin esperar a que termine de arrancar.
     * Se llama lo antes posible (antes de abrir la ventana) para ganar esos segundos.
     *
     * @return null si salió bien (o no hacía falta); si no, qué pasó
     */
    public static synchronized String lanzar() {
        if (proceso != null) return null; // ya lo lanzamos
        if (responde()) return null;      // ya estaba levantado

        File backend = archivoDelBackend();
        if (backend == null) return null; // modo desarrollo: no es asunto nuestro

        try {
            // El mismo Java con el que corre esta ventana (el que viene dentro del instalador)
            String java = Paths.get(System.getProperty("java.home"), "bin", "javaw.exe").toString();
            if (!new File(java).exists()) java = Paths.get(System.getProperty("java.home"), "bin", "java").toString();

            Path carpetaDatos = Paths.get(System.getProperty("user.home"), "Documents", "NakelSoftware");
            Files.createDirectories(carpetaDatos);
            registro = carpetaDatos.resolve("registro-del-sistema.log");

            // Ajustes para computadoras modestas (la del local es un i3 viejo con 4 GB):
            //  - TieredStopAtLevel=1: arranca bastante más rápido (medido: de ~25 s a ~13 s con un solo núcleo)
            //  - UseSerialGC + Xmx: usa menos memoria (medido: de ~425 MB a ~290 MB)
            ProcessBuilder pb = new ProcessBuilder(java,
                    "-XX:TieredStopAtLevel=1", "-XX:+UseSerialGC", "-Xms64m", "-Xmx512m",
                    "-Dfile.encoding=UTF-8", "-jar", backend.getAbsolutePath());
            pb.directory(backend.getParentFile());
            pb.redirectErrorStream(true);
            pb.redirectOutput(registro.toFile()); // se pisa en cada arranque: siempre tiene el último intento
            proceso = pb.start();

            // Si esta aplicación se cierra de cualquier forma, el backend se va con ella
            Runtime.getRuntime().addShutdownHook(new Thread(ServidorLocal::apagar, "apagar-backend"));
        } catch (Exception e) {
            return "No se pudo iniciar el motor del sistema: " + e.getMessage();
        }
        return null;
    }

    /** Espera a que el backend lanzado conteste. Puede tardar: llamarlo fuera del hilo de la pantalla. */
    private static String esperar(Consumer<String> avisar) {
        long limite = System.currentTimeMillis() + SEGUNDOS_DE_ESPERA * 1000L;
        int vueltas = 0;
        while (System.currentTimeMillis() < limite) {
            if (responde()) return null;
            if (!proceso.isAlive()) {
                return "El motor del sistema se cerró apenas arrancó. Puede que ya haya otro Nakel abierto, "
                        + "o que la base de datos esté dañada.\n\nEl detalle quedó en:\n" + registro;
            }
            if (avisar != null && ++vueltas == 80) avisar.accept("Sigue iniciando… en esta computadora tarda un poco más.");
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Se interrumpió el inicio del sistema.";
            }
        }
        return "El sistema tardó demasiado en iniciar (más de " + SEGUNDOS_DE_ESPERA + " segundos).\n\nEl detalle quedó en:\n" + registro;
    }

    /** Apaga el backend, solo si lo arrancó esta aplicación. Se puede llamar más de una vez. */
    public static synchronized void apagar() {
        if (proceso == null || !proceso.isAlive()) return;
        try {
            proceso.destroy();
            if (!proceso.waitFor(6, TimeUnit.SECONDS)) proceso.destroyForcibly();
        } catch (Exception e) {
            proceso.destroyForcibly();
        }
    }
}
