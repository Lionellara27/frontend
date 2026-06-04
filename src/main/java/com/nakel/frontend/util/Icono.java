package com.nakel.frontend.util;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

public enum Icono {

    MOSTRADOR("M4 7H20V18H4Z M4 11H20 M9 15H11 M13 15H15 M17 15H19"),
    CATALOGO("M4 5H20V19H4Z M8 9H16 M8 13H16 M8 17H13"),
    CLIENTES("M12 12A4 4 0 1 0 12 4 M5 20C5 16 8 14 12 14 M19 20C19 16 16 14 12 14"),
    PROVEEDORES("M4 12L8 16 M8 16L12 12 M12 12L16 16 M16 16L20 12"),
    HISTORIAL("M12 4A8 8 0 1 0 12 20 M12 8V12 M12 12L16 14"),
    CAJAS("M4 7H20V18H4Z M8 11H16 M12 7V18 M7 4H17"),
    INSUMOS("M5 7L12 3L19 7 M5 7V17L12 21L19 17V7 M12 3V21"),
    CALCULADORA("M7 3H17V21H7Z M10 7H14 M10 11H11 M13 11H14 M10 15H11 M13 15H14 M10 19H11 M13 19H14"),
    ESTADISTICAS("M4 20H20 M7 17V10 M12 17V5 M17 17V8"),
    CERRAR_CAJA("M4 5H14V19H4Z M12 12H20 M17 9L20 12L17 15");

    private final String path;

    // Constructor
    Icono(String path) {
        this.path = path;
    }

    // El motor de renderizado del icono
    public SVGPath construir() {
        SVGPath svg = new SVGPath();
        svg.setContent(this.path);
        svg.setFill(Color.TRANSPARENT);
        svg.setStroke(Color.web("#D4AF37")); // Dorado premium
        svg.setStrokeWidth(1.5);
        return svg;
    }
}