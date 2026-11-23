package com.nakel.frontend.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/**
 * Formato único de los códigos de vale: "VALE-" + 5 caracteres (letras o números). Ej: VALE-C44F4
 *
 * aplicar(campo) convierte cualquier TextField en un campo de vale:
 *  - El "VALE-" está siempre puesto y no se puede borrar: solo se escriben los 5 caracteres.
 *  - Pasa todo a mayúsculas e ignora espacios, guiones y símbolos.
 *  - No deja escribir más de 5 caracteres.
 *  - Si se pega el código entero ("vale-c44f4") o solo el final ("c44f4"), lo acomoda solo.
 *
 * Para usarlo en otra pantalla donde se escriba un vale: FormatoVale.aplicar(miTextField);
 */
public final class FormatoVale {

    public static final String PREFIJO = "VALE-";
    public static final int LARGO_CODIGO = 5;

    private FormatoVale() {
    }

    /** Lleva cualquier texto al formato VALE-XXXXX (con los caracteres que haya, hasta 5). */
    public static String normalizar(String texto) {
        String limpio = texto == null ? "" : texto.toUpperCase().replaceAll("[^A-Z0-9]", "");

        // Quitamos el "VALE" del principio (o el pedazo que haya quedado si se borró una letra)
        String letrasPrefijo = "VALE";
        int coincide = 0;
        while (coincide < letrasPrefijo.length() && coincide < limpio.length()
                && limpio.charAt(coincide) == letrasPrefijo.charAt(coincide)) {
            coincide++;
        }
        String codigo = limpio.substring(coincide);

        if (codigo.length() > LARGO_CODIGO) {
            codigo = codigo.substring(0, LARGO_CODIGO);
        }
        return PREFIJO + codigo;
    }

    /** true si el texto ya es un código completo: VALE- y exactamente 5 caracteres. */
    public static boolean esCompleto(String texto) {
        return texto != null && texto.matches("VALE-[A-Z0-9]{" + LARGO_CODIGO + "}");
    }

    /** Convierte el campo en un campo de código de vale con formato obligatorio. */
    public static void aplicar(TextField campo) {
        campo.setTextFormatter(new TextFormatter<String>(cambio -> {
            if (!cambio.isContentChange()) {
                return cambio; // mover el cursor o seleccionar no modifica nada
            }
            String nuevo = normalizar(cambio.getControlNewText());

            // Reemplazamos todo el contenido por la versión con formato y dejamos el cursor al final
            cambio.setRange(0, cambio.getControlText().length());
            cambio.setText(nuevo);
            cambio.setCaretPosition(nuevo.length());
            cambio.setAnchor(nuevo.length());
            return cambio;
        }));
        campo.setText(PREFIJO);
        campo.positionCaret(PREFIJO.length());
    }
}
