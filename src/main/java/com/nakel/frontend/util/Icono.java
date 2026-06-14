package com.nakel.frontend.util;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

public enum Icono {


    MOSTRADOR("M3 5H5L7 15H17L19 8H8 M9 19A1 1 0 1 0 9.01 19 M15 19A1 1 0 1 0 15.01 19"),
    CATALOGO("M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20"),
    CLIENTES("M12 12A4 4 0 1 0 12 4 M5 20C5 16 8 14 12 14 M19 20C19 16 16 14 12 14"),
    PROVEEDORES("M5 18H3V7c0-.6.4-1 1-1h10c.6 0 1 .4 1 1v11 M14 9h4l4 4v5h-3 M7 18c0-1.1.9-2 2-2s2 .9 2 2-.9 2-2 2-2-.9-2-2z M15 18c0-1.1.9-2 2-2s2 .9 2 2-.9 2-2 2-2-.9-2-2z"),
    HISTORIAL("M12 4A8 8 0 1 0 12 20 M12 8V12 M12 12L16 14"),
    CAJAS("M2 6h20v12H2Z M12 15c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3z"),
    INSUMOS("M4 7L12 3L20 7V17L12 21L4 17V7 M12 3V21 M4 7L12 11L20 7"),
    CALCULADORA("M7 3H17V21H7Z M10 7H14 M10 11H11 M13 11H14 M10 15H11 M13 15H14 M10 19H11 M13 19H14"),
    ESTADISTICAS("M4 20H20 M7 17V10 M12 17V5 M17 17V8"),
    CONFIGURACION("M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1Z"),
    CERRAR_CAJA("M4 5H14V19H4Z M12 12H20 M17 9L20 12L17 15");


    private final String path;

    // Constructor
    Icono(String path) {
        this.path = path;
    }

    // El motor de renderizado del icono
// 1. El método NUEVO que acepta colores a pedido
    public SVGPath construir(String hexColor) {
        SVGPath svg = new SVGPath();
        svg.setContent(this.path);
        svg.setFill(Color.TRANSPARENT);
        svg.setStroke(Color.web(hexColor)); // ¡Acá recibe el color que le mandemos!
        svg.setStrokeWidth(1.5);
        return svg;
    }

    // 2. El método ORIGINAL (para que los demás botones sigan siendo dorados por defecto)
    public SVGPath construir() {
        return construir("#D4AF37"); // Si no le pasamos color, usa el dorado premium
    }
}