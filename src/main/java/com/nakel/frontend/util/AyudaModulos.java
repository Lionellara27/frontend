package com.nakel.frontend.util;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * La (i) que aparece al lado del título de cada módulo: al tocarla explica, en palabras simples,
 * para qué sirve esa pantalla. La idea es que cualquiera pueda usar el sistema sin que le expliquen.
 *
 * El Navegador llama a {@link #agregar(String, Parent)} cada vez que abre una pantalla.
 * Para sumar o cambiar una explicación alcanza con tocar el mapa de abajo.
 */
public final class AyudaModulos {

    private static final Map<String, String> AYUDAS = new LinkedHashMap<>();

    static {
        AYUDAS.put("articulo-view.fxml",
                "Es la lista de todo lo que vendés.\n\n"
                        + "• Cada artículo tiene su código, precio y cuántas unidades quedan (stock).\n"
                        + "• Con «Nuevo Artículo» cargás un producto que comprás hecho para revender.\n"
                        + "• Con el lápiz cambiás el precio o el stock; con el tachito lo das de baja.\n"
                        + "• «Imprimir Stock» arma una hoja con lo que tenés.\n\n"
                        + "Lo que fabricás vos no se carga acá: se crea desde «Calcular Producción».");
        AYUDAS.put("calcular-produccion-view.fxml",
                "Sirve para saber cuánto te cuesta fabricar un producto y a cuánto venderlo.\n\n"
                        + "1. Escribís el nombre del producto.\n"
                        + "2. Elegís los insumos que lleva (horas de trabajo, cierres, tela...) y cuánto usa de cada uno.\n"
                        + "3. Ponés el margen de ganancia y el sistema te sugiere el precio.\n\n"
                        + "Al tocar «Crear producto y fabricar», el producto aparece en el Catálogo y se descuentan los insumos usados. "
                        + "Hasta ese momento no se descuenta nada.");
        AYUDAS.put("cliente-view.fxml",
                "Es la agenda de tus clientes.\n\n"
                        + "• Guardás nombre, DNI o CUIT, teléfono y mail.\n"
                        + "• «Saldo a favor» es la plata que el cliente tiene en vales sin usar.\n"
                        + "• Con el ojito ves el detalle y los vales de cada uno.\n\n"
                        + "Si al vender elegís el cliente, después podés ver quién te compra más en Estadísticas.");
        AYUDAS.put("configuracion.fxml",
                "Acá se ajusta cómo trabaja el sistema. Tiene cinco pestañas:\n\n"
                        + "• Precios y Dólar: subir precios por porcentaje o según el dólar.\n"
                        + "• Categorías y Materiales: los rubros y materiales del catálogo.\n"
                        + "• Vales: regalarle un vale a un cliente y ver todos los vales.\n"
                        + "• Usuario y Contraseña: cambiar los datos para entrar al sistema.\n"
                        + "• Copias de seguridad: el sistema guarda solo una copia por día. Si se pierde algo, desde ahí se vuelve atrás.");
        AYUDAS.put("estadisticas.fxml",
                "Te muestra cómo viene el negocio.\n\n"
                        + "• Arriba elegís el período: hoy, 7 días, el mes o un año.\n"
                        + "• Ventas: cuánto vendiste, el promedio por venta y cómo te pagaron.\n"
                        + "• Productos: qué rubros y qué artículos se venden más.\n"
                        + "• Clientes: quién te compra más.\n"
                        + "• Inventario: qué hay que reponer y qué mercadería no se mueve.\n\n"
                        + "Es solo para mirar: acá no se modifica nada.");
        AYUDAS.put("historial-cajas-view.fxml",
                "Es el registro de la caja de cada día.\n\n"
                        + "• Cada renglón es un día: cuánta plata entró y por qué medio (efectivo, transferencia, tarjeta...).\n"
                        + "• Con el ojito ves todos los movimientos de ese día: ventas, cambios y vales.\n"
                        + "• Podés filtrar por mes y año.\n\n"
                        + "La caja se abre sola con la primera venta del día.");
        AYUDAS.put("historial-ventas-view.fxml",
                "Es la lista de todas las ventas y presupuestos que hiciste.\n\n"
                        + "• Podés buscar por número de comprobante o por cliente, y filtrar por mes.\n"
                        + "• Con el ojito ves qué se llevó; con la impresora volvés a imprimir el ticket.\n"
                        + "• Con las flechas hacés un cambio o devolución (hasta 2 por venta).\n\n"
                        + "Los presupuestos figuran pero no suman al total: no son ventas.");
        AYUDAS.put("insumo-view.fxml",
                "Son los materiales y el trabajo que usás para fabricar.\n\n"
                        + "• Telas y cueros se miden por superficie (ancho x largo).\n"
                        + "• Cierres, hebillas y herrajes se miden por unidad.\n"
                        + "• La mano de obra se mide por hora.\n\n"
                        + "Estos insumos son los que después elegís en «Calcular Producción». "
                        + "No son artículos para vender: esos están en el Catálogo.");
        AYUDAS.put("proveedor-view.fxml",
                "Es la agenda de a quién le comprás.\n\n"
                        + "• Guardás la empresa, la persona de contacto, el teléfono y qué te vende.\n"
                        + "• En «Comentarios» podés anotar lo que quieras recordar (señas, plazos, etc.).\n\n"
                        + "Es solo una agenda: no registra compras ni pagos.");
        AYUDAS.put("venta-view.fxml",
                "Es donde se vende.\n\n"
                        + "1. Buscás el artículo (o pasás el lector) y lo agregás al ticket.\n"
                        + "2. Elegís el cliente, o lo dejás en Consumidor Final.\n"
                        + "3. Tocás «Cobrar» (F12) y elegís cómo paga.\n\n"
                        + "Si elegís «Presupuesto», se guarda sin cobrar y sin descontar stock.");
    }

    private AyudaModulos() {
    }

    /**
     * Si la pantalla tiene un título de módulo y hay una explicación para ella, le pone la (i) al lado.
     *
     * @param rutaFxml ruta con la que se abrió la pantalla (ej: "/com/nakel/frontend/view/cliente-view.fxml")
     */
    public static void agregar(String rutaFxml, Parent pantalla) {
        if (rutaFxml == null || pantalla == null) return;
        String archivo = rutaFxml.substring(rutaFxml.lastIndexOf('/') + 1);
        String explicacion = AYUDAS.get(archivo);
        if (explicacion == null) return;

        Node nodo = pantalla.lookup(".titulo-modulo");
        if (!(nodo instanceof Label titulo) || titulo.getGraphic() != null) return;

        FontIcon icono = new FontIcon("fas-info-circle");
        icono.setIconSize(18);
        icono.setIconColor(Color.web("#1976D2"));

        Button boton = new Button();
        boton.setGraphic(icono);
        boton.getStyleClass().add("boton-info");
        boton.setFocusTraversable(false);
        Acciones.ayuda(boton, "¿Para qué sirve esta pantalla?");
        boton.setOnAction(e -> {
            Alert cartel = new Alert(Alert.AlertType.INFORMATION);
            cartel.setTitle("Ayuda");
            cartel.setHeaderText(titulo.getText());
            cartel.setContentText(explicacion);
            cartel.getDialogPane().setMinWidth(520);
            cartel.showAndWait();
        });

        // La (i) va pegada a la derecha del título
        titulo.setGraphic(boton);
        titulo.setContentDisplay(ContentDisplay.RIGHT);
        titulo.setGraphicTextGap(10);
    }
}
