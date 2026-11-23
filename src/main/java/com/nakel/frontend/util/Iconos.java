package com.nakel.frontend.util;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Íconos VECTORIALES (FontAwesome) para los botones, en reemplazo de los emojis.
 *
 * Los emojis dependen de la fuente de cada Windows: en algunas PC salen como un cuadradito.
 * Estos íconos viajan dentro del programa y se ven iguales en cualquier máquina.
 *
 * decorar(raiz) recorre una pantalla y le pone a cada botón el ícono que le corresponde
 * según su texto ("Buscar" -> lupa, "Guardar" -> disquete, etc.). Lo llaman solas
 * Ventanas (para cada ventana que se abre) y Navegador (para cada pantalla del menú),
 * así que un botón nuevo que diga "Buscar" ya sale con su lupa sin hacer nada.
 *
 * Para un botón puntual: Iconos.poner(boton, "fas-print");
 */
public final class Iconos {

    private static final String MARCA = "nakel-icono-automatico";

    private Iconos() {
    }

    /** Qué ícono le toca a un botón según lo que dice. null = sin ícono. */
    static String iconoPara(String textoBoton) {
        if (textoBoton == null) return null;
        String t = textoBoton.toLowerCase().trim();
        if (t.isEmpty()) return null;

        if (t.startsWith("buscar")) return "fas-search";
        if (t.startsWith("filtrar")) return "fas-filter";
        if (t.startsWith("limpiar")) return "fas-eraser";
        if (t.startsWith("nueva venta")) return "fas-shopping-cart";
        if (t.startsWith("nuevo") || t.startsWith("nueva") || t.startsWith("cliente nuevo") || t.startsWith("agregar")) return "fas-plus";
        if (t.startsWith("guardar")) return "fas-save";
        if (t.startsWith("actualizar") || t.startsWith("sincronizar")) return "fas-sync-alt";
        if (t.startsWith("cobrar")) return "fas-cash-register";
        if (t.startsWith("presupuestar")) return "fas-file-invoice";
        if (t.startsWith("confirmar") || t.startsWith("ok ") || t.startsWith("emitir")) return "fas-check";
        if (t.startsWith("quitar") || t.startsWith("eliminar")) return "fas-trash-alt";
        if (t.startsWith("aplicar aumento")) return "fas-percentage";
        if (t.startsWith("generar vale")) return "fas-ticket-alt";
        if (t.contains("impresora") || t.startsWith("imprimir")) return "fas-print";
        return null;
    }

    /** Le pone a un botón un ícono del mismo color y tamaño que su texto. */
    public static void poner(ButtonBase boton, String literal) {
        try {
            FontIcon icono = new FontIcon(literal);
            // El ícono copia siempre el color y el tamaño de la letra del botón
            icono.setIconColor(boton.getTextFill() instanceof Color c ? c : Color.web("#333333"));
            boton.textFillProperty().addListener((obs, antes, ahora) -> {
                if (ahora instanceof Color c) icono.setIconColor(c);
            });
            icono.setIconSize((int) Math.round(boton.getFont().getSize()));
            boton.fontProperty().addListener((obs, antes, ahora) -> icono.setIconSize((int) Math.round(ahora.getSize())));

            boton.setGraphic(icono);
            boton.setGraphicTextGap(8);
        } catch (Exception e) {
            System.out.println("No se pudo poner el icono " + literal + ": " + e.getMessage());
        }
    }

    /** Recorre toda una pantalla y decora sus botones. Se puede llamar más de una vez sin problema. */
    public static void decorar(Node nodo) {
        if (nodo == null) return;

        if (nodo instanceof ButtonBase boton && !(nodo instanceof ToggleButton)) {
            decorarBoton(boton);
        }

        // Un botón nunca se achica por debajo de su tamaño: si falta lugar ceden los campos
        // de texto y los combos, pero el botón no queda en "Bus..." ni en "...".
        if (nodo instanceof javafx.scene.control.Button botonComun && botonComun.getMinWidth() == javafx.scene.layout.Region.USE_COMPUTED_SIZE) {
            botonComun.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        }

        // De paso, a cada tabla le ponemos anchos mínimos para que no se aplaste al achicar la ventana
        if (nodo instanceof javafx.scene.control.TableView<?> tabla) {
            Tablas.ajustar(tabla);
        }

        // Contenedores cuyo contenido no figura como "hijos" comunes
        if (nodo instanceof TabPane pestanias) {
            for (Tab tab : pestanias.getTabs()) decorar(tab.getContent());
        }
        if (nodo instanceof ScrollPane scroll) decorar(scroll.getContent());
        if (nodo instanceof TitledPane titulado) decorar(titulado.getContent());
        if (nodo instanceof SplitPane dividido) dividido.getItems().forEach(Iconos::decorar);

        if (nodo instanceof Parent padre) {
            for (Node hijo : padre.getChildrenUnmodifiable()) decorar(hijo);
        }
    }

    private static void decorarBoton(ButtonBase boton) {
        boolean yaEsNuestro = boton.getProperties().containsKey(MARCA);

        // Si el botón ya trae su propio dibujo (los de las tablas, por ejemplo), no se toca
        if (boton.getGraphic() != null && !yaEsNuestro) return;
        if (yaEsNuestro) return;

        boton.getProperties().put(MARCA, Boolean.TRUE);
        actualizar(boton);

        // Si el texto cambia (COBRAR <-> PRESUPUESTAR) el ícono cambia con él
        boton.textProperty().addListener((obs, antes, ahora) -> actualizar(boton));
    }

    private static void actualizar(ButtonBase boton) {
        String literal = iconoPara(boton.getText());
        if (literal == null) {
            boton.setGraphic(null);
        } else {
            poner(boton, literal);
        }
    }
}
