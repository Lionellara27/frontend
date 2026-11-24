package com.nakel.frontend.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nakel.frontend.service.EstadisticasApiService;
import com.nakel.frontend.util.FormatoMoneda;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Módulo de Estadísticas.
 *
 * Toda la cuenta la hace el backend (/api/estadisticas/resumen?periodo=...). Acá solo se dibuja:
 * tarjetas con números, gráficos de barras y de torta, y tablas de ranking.
 * El selector de período mueve Ventas, Productos y Clientes; Inventario es la foto de hoy.
 */
public class EstadisticasController {

    @FXML private ComboBox<String> cmbPeriodo;
    @FXML private ComboBox<Integer> cmbAnio;
    private boolean cargandoAnios = false; // para no volver a pedir datos mientras se llena la lista de años
    @FXML private Label lblRango;

    // --- Ventas ---
    @FXML private Label lblTituloVentas;
    @FXML private Label lblVentasPeriodo;
    @FXML private Label lblCantidadPeriodo;
    @FXML private Label lblVariacion;
    @FXML private Label lblTicketPromedio;
    @FXML private Label lblGanancia;
    @FXML private Label lblGananciaDetalle;
    @FXML private Label lblMayorCompra;
    @FXML private Label lblMayorCompraDetalle;
    @FXML private Label lblVentasHoy;
    @FXML private Label lblCantidadHoy;

    @FXML private Label lblTituloEvolucion;
    @FXML private BarChart<String, Number> chartEvolucion;
    @FXML private CategoryAxis ejeEvolucionX;
    @FXML private NumberAxis ejeEvolucionY;
    @FXML private Label lblSinEvolucion;
    @FXML private PieChart chartMediosPago;
    @FXML private Label lblSinMedios;

    // --- Productos ---
    @FXML private BarChart<Number, String> chartCategorias;
    @FXML private NumberAxis ejeCategoriasX;
    @FXML private CategoryAxis ejeCategoriasY;
    @FXML private Label lblSinCategorias;
    @FXML private Label lblOrigenes;
    @FXML private TableView<String[]> tablaTopArticulos;

    // --- Clientes ---
    @FXML private TableView<String[]> tablaTopClientes;
    @FXML private Label lblClientesIdentificados;
    @FXML private Label lblClientesIdentificadosDetalle;
    @FXML private Label lblConsumidorFinal;
    @FXML private Label lblConsumidorFinalDetalle;

    // --- Inventario ---
    @FXML private Label lblTotalProductos;
    @FXML private Label lblUnidadesStock;
    @FXML private Label lblValorStock;
    @FXML private Label lblStockCritico;
    @FXML private Label lblVales;
    @FXML private Label lblValesDetalle;
    @FXML private TableView<String[]> tablaStockCritico;
    @FXML private Label lblSinMovimiento;
    @FXML private TableView<String[]> tablaSinMovimiento;

    private final EstadisticasApiService estadisticasApi = new EstadisticasApiService();

    // Texto que ve la usuaria -> código que entiende el backend
    private final Map<String, String> periodos = new LinkedHashMap<>();

    private final DecimalFormat enteros = new DecimalFormat("#,##0", new DecimalFormatSymbols(new Locale("es", "AR")));
    private final DecimalFormat unDecimal = new DecimalFormat("#,##0.#", new DecimalFormatSymbols(new Locale("es", "AR")));

    private static final double ALTO_RENGLON = 32;
    private static final double ALTO_ENCABEZADO = 38;

