package com.nakel.frontend.util;

import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.InputStream;
import java.net.URL;

/**
 * Le da a TODAS las ventanas del sistema (alertas, diálogos, modales) el ícono de Nakel
 * y la hoja de estilos de la aplicación, desde un solo lugar.
 *
 * Cómo funciona: JavaFX mantiene la lista de ventanas abiertas (Window.getWindows()).
 * Nos colgamos de esa lista y, cada vez que aparece una ventana nueva, le aplicamos
 * ícono y estilo. Así no hay que acordarse de hacerlo en cada "new Alert(...)" o "new Stage()".
 */
public final class Ventanas {

    private static final String RUTA_ICONO = "/images/FlaviconSin.png";
    private static final String RUTA_CSS = "/css/nakel.css";

    private static Image icono;
    private static String css;
    private static boolean instalado = false;

    private Ventanas() {
    }

    /** Se llama una sola vez al arrancar la aplicación. */
    public static void instalar() {
        if (instalado) return;
        instalado = true;

        try (InputStream in = Ventanas.class.getResourceAsStream(RUTA_ICONO)) {
            if (in != null) icono = new Image(in);
        } catch (Exception e) {
            System.out.println("⚠️ No se pudo cargar el ícono de la aplicación: " + e.getMessage());
        }

        URL cssUrl = Ventanas.class.getResource(RUTA_CSS);
        if (cssUrl != null) css = cssUrl.toExternalForm();

        // Ventanas que ya estén abiertas + todas las que se abran de acá en adelante
        Window.getWindows().forEach(Ventanas::aplicar);
        Window.getWindows().addListener((ListChangeListener<Window>) cambio -> {
            while (cambio.next()) {
                if (cambio.wasAdded()) {
                    cambio.getAddedSubList().forEach(Ventanas::aplicar);
                }
            }
        });
    }

    private static void aplicar(Window ventana) {
        // Solo ventanas "de verdad": los desplegables de combos y los tooltips no son Stage
        if (!(ventana instanceof Stage stage)) return;

        if (icono != null && stage.getIcons().isEmpty()) {
            stage.getIcons().add(icono);
        }

        aplicarEstilo(stage.getScene());
        stage.sceneProperty().addListener((obs, anterior, nueva) -> aplicarEstilo(nueva));
    }

    /** Agrega la hoja de estilos de Nakel a una escena y marca los diálogos para poder estilarlos. */
    public static void aplicarEstilo(Scene escena) {
        if (escena == null) return;

        if (css != null && !escena.getStylesheets().contains(css)) {
            escena.getStylesheets().add(css);
        }

        if (escena.getRoot() instanceof DialogPane panel && !panel.getStyleClass().contains("dialogo-nakel")) {
            panel.getStyleClass().add("dialogo-nakel");
        }

        // Íconos vectoriales en los botones de la ventana (Guardar, Buscar, Confirmar...)
        Iconos.decorar(escena.getRoot());
        if (!escena.getProperties().containsKey("nakel-iconos")) {
            escena.getProperties().put("nakel-iconos", Boolean.TRUE);
            escena.rootProperty().addListener((obs, anterior, nueva) -> Iconos.decorar(nueva));
        }
    }
}
