package com.nakel.frontend.util;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Atajos de teclado por pantalla (F3 buscar, F12 cobrar, ESC limpiar...).
 *
 * El atajo vive mientras la pantalla está a la vista: se engancha a la ventana cuando la
 * pantalla se muestra y se desengancha solo cuando se cambia a otra. Así F12 cobra en el
 * Mostrador pero no hace nada raro si estás en Clientes.
 *
 *   Atajos.registrar(unNodoDeLaPantalla, KeyCode.F12, () -> cobrarVenta(null));
 *   Atajos.buscarConF3(txtBuscar, () -> buscarArticulos(null));
 */
public final class Atajos {

    private Atajos() {
    }

    /** Ejecuta la acción cuando se aprieta la tecla, mientras "nodo" esté en pantalla. */
    public static void registrar(Node nodo, KeyCode tecla, Runnable accion) {
        EventHandler<KeyEvent> filtro = evento -> {
            if (evento.getCode() == tecla && !evento.isConsumed()) {
                evento.consume();
                accion.run();
            }
        };

        if (nodo.getScene() != null) {
            nodo.getScene().addEventFilter(KeyEvent.KEY_PRESSED, filtro);
        }
        nodo.sceneProperty().addListener((obs, anterior, nueva) -> {
            if (anterior != null) anterior.removeEventFilter(KeyEvent.KEY_PRESSED, filtro);
            if (nueva != null) nueva.addEventFilter(KeyEvent.KEY_PRESSED, filtro);
        });
    }

    /**
     * F3 en cualquier pantalla con buscador: lleva el cursor al campo de búsqueda,
     * selecciona lo escrito (para pisarlo tipeando) y ejecuta la búsqueda.
     */
    public static void buscarConF3(TextField campo, Runnable buscar) {
        if (campo == null) return;
        registrar(campo, KeyCode.F3, () -> {
            campo.requestFocus();
            campo.selectAll();
            if (buscar != null) buscar.run();
        });
    }
}
