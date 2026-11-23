package com.nakel.frontend.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Campos de plata con formato argentino mientras se escribe.
 *
 *   se tipea 34000      -> se ve 34.000
 *   se tipea 34000.04   -> se ve 34.000,04   (el punto o la coma del teclado marcan los decimales)
 *
 * Reglas: solo números, los puntos de miles se ponen solos, una sola coma y hasta 2 decimales.
 *
 *   FormatoMoneda.aplicar(txtPrecio);            // convierte el campo
 *   double precio = FormatoMoneda.leer(txtPrecio); // lee el número real (34000.04)
 *   FormatoMoneda.poner(txtPrecio, 34000.04);     // carga un valor ya formateado
 */
public final class FormatoMoneda {

    private static final int MAX_DECIMALES = 2;
    private static final int MAX_ENTEROS = 12; // hasta 999.999.999.999

    private FormatoMoneda() {
    }

    // ------------------------------------------------------------------
    // Texto "canónico": solo dígitos y, como mucho, una coma. Ej: "34000,04"
    // ------------------------------------------------------------------

    /** Limpia un texto canónico: una sola coma, 2 decimales, sin ceros de más a la izquierda. */
    private static String sanear(String canonico) {
        StringBuilder enteros = new StringBuilder();
        StringBuilder decimales = new StringBuilder();
        boolean hayComa = false;

        for (char c : canonico.toCharArray()) {
            if (c == ',') {
                if (!hayComa) hayComa = true;
            } else if (Character.isDigit(c)) {
                if (!hayComa) {
                    if (enteros.length() < MAX_ENTEROS) enteros.append(c);
                } else if (decimales.length() < MAX_DECIMALES) {
                    decimales.append(c);
                }
            }
        }

        // "007" -> "7", pero "0" se queda
        while (enteros.length() > 1 && enteros.charAt(0) == '0') enteros.deleteCharAt(0);
        if (hayComa && enteros.length() == 0) enteros.append('0'); // ",5" -> "0,5"

        return enteros + (hayComa ? "," + decimales : "");
    }