    @FXML
    public void initialize() {
        periodos.put("Hoy", "HOY");
        periodos.put("Últimos 7 días", "SEMANA");
        periodos.put("Este mes", "MES");
        periodos.put("Mes pasado", "MES_PASADO");
        periodos.put("Por año", "ANIO");
        cmbPeriodo.getItems().setAll(periodos.keySet());
        cmbPeriodo.setValue("Este mes");
        cmbPeriodo.valueProperty().addListener((obs, viejo, nuevo) -> {
            // El selector de año solo se muestra cuando se mira "Por año"
            boolean porAnio = "ANIO".equals(periodos.get(nuevo));
            cmbAnio.setVisible(porAnio);
            cmbAnio.setManaged(porAnio);
            cargarDatosDashboard();
        });
        cmbAnio.valueProperty().addListener((obs, viejo, nuevo) -> {
            if (!cargandoAnios && nuevo != null) cargarDatosDashboard();
        });

        armarTabla(tablaTopArticulos, "Todavía no hay artículos vendidos en este período.",
                new String[]{"Artículo", "Unidades", "Total", "Ganancia"}, new double[]{200, 75, 130, 130}, 1, 2, 3);
        armarTabla(tablaTopClientes, "No hay ventas a clientes con nombre en este período.",
                new String[]{"Cliente", "Total gastado", "Compras", "Promedio por compra"}, new double[]{220, 130, 80, 150}, 1, 2, 3);
        armarTabla(tablaStockCritico, "No hay artículos por reponer.",
                new String[]{"Código", "Artículo", "Stock", "Mínimo"}, new double[]{150, 230, 60, 70}, 2, 3);
        armarTabla(tablaSinMovimiento, "Toda la mercadería tuvo ventas.",
                new String[]{"Código", "Artículo", "Stock", "Valor parado"}, new double[]{140, 200, 60, 150}, 2, 3);

        // Los ejes de plata se leen "$ 500 mil", no "500000.0"
        StringConverter<Number> plataCorta = new StringConverter<>() {
            @Override public String toString(Number n) { return plataCorta(n.doubleValue()); }
            @Override public Number fromString(String s) { return 0; }
        };
        ejeEvolucionY.setTickLabelFormatter(plataCorta);
        ejeCategoriasX.setTickLabelFormatter(plataCorta);

        cargarDatosDashboard();
    }

    @FXML
    public void actualizar(ActionEvent event) {
        cargarDatosDashboard();
    }

