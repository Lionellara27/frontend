package com.nakel.frontend.util;

import javafx.animation.PauseTransition;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Aspecto y ayuda de los botoncitos de la columna "Acciones" de todas las tablas
 * (ver, editar, eliminar, imprimir, cambio...), para que sean iguales en todo el sistema.
 *
 *   Acciones.ver(btnVer, "Ver el detalle");
 *   Acciones.editar(btnEditar, "Editar el cliente");
 *   Acciones.eliminar(btnEliminar, "Eliminar el cliente");   // siempre tachito ROJO
 */
public final class Acciones {

    /** Rojo único para todo lo que borra o da de baja. */
    public static final String ROJO_ELIMINAR = "#e74c3c";

    private static final String ESTILO_BOTON = "-fx-background-color: transparent; -fx-cursor: hand;";

    private Acciones() {
    }

    public static void ver(Button boton, String ayuda) {
        boton.setStyle(ESTILO_BOTON);
        ayuda(boton, ayuda);
    }

    public static void editar(Button boton, String ayuda) {
        boton.setStyle(ESTILO_BOTON);
        ayuda(boton, ayuda);
    }

    /** Tachito rojo, igual en todas las pantallas. */
    public static void eliminar(Button boton, String ayuda) {
        FontIcon tacho = new FontIcon("fas-trash-alt");
        tacho.setIconColor(Color.web(ROJO_ELIMINAR));
        boton.setGraphic(tacho);
        boton.setStyle(ESTILO_BOTON);
        ayuda(boton, ayuda);
    }

    /**
     * Cartelito de ayuda que aparece al dejar el mouse sobre el botón, centrado justo debajo.
     * (El cartelito estándar de JavaFX sale corrido, al costado del puntero.)
     * Devuelve el Tooltip por si después hay que cambiarle el texto con setText().
     */
    public static Tooltip ayuda(Node nodo, String texto) {
        Tooltip cartel = new Tooltip(texto);
        cartel.setStyle("-fx-font-size: 13px;");

        PauseTransition espera = new PauseTransition(Duration.millis(300));
        espera.setOnFinished(e -> {
            if (!nodo.isHover() || nodo.getScene() == null || nodo.getScene().getWindow() == null) return;
            if (cartel.getText() == null || cartel.getText().isBlank()) return;

            Bounds zona = nodo.localToScreen(nodo.getBoundsInLocal());
            if (zona == null) return;

            // Lo mostramos debajo del botón y, ya sabiendo cuánto mide, lo centramos
            cartel.show(nodo, zona.getMinX(), zona.getMaxY() + 6);
            cartel.setAnchorX(zona.getMinX() + zona.getWidth() / 2 - cartel.getWidth() / 2);
        });

        // Aparece al posar el mouse y se va al salir, al hacer clic o si el botón desaparece de pantalla
        nodo.hoverProperty().addListener((obs, antes, encima) -> {
            if (encima) {
                espera.playFromStart();
            } else {
                espera.stop();
                cartel.hide();
            }
        });
        nodo.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            espera.stop();
            cartel.hide();
        });
        nodo.sceneProperty().addListener((obs, antes, escena) -> {
            if (escena == null) {
                espera.stop();
                cartel.hide();
            }
        });

        return cartel;
    }
}