    /** "34000,04" -> "34.000,04" */
    private static String conMiles(String canonico) {
        int coma = canonico.indexOf(',');
        String enteros = coma >= 0 ? canonico.substring(0, coma) : canonico;
        String resto = coma >= 0 ? canonico.substring(coma) : "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < enteros.length(); i++) {
            if (i > 0 && (enteros.length() - i) % 3 == 0) sb.append('.');
            sb.append(enteros.charAt(i));
        }
        return sb + resto;
    }

    /** Lo que se tipea o se pega, llevado a canónico. */
    private static String interpretarEntrada(String entrada) {
        if (entrada == null || entrada.isEmpty()) return "";
        String e = entrada.trim().replace("$", "").replace(" ", "");

        if (e.length() > 1) {
            // Texto pegado: puede venir con formato argentino ("34.000,04" o "34.000")
            if (e.contains(",")) {
                e = e.replace(".", "");                       // los puntos eran de miles
            } else if (e.matches("\\d{1,3}(\\.\\d{3})+")) {
                e = e.replace(".", "");                       // "34.000" = treinta y cuatro mil
            } else {
                e = e.replace('.', ',');                      // "34000.04": el punto es decimal
            }
        } else {
            e = e.replace('.', ',');                          // una tecla: punto = coma decimal
        }
        return e.replaceAll("[^0-9,]", "");
    }

    // ------------------------------------------------------------------
    // API pública
    // ------------------------------------------------------------------

    /** Convierte el campo en un campo de plata con formato automático. */
    public static void aplicar(TextField campo) {
        if (campo == null) return;

        campo.setTextFormatter(new TextFormatter<String>(cambio -> {
            if (!cambio.isContentChange()) return cambio;

            String viejo = cambio.getControlText();
            int desde = cambio.getRangeStart();
            int hasta = cambio.getRangeEnd();

            // Pasamos lo que ya había a canónico (sin puntos de miles), recordando dónde cae el cambio
            String antes = viejo.substring(0, desde).replace(".", "");
            String despues = viejo.substring(hasta).replace(".", "");
            String insertado = interpretarEntrada(cambio.getText());

            // Una segunda coma no se acepta: se ignora la tecla
            if (insertado.contains(",") && (antes.contains(",") || despues.contains(","))) {
                insertado = insertado.replace(",", "");
            }

            String nuevoCanonico = sanear(antes + insertado + despues);
            String nuevo = conMiles(nuevoCanonico);

            // Cursor: justo después de lo que se acaba de escribir
            int caracteresAntesDelCursor = sanear(antes + insertado).length();
            if (antes.isEmpty() && insertado.startsWith(",")) caracteresAntesDelCursor = 2; // ",": queda "0,"
            int cursor = 0, contados = 0;
            while (cursor < nuevo.length() && contados < caracteresAntesDelCursor) {
                if (nuevo.charAt(cursor) != '.') contados++;
                cursor++;
            }

            cambio.setRange(0, viejo.length());
            cambio.setText(nuevo);
            cambio.setCaretPosition(cursor);
            cambio.setAnchor(cursor);
            return cambio;
        }));

        // Si el campo ya traía algo escrito, lo dejamos con formato
        String actual = campo.getText();
        if (actual != null && !actual.isBlank()) {
            campo.setText(actual);
        }
    }

    /** Número real que hay en el campo. Lanza NumberFormatException si está vacío o no es un número. */
    public static double leer(TextField campo) {
        return leer(campo == null ? null : campo.getText());
    }

    /** "34.000,04" -> 34000.04. También entiende "34000", "34000,04" y "34000.04". */
    public static double leer(String texto) {
        if (texto == null || texto.isBlank()) throw new NumberFormatException("vacío");
        String limpio = texto.trim().replace("$", "").replace(" ", "");
        if (limpio.contains(",")) {
            limpio = limpio.replace(".", "").replace(",", ".");
        } else if (limpio.matches("\\d{1,3}(\\.\\d{3})+")) {
            limpio = limpio.replace(".", "");
        }
        return Double.parseDouble(limpio);
    }

    /** Igual que leer(), pero vacío cuenta como 0. */
    public static double leerOCero(TextField campo) {
        if (campo == null || campo.getText() == null || campo.getText().isBlank()) return 0.0;
        return leer(campo.getText());
    }

    /** 34000.04 -> "34.000,04" ; 34000 -> "34.000" (sin decimales si no hacen falta). */
    public static String formatear(Number valor) {
        if (valor == null) return "";
        BigDecimal numero = new BigDecimal(valor.toString()).setScale(MAX_DECIMALES, RoundingMode.HALF_UP).stripTrailingZeros();
        String plano = numero.abs().toPlainString().replace('.', ',');
        return (numero.signum() < 0 ? "-" : "") + conMiles(plano);
    }

    /**
     * Para MOSTRAR plata en tablas, etiquetas y avisos: siempre con signo y dos decimales.
     * 10000 -> "$ 10.000,00" ; 34000.04 -> "$ 34.000,04" ; null -> "$ 0,00"
     */
    public static String pesos(Number valor) {
        BigDecimal numero = (valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString()))
                .setScale(MAX_DECIMALES, RoundingMode.HALF_UP);
        String plano = numero.abs().toPlainString().replace('.', ',');
        return (numero.signum() < 0 ? "-$ " : "$ ") + conMiles(plano);
    }

    /**
     * Para columnas de tabla que guardan un número (Double, BigDecimal...): lo muestra como plata.
     *   colPrecio.setCellFactory(FormatoMoneda.celdaPesos());
     */
    public static <S, T extends Number> javafx.util.Callback<javafx.scene.control.TableColumn<S, T>, javafx.scene.control.TableCell<S, T>> celdaPesos() {
        return columna -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(T valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio || valor == null ? null : pesos(valor));
            }
        };
    }

    /** Carga un número en el campo, ya con formato. */
    public static void poner(TextField campo, Number valor) {
        if (campo == null) return;
        campo.setText(valor == null ? "" : formatear(valor).replace("-", ""));
    }
}