    /** Pide el resumen del período elegido y vuelve a dibujar todo. */
    public void cargarDatosDashboard() {
        String codigo = periodos.getOrDefault(cmbPeriodo.getValue(), "MES");
        String json = estadisticasApi.obtenerResumen(codigo, "ANIO".equals(codigo) ? cmbAnio.getValue() : null);
        if (json == null || json.isBlank()) {
            lblRango.setText("No se pudieron cargar las estadísticas. Revisá que el sistema esté iniciado y tocá Actualizar.");
            return;
        }
        try {
            JsonObject r = JsonParser.parseString(json).getAsJsonObject();
            cargarAnios(r);
            pintarVentas(r);
            pintarProductos(r);
            pintarClientes(r);
            pintarInventario(r);
        } catch (Exception e) {
            lblRango.setText("Hubo un problema al mostrar las estadísticas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Llena la lista de años (del más nuevo al más viejo) y deja marcado el que se está mostrando. */
    private void cargarAnios(JsonObject r) {
        cargandoAnios = true;
        try {
            ObservableList<Integer> anios = FXCollections.observableArrayList();
            for (JsonElement e : r.getAsJsonArray("anios")) anios.add(e.getAsInt());
            if (!anios.equals(cmbAnio.getItems())) cmbAnio.setItems(anios);
            if ("ANIO".equals(texto(r, "periodo")) || cmbAnio.getValue() == null) {
                cmbAnio.setValue((int) numero(r, "anio"));
            }
        } finally {
            cargandoAnios = false;
        }
    }

    // =====================================================================
    //  VENTAS
    // =====================================================================

    private void pintarVentas(JsonObject r) {
        String periodo = texto(r, "periodoNombre");
        lblRango.setText("Mostrando: " + periodo + "  (del " + fechaCorta(texto(r, "desde")) + " al " + fechaCorta(texto(r, "hasta")) + ")");

        double total = numero(r, "totalVentas");
        long cantidad = (long) numero(r, "cantidadVentas");
        lblTituloVentas.setText("Ventas · " + periodo);
        lblVentasPeriodo.setText(pesosTarjeta(total));
        lblCantidadPeriodo.setText(cantidad + (cantidad == 1 ? " venta" : " ventas"));
        lblTicketPromedio.setText(pesosTarjeta(numero(r, "ticketPromedio")));

        // Ganancia: precio menos costo, solo de lo vendido que tiene el costo cargado
        double conCosto = numero(r, "vendidoConCosto");
        double sinCosto = numero(r, "vendidoSinCosto");
        if (conCosto <= 0) {
            lblGanancia.setText("Sin datos");
            lblGanancia.setStyle("-fx-text-fill: #9AA4AE;");
            long faltan = (long) numero(r, "articulosSinCosto");
            lblGananciaDetalle.setText(total <= 0 ? "No hubo ventas en el período"
                    : "Lo vendido no tiene el costo cargado" + (faltan > 0 ? " (" + faltan + " artículos sin costo)" : ""));
        } else {
            double ganancia = numero(r, "ganancia");
            lblGanancia.setText(pesosTarjeta(ganancia));
            lblGanancia.setStyle(ganancia < 0 ? "-fx-text-fill: #C0392B;" : "");
            lblGananciaDetalle.setText("Margen " + unDecimal.format(numero(r, "margenGanancia")) + " %"
                    + (sinCosto > 0 ? "  ·  no incluye " + FormatoMoneda.pesos(sinCosto) + " vendidos sin costo cargado" : ""));
        }

        // Comparación contra el período anterior
        double anterior = numero(r, "totalAnterior");
        if (anterior <= 0) {
            lblVariacion.setText(total > 0 ? "El período anterior no tuvo ventas" : "");
            lblVariacion.setStyle("");
        } else {
            double variacion = (total - anterior) / anterior * 100;
            lblVariacion.setText((variacion >= 0 ? "+" : "−") + unDecimal.format(Math.abs(variacion)) + " % contra el período anterior");
            lblVariacion.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (variacion >= 0 ? "#2E9E6B" : "#C0392B") + ";");
        }

        JsonElement mayor = r.get("mayorCompra");
        if (mayor != null && mayor.isJsonObject()) {
            JsonObject m = mayor.getAsJsonObject();
            lblMayorCompra.setText(pesosTarjeta(numero(m, "total")));
            lblMayorCompraDetalle.setText(texto(m, "cliente") + " · " + texto(m, "fecha") + " · Ticket #" + (long) numero(m, "idVenta"));
        } else {
            lblMayorCompra.setText(pesosTarjeta(0));
            lblMayorCompraDetalle.setText("Sin ventas en el período");
        }

        long hoy = (long) numero(r, "cantidadHoy");
        lblVentasHoy.setText(pesosTarjeta(numero(r, "ventasHoy")));
        lblCantidadHoy.setText(hoy + (hoy == 1 ? " venta" : " ventas"));

        // --- Gráfico de barras: evolución ---
        String codigo = texto(r, "periodo");
        lblTituloEvolucion.setText("HOY".equals(codigo) ? "Ventas de hoy por hora"
                : "ANIO".equals(codigo) ? "Ventas por mes" : "Ventas por día");

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        JsonArray evolucion = r.getAsJsonArray("evolucion");
        for (JsonElement e : evolucion) {
            JsonObject t = e.getAsJsonObject();
            serie.getData().add(new XYChart.Data<>(texto(t, "etiqueta"), numero(t, "total")));
        }
        chartEvolucion.getData().clear();
        chartEvolucion.getData().add(serie);
        // Con muchos días (un mes) se muestran menos etiquetas para que no se pisen
        ejeEvolucionX.setTickLabelRotation(evolucion.size() > 16 ? 90 : 0);
        lblSinEvolucion.setVisible(total <= 0);
        for (int i = 0; i < serie.getData().size(); i++) {
            JsonObject t = evolucion.get(i).getAsJsonObject();
            long ventas = (long) numero(t, "cantidad");
            ayuda(serie.getData().get(i).getNode(), texto(t, "etiqueta") + ": " + FormatoMoneda.pesos(numero(t, "total"))
                    + " en " + ventas + (ventas == 1 ? " venta" : " ventas"));
        }

        // --- Gráfico de torta: medios de pago ---
        JsonArray medios = r.getAsJsonArray("mediosPago");
        double totalMedios = 0;
        for (JsonElement e : medios) totalMedios += numero(e.getAsJsonObject(), "total");
        ObservableList<PieChart.Data> porciones = FXCollections.observableArrayList();
        for (JsonElement e : medios) {
            JsonObject m = e.getAsJsonObject();
            double monto = numero(m, "total");
            double porcentaje = totalMedios > 0 ? monto / totalMedios * 100 : 0;
            // El importe y el porcentaje van en la leyenda: se lee sin pasar el mouse
            porciones.add(new PieChart.Data(texto(m, "nombre") + "  " + FormatoMoneda.pesos(monto)
                    + "  (" + unDecimal.format(porcentaje) + " %)", monto));
        }
        chartMediosPago.setData(porciones);
        lblSinMedios.setVisible(porciones.isEmpty());
        for (PieChart.Data porcion : porciones) ayuda(porcion.getNode(), porcion.getName());
    }

    // =====================================================================
    //  PRODUCTOS
    // =====================================================================

    private void pintarProductos(JsonObject r) {
        // Barras horizontales: el nombre del rubro se lee entero. El eje de categorías dibuja de abajo
        // hacia arriba, así que se cargan al revés para que el que más vende quede arriba.
        JsonArray categorias = r.getAsJsonArray("categorias");
        int cuantas = Math.min(categorias.size(), 8);
        XYChart.Series<Number, String> serie = new XYChart.Series<>();
        ObservableList<String> nombres = FXCollections.observableArrayList();
        for (int i = cuantas - 1; i >= 0; i--) {
            JsonObject c = categorias.get(i).getAsJsonObject();
            nombres.add(texto(c, "nombre"));
            serie.getData().add(new XYChart.Data<>(numero(c, "total"), texto(c, "nombre")));
        }
        ejeCategoriasY.setCategories(nombres);
        chartCategorias.getData().clear();
        chartCategorias.getData().add(serie);
        lblSinCategorias.setVisible(cuantas == 0);
        // El alto acompaña a la cantidad de rubros: con uno solo no queda una barra gigante
        double altoGrafico = 70 + Math.max(cuantas, 2) * 42;
        chartCategorias.setMinHeight(altoGrafico);
        chartCategorias.setPrefHeight(altoGrafico);
        chartCategorias.setMaxHeight(altoGrafico);
        for (int i = 0; i < serie.getData().size(); i++) {
            JsonObject c = categorias.get(cuantas - 1 - i).getAsJsonObject();
            ayuda(serie.getData().get(i).getNode(), texto(c, "nombre") + ": " + FormatoMoneda.pesos(numero(c, "total"))
                    + " en " + enteros.format(numero(c, "unidades")) + " unidades");
        }

        // Producción propia vs. reventa
        StringBuilder origenes = new StringBuilder();
        for (JsonElement e : r.getAsJsonArray("origenes")) {
            JsonObject o = e.getAsJsonObject();
            if (origenes.length() > 0) origenes.append("\n");
            origenes.append(nombreOrigen(texto(o, "nombre"))).append(": ").append(FormatoMoneda.pesos(numero(o, "total")))
                    .append(" (").append(enteros.format(numero(o, "unidades"))).append(" u.)")
                    .append(numero(o, "conCosto") > 0 ? "  ·  ganancia " + FormatoMoneda.pesos(numero(o, "ganancia")) : "");
        }
        lblOrigenes.setText(origenes.toString());

        ObservableList<String[]> filas = FXCollections.observableArrayList();
        for (JsonElement e : r.getAsJsonArray("topArticulos")) {
            JsonObject a = e.getAsJsonObject();
            filas.add(new String[]{texto(a, "nombre"),
                    enteros.format(numero(a, "unidades")), FormatoMoneda.pesos(numero(a, "total")),
                    numero(a, "conCosto") > 0 ? FormatoMoneda.pesos(numero(a, "ganancia")) : "Sin costo"});
        }
        cargar(tablaTopArticulos, filas, 10);
    }

    private String nombreOrigen(String origen) {
        if (origen == null) return "Sin origen";
        return switch (origen.trim().toUpperCase().replace(' ', '_')) {
            case "PRODUCCION_PROPIA" -> "Producción propia";
            case "REVENTA" -> "Reventa";
            default -> origen;
        };
    }

    // =====================================================================
    //  CLIENTES
    // =====================================================================

    private void pintarClientes(JsonObject r) {
        ObservableList<String[]> filas = FXCollections.observableArrayList();
        for (JsonElement e : r.getAsJsonArray("topClientes")) {
            JsonObject c = e.getAsJsonObject();
            filas.add(new String[]{texto(c, "nombre"), FormatoMoneda.pesos(numero(c, "total")),
                    enteros.format(numero(c, "compras")), FormatoMoneda.pesos(numero(c, "ticketPromedio"))});
        }
        cargar(tablaTopClientes, filas, 10);

        long conNombre = (long) numero(r, "ventasClientesIdentificados");
        long sinNombre = (long) numero(r, "ventasConsumidorFinal");
        lblClientesIdentificados.setText(FormatoMoneda.pesos(numero(r, "totalClientesIdentificados")));
        lblClientesIdentificadosDetalle.setText(conNombre + (conNombre == 1 ? " venta" : " ventas"));
        lblConsumidorFinal.setText(FormatoMoneda.pesos(numero(r, "totalConsumidorFinal")));
        lblConsumidorFinalDetalle.setText(sinNombre + (sinNombre == 1 ? " venta" : " ventas"));
    }

    // =====================================================================
    //  INVENTARIO
    // =====================================================================

    private void pintarInventario(JsonObject r) {
        lblTotalProductos.setText(enteros.format(numero(r, "productosActivos")));
        lblUnidadesStock.setText(enteros.format(numero(r, "unidadesEnStock")) + " unidades en stock");
        lblValorStock.setText(pesosTarjeta(numero(r, "valorStock")));

        long criticos = (long) numero(r, "cantidadStockCritico");
        lblStockCritico.setText(String.valueOf(criticos));
        lblStockCritico.setStyle("-fx-text-fill: " + (criticos > 0 ? "#C0392B" : "#2E9E6B") + ";");

        long vales = (long) numero(r, "cantidadVales");
        lblVales.setText(pesosTarjeta(numero(r, "totalVales")));
        lblValesDetalle.setText(vales + (vales == 1 ? " vale vigente" : " vales vigentes"));

        ObservableList<String[]> reponer = FXCollections.observableArrayList();
        for (JsonElement e : r.getAsJsonArray("stockCritico")) {
            JsonObject a = e.getAsJsonObject();
            reponer.add(new String[]{texto(a, "codigo"), texto(a, "nombre"),
                    enteros.format(numero(a, "stock")), enteros.format(numero(a, "minimo"))});
        }
        cargar(tablaStockCritico, reponer, 8);

        long dias = (long) numero(r, "diasSinMovimiento");
        long quietos = (long) numero(r, "cantidadSinMovimiento");
        lblSinMovimiento.setText(quietos == 0 ? "Toda la mercadería con stock tuvo alguna venta en los últimos " + dias + " días."
                : quietos + " artículos con stock no se vendieron en los últimos " + dias + " días. Entre todos suman "
                + FormatoMoneda.pesos(numero(r, "valorSinMovimiento")) + " a precio de venta. Abajo, los 15 de mayor valor.");

        ObservableList<String[]> parados = FXCollections.observableArrayList();
        for (JsonElement e : r.getAsJsonArray("sinMovimiento")) {
            JsonObject a = e.getAsJsonObject();
            parados.add(new String[]{texto(a, "codigo"), texto(a, "nombre"),
                    enteros.format(numero(a, "stock")), FormatoMoneda.pesos(numero(a, "valor"))});
        }
        cargar(tablaSinMovimiento, parados, 8);
    }

    // =====================================================================
    //  AYUDAS
    // =====================================================================

    /** Arma las columnas de una tabla de ranking. Las columnas "aLaDerecha" son números. */
    private void armarTabla(TableView<String[]> tabla, String textoVacio, String[] titulos, double[] anchos, int... aLaDerecha) {
        tabla.getColumns().clear();
        for (int i = 0; i < titulos.length; i++) {
            final int indice = i;
            TableColumn<String[], String> columna = new TableColumn<>(titulos[i]);
            columna.setPrefWidth(anchos[i]);
            columna.setSortable(false);
            columna.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[indice]));
            boolean numero = false;
            for (int d : aLaDerecha) if (d == i) numero = true;
            if (numero) {
                columna.setCellFactory(col -> new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? null : item);
                        setStyle("-fx-alignment: CENTER-RIGHT;"); // los números, alineados a la derecha
                    }
                });
            }
            tabla.getColumns().add(columna);
        }
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setFixedCellSize(ALTO_RENGLON);
        tabla.setPlaceholder(new Label(textoVacio));
    }

    /** Carga las filas y ajusta el alto: sin huecos si hay pocas, con scroll si hay más que el máximo. */
    private void cargar(TableView<String[]> tabla, ObservableList<String[]> filas, int maximoALaVista) {
        tabla.setItems(filas);
        int renglones = Math.max(3, Math.min(filas.size(), maximoALaVista));
        double alto = ALTO_ENCABEZADO + renglones * ALTO_RENGLON + 4;
        tabla.setMinHeight(alto);
        tabla.setPrefHeight(alto);
        tabla.setMaxHeight(alto);
    }

    private void ayuda(javafx.scene.Node nodo, String mensaje) {
        if (nodo == null) return;
        Tooltip cartel = new Tooltip(mensaje);
        cartel.setStyle("-fx-font-size: 13px;");
        cartel.setShowDelay(javafx.util.Duration.millis(150));
        Tooltip.install(nodo, cartel);
    }

    /** Importe para las tarjetas de números grandes: redondeado a pesos, sin centavos, para que no se corte. */
    private String pesosTarjeta(double valor) {
        return FormatoMoneda.pesos(Math.round(valor)).replace(",00", "");
    }

    /** "$ 1,2 M", "$ 500 mil", "$ 800": para los ejes de los gráficos. */
    private String plataCorta(double valor) {
        if (valor >= 1_000_000) return "$ " + unDecimal.format(valor / 1_000_000) + " M";
        if (valor >= 1_000) return "$ " + unDecimal.format(valor / 1_000) + " mil";
        return "$ " + enteros.format(valor);
    }

    /** "2026-11-23" -> "23/11/2026" */
    private String fechaCorta(String iso) {
        String[] p = iso == null ? new String[0] : iso.split("-");
        return p.length == 3 ? p[2] + "/" + p[1] + "/" + p[0] : String.valueOf(iso);
    }

    private String texto(JsonObject o, String campo) {
        JsonElement e = o.get(campo);
        return e == null || e.isJsonNull() ? "" : e.getAsString();
    }

    private double numero(JsonObject o, String campo) {
        JsonElement e = o.get(campo);
        return e == null || e.isJsonNull() ? 0 : e.getAsDouble();
    }
}
