package com.nakel.frontend.util;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

/**
 * Evita que las tablas "se aplasten" cuando la ventana se achica.
 *
 * Las tablas del sistema reparten el ancho entre sus columnas (modo "ajustado").
 * Ese modo no tiene barra horizontal: al achicar la ventana comprime todo y los botones
 * de "Acciones" quedan cortados. Acá:
 *   1. Cada columna recibe un ancho mínimo razonable.
 *   2. Si la suma de mínimos no entra en la tabla, se pasa sola al modo "libre",
 *      que sí muestra la barra de desplazamiento horizontal.
 *   3. Cuando la ventana vuelve a agrandarse, la tabla vuelve al modo ajustado.
 *
 * Lo aplica solo util/Iconos.decorar() a cada tabla de cada pantalla.
 */
public final class Tablas {

    private static final String MARCA = "nakel-tabla-ajustada";

    private Tablas() {
    }

    public static <S> void ajustar(TableView<S> tabla) {
        if (tabla == null || tabla.getProperties().containsKey(MARCA)) return;
        tabla.getProperties().put(MARCA, Boolean.TRUE);

        int cantidad = tabla.getColumns().size();
        double[] anchoPensado = new double[cantidad];

        for (int i = 0; i < cantidad; i++) {
            TableColumn<S, ?> columna = tabla.getColumns().get(i);
            double preferido = columna.getPrefWidth();
            anchoPensado[i] = preferido;
            String titulo = columna.getText() == null ? "" : columna.getText().toLowerCase();

            double minimo;
            if (titulo.startsWith("acci")) {
                // Columna de botones: tiene que entrar la fila completa de íconos
                minimo = preferido >= 100 ? 150 : 90;
            } else {
                // El resto puede achicarse: las columnas anchas (descripciones) hasta la mitad,
                // las demás hasta un 65% de su ancho pensado. Nunca menos de 70.
                minimo = Math.max(70, preferido * (preferido >= 200 ? 0.5 : 0.65));
            }

            // Si el diseño ya pedía un mínimo mayor (columnas de plata), se respeta
            columna.setMinWidth(Math.max(columna.getMinWidth(), minimo));
        }

        // Las tablas que ya estaban en modo libre tienen su barra horizontal: no hay nada más que hacer
        Callback<TableView.ResizeFeatures, Boolean> modoOriginal = tabla.getColumnResizePolicy();
        if (modoOriginal == TableView.UNCONSTRAINED_RESIZE_POLICY) return;

        boolean[] enModoLibre = {false};

        Runnable evaluar = () -> {
            double ancho = tabla.getWidth();
            if (ancho <= 0 || tabla.getColumns().size() != cantidad) return;

            double sumaMinimos = 20; // margen para la barra vertical y los bordes
            for (TableColumn<S, ?> columna : tabla.getColumns()) sumaMinimos += columna.getMinWidth();

            boolean noEntra = ancho < sumaMinimos;

            if (noEntra && !enModoLibre[0]) {
                enModoLibre[0] = true;
                tabla.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
                for (TableColumn<S, ?> columna : tabla.getColumns()) columna.setPrefWidth(columna.getMinWidth());
            } else if (!noEntra && enModoLibre[0]) {
                enModoLibre[0] = false;
                for (int i = 0; i < cantidad; i++) tabla.getColumns().get(i).setPrefWidth(anchoPensado[i]);
                tabla.setColumnResizePolicy(modoOriginal);
            }
        };

        tabla.widthProperty().addListener((obs, antes, ahora) -> evaluar.run());
        evaluar.run();
    }
}
