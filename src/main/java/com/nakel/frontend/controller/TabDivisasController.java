package com.nakel.frontend.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nakel.frontend.model.AjustesDivisa;
import com.nakel.frontend.model.Categoria;
import com.nakel.frontend.service.ArticuloApiService;
import com.nakel.frontend.service.ConfiguracionApiService;
import com.nakel.frontend.service.ParametrosApiService;
import com.nakel.frontend.service.PreciosApiService;
import com.nakel.frontend.util.FormatoMoneda;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Pestaña "Precios y Dólar" de Configuración.
 *
 *  - Aumentar por porcentaje los precios de una categoría (o de todas).
 *  - Seguir al dólar: los artículos se guardan solo en pesos; el sistema recuerda con qué dólar
 *    se calcularon y, si el dólar cambia, ofrece poner todos los precios al día.
 *  - Historial de cambios (de a 7), con "Volver atrás" durante 7 días.
 */
public class TabDivisasController {

    private static final int CAMBIOS_POR_PAGINA = 7;

    // --- Aumento por porcentaje ---
    @FXML private ComboBox<Categoria> cmbCategoriaAumento;
    @FXML private TextField txtPorcentajeAumento;
    @FXML private Label lblEjemploAumento;
    @FXML private Button btnAplicarAumento;

    // --- Dólar ---
    @FXML private ComboBox<String> cmbTipoDolar;
    @FXML private TextField txtCotizacionDolar;
    @FXML private Button btnBuscarCotizacion;
    @FXML private Button btnGuardarCotizacion;
    @FXML private Label lblEstadoDolar;
    @FXML private Button btnSincronizarDolar;

    // --- Historial ---
    @FXML private TableView<JsonObject> tablaHistorial;
    @FXML private TableColumn<JsonObject, String> colHistFecha;
    @FXML private TableColumn<JsonObject, String> colHistQue;
    @FXML private TableColumn<JsonObject, String> colHistArticulos;
    @FXML private TableColumn<JsonObject, String> colHistEstado;
    @FXML private TableColumn<JsonObject, JsonObject> colHistAccion;
    @FXML private Button btnHistorialAnterior;
    @FXML private Button btnHistorialSiguiente;
    @FXML private Label lblPaginaHistorial;

    private final ConfiguracionApiService apiService = new ConfiguracionApiService();
    private final ParametrosApiService parametrosApiService = new ParametrosApiService();
    private final ArticuloApiService articuloApiService = new ArticuloApiService();
    private final PreciosApiService preciosApi = new PreciosApiService();

    // Texto que se ve -> código que entiende el backend
    private final Map<String, String> tiposDolar = new LinkedHashMap<>();

    private final DecimalFormat unDecimal = new DecimalFormat("#,##0.##", new DecimalFormatSymbols(new Locale("es", "AR")));

    // Lo último que se guardó en el sistema (para saber cuánto se atrasaron los precios)
    private double cotizacionGuardada = 0;
    private double cotizacionBasePrecios = 0;
    // De qué tipo y de qué día es el dólar con el que están calculados los precios
    private String tipoDolarBase = "blue";
    private String fechaBasePrecios = "";

    private int paginaHistorial = 0;
    private int totalPaginasHistorial = 1;

    @FXML
    public void initialize() {
        // La cotización se formatea sola mientras se escribe: 1450.5 -> 1.450,5
        FormatoMoneda.aplicar(txtCotizacionDolar);

        tiposDolar.put("Dólar blue", "blue");
        // El "oficial" es el del Banco Nación: se aclara porque mucha gente lo conoce como "dólar banco"
        tiposDolar.put("Dólar oficial (Banco Nación)", "oficial");
        tiposDolar.put("Dólar tarjeta", "tarjeta");
        tiposDolar.put("Dólar bolsa (MEP)", "bolsa");
        cmbTipoDolar.getItems().setAll(tiposDolar.keySet());

        // El ejemplo del aumento acompaña a lo que se va escribiendo
        txtPorcentajeAumento.textProperty().addListener((obs, viejo, nuevo) -> actualizarEjemplo());
        txtPorcentajeAumento.setOnAction(this::aplicarAumentoMasivo);

        configurarTablaHistorial();
        cargarCategorias();
        cargarAjustesDivisa();
        cargarHistorial(0);
    }

    // =====================================================================
    //  AUMENTO POR PORCENTAJE
    // =====================================================================

    private void cargarCategorias() {
        cmbCategoriaAumento.setConverter(new StringConverter<>() {
            @Override public String toString(Categoria c) { return c == null ? "" : c.getNombre(); }
            @Override public Categoria fromString(String s) { return null; }
        });
        cmbCategoriaAumento.getItems().clear();
        try {
            List<Categoria> categorias = parametrosApiService.obtenerCategorias();
            if (categorias != null) {
                for (Categoria categoria : categorias) {
                    if (categoria != null && categoria.getNombre() != null && !categoria.getNombre().isBlank()) {
                        cmbCategoriaAumento.getItems().add(categoria);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al cargar categorías: " + e.getMessage());
        }

        // Última opción: todo el catálogo (va sin id; el backend lo entiende como "todas")
        Categoria todas = new Categoria();
        todas.setNombre("Todas las categorías");
        cmbCategoriaAumento.getItems().add(todas);

        cmbCategoriaAumento.getSelectionModel().select(0);
    }

    /** "Ej: con 50, un artículo de $ 10.000 pasa a $ 15.000" con el porcentaje que se está escribiendo. */
    private void actualizarEjemplo() {
        Double porcentaje = leerPorcentaje();
        if (porcentaje == null || porcentaje <= 0) {
            lblEjemploAumento.setText("Ej: con 15, un artículo de $ 10.000 pasa a $ 11.500");
            return;
        }
        double nuevo = 10000 * (1 + porcentaje / 100);
        lblEjemploAumento.setText("Con " + unDecimal.format(porcentaje) + " %, un artículo de $ 10.000 pasa a "
                + FormatoMoneda.pesos(nuevo).replace(",00", ""));
    }

    private Double leerPorcentaje() {
        String texto = txtPorcentajeAumento.getText();
        if (texto == null || texto.isBlank()) return null;
        try {
            return Double.parseDouble(texto.trim().replace("%", "").replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @FXML
    public void aplicarAumentoMasivo(ActionEvent event) {
        Categoria categoria = cmbCategoriaAumento.getValue();
        if (categoria == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Falta la categoría", "Elegí a qué categoría querés aumentarle los precios.");
            return;
        }

        Double porcentaje = leerPorcentaje();
        if (porcentaje == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Falta el porcentaje", "Escribí el porcentaje de aumento con números.\nEjemplo: 20");
            return;
        }
        if (porcentaje <= 0) {
            mostrarAlerta(Alert.AlertType.WARNING, "Porcentaje inválido", "El porcentaje debe ser mayor que cero.");
            return;
        }
        if (porcentaje > 1000) {
            mostrarAlerta(Alert.AlertType.WARNING, "Porcentaje demasiado alto", "El aumento no puede superar el 1000 %.");
            return;
        }

        boolean confirmo = confirmar("Confirmar aumento de precios",
                "Vas a subir los precios de: " + categoria.getNombre(),
                "Aumento: " + unDecimal.format(porcentaje) + " %\n"
                        + "Un artículo de $ 10.000 pasa a " + FormatoMoneda.pesos(10000 * (1 + porcentaje / 100)) + ".\n\n"
                        + "Si después te arrepentís, tenés 7 días para volver atrás desde el historial de abajo.",
                "Sí, aumentar");
        if (!confirmo) return;

        int cantidad = articuloApiService.aumentarPrecios(categoria.getId(), BigDecimal.valueOf(porcentaje));
        if (cantidad >= 0) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Precios actualizados",
                    "Se actualizaron " + cantidad + " artículos de " + categoria.getNombre() + ".");
            txtPorcentajeAumento.clear();
            cargarHistorial(0);
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo aumentar",
                    "No se cambiaron los precios. Puede que esa categoría no tenga artículos activos.");
        }
    }

    // =====================================================================
    //  DÓLAR
    // =====================================================================

    private void cargarAjustesDivisa() {
        try {
            AjustesDivisa ajustes = apiService.obtenerAjustesDivisa();
            if (ajustes == null || ajustes.getCotizacionUsd() == null) return;

            cotizacionGuardada = ajustes.getCotizacionUsd();
            cotizacionBasePrecios = ajustes.getCotizacionBasePrecios() != null ? ajustes.getCotizacionBasePrecios() : cotizacionGuardada;
            tipoDolarBase = ajustes.getTipoDolarBase() != null ? ajustes.getTipoDolarBase() : "blue";
            fechaBasePrecios = ajustes.getFechaBasePrecios() != null ? ajustes.getFechaBasePrecios() : "";
            FormatoMoneda.poner(txtCotizacionDolar, cotizacionGuardada);

            String tipo = ajustes.getTipoDolar() == null ? "blue" : ajustes.getTipoDolar();
            cmbTipoDolar.setValue(tiposDolar.entrySet().stream()
                    .filter(e -> e.getValue().equals(tipo)).map(Map.Entry::getKey).findFirst().orElse("Dólar blue"));
        } catch (Exception e) {
            System.err.println("Error al cargar la configuración del dólar: " + e.getMessage());
        }
        actualizarEstadoDolar();
    }

    /** Cartel que dice si los precios están al día con el dólar o cuánto se atrasaron. */
    private void actualizarEstadoDolar() {
        if (cotizacionGuardada <= 0 || cotizacionBasePrecios <= 0) {
            lblEstadoDolar.setText("Guardá la cotización de hoy para empezar a seguir al dólar.");
            lblEstadoDolar.setStyle("-fx-font-size: 14px; -fx-text-fill: #5F6B76;");
            btnSincronizarDolar.setDisable(true);
            return;
        }

        double variacion = (cotizacionGuardada - cotizacionBasePrecios) / cotizacionBasePrecios * 100;
        if (Math.abs(cotizacionGuardada - cotizacionBasePrecios) < 0.005) {
            lblEstadoDolar.setText("Tus precios están al día: están calculados con el último valor del " + dolarBase() + ".");
            lblEstadoDolar.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2E9E6B;");
            btnSincronizarDolar.setDisable(true);
            btnSincronizarDolar.setText("Poner los precios al día con el dólar");
        } else if (variacion > 0) {
            lblEstadoDolar.setText("Tus precios se atrasaron: están calculados con el " + dolarBase()
                    + ", y hoy lo tenés a " + pesosSinDecimales(cotizacionGuardada)
                    + ". El dólar subió un " + unDecimal.format(variacion) + " %.");
            lblEstadoDolar.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #C0563F;");
            btnSincronizarDolar.setDisable(false);
            btnSincronizarDolar.setText("Subir todos los precios un " + unDecimal.format(variacion) + " %");
        } else {
            lblEstadoDolar.setText("El dólar bajó: tus precios están calculados con el " + dolarBase()
                    + ", y hoy lo tenés a " + pesosSinDecimales(cotizacionGuardada)
                    + " (" + unDecimal.format(Math.abs(variacion)) + " % menos).");
            lblEstadoDolar.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
            btnSincronizarDolar.setDisable(false);
            btnSincronizarDolar.setText("Bajar todos los precios un " + unDecimal.format(Math.abs(variacion)) + " %");
        }
    }

    /** "dólar oficial a $ 1.600, que es el valor del 23/11/2026": el dólar con el que están calculados los precios. */
    private String dolarBase() {
        String nombre = switch (tipoDolarBase == null ? "" : tipoDolarBase) {
            case "oficial" -> "dólar oficial (Banco Nación)";
            case "tarjeta" -> "dólar tarjeta";
            case "bolsa" -> "dólar bolsa (MEP)";
            default -> "dólar blue";
        };
        return nombre + " a " + pesosSinDecimales(cotizacionBasePrecios)
                + (fechaBasePrecios == null || fechaBasePrecios.isBlank() ? "" : ", que es el valor del " + fechaBasePrecios);
    }

    /** Pregunta antes de salir a internet; si acepta, trae la cotización y la deja escrita (sin guardar). */
    @FXML
    public void buscarCotizacionEnInternet(ActionEvent event) {
        String nombreTipo = cmbTipoDolar.getValue() == null ? "Dólar blue" : cmbTipoDolar.getValue();

        boolean continuar = confirmar("Ver cotización en internet",
                "Para saber a cuánto está el " + nombreTipo.toLowerCase() + " hoy, el sistema va a consultar una página de internet.",
                "Se necesita conexión. No se envía ningún dato del local: solo se lee el valor del dólar.\n\n"
                        + "Si preferís, podés escribir el valor a mano en el casillero.",
                "Sí, consultar");
        if (!continuar) {
            txtCotizacionDolar.requestFocus();
            return;
        }

        PreciosApiService.Respuesta respuesta = preciosApi.cotizacionOnline(tiposDolar.getOrDefault(nombreTipo, "blue"));
        if (!respuesta.ok()) {
            mostrarAlerta(Alert.AlertType.WARNING, "No se pudo consultar", respuesta.cuerpo());
            txtCotizacionDolar.requestFocus();
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(respuesta.cuerpo()).getAsJsonObject();
            double venta = json.get("venta").getAsDouble();
            FormatoMoneda.poner(txtCotizacionDolar, venta);
            mostrarAlerta(Alert.AlertType.INFORMATION, "Cotización encontrada",
                    "El " + nombreTipo.toLowerCase() + " está a " + pesosSinDecimales(venta) + " (valor de venta).\n\n"
                            + "Ya quedó escrito en el casillero. Si estás de acuerdo, tocá «Guardar cotización».");
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.WARNING, "No se pudo leer la cotización", "Escribí el valor a mano en el casillero.");
        }
    }

    @FXML
    public void guardarCotizacion(ActionEvent event) {
        String texto = txtCotizacionDolar.getText();
        if (texto == null || texto.isBlank()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Falta la cotización", "Escribí a cuánto está el dólar.");
            return;
        }

        try {
            Double valor = FormatoMoneda.leer(texto);
            if (valor == null || valor <= 0) {
                mostrarAlerta(Alert.AlertType.WARNING, "Cotización inválida", "La cotización debe ser mayor que cero.");
                return;
            }

            AjustesDivisa ajustes = new AjustesDivisa(valor, false);
            ajustes.setTipoDolar(tiposDolar.getOrDefault(cmbTipoDolar.getValue(), "blue"));

            if (apiService.guardarAjustesDivisa(ajustes)) {
                cargarAjustesDivisa(); // vuelve a leer lo guardado y actualiza el cartel de "al día / atrasados"
                mostrarAlerta(Alert.AlertType.INFORMATION, "Cotización guardada",
                        "Quedó guardado el dólar a " + pesosSinDecimales(valor) + ".\n\n"
                                + "Esto no cambia ningún precio. Mirá el cartel de abajo para ver si tus precios quedaron atrasados.");
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "No se pudo guardar", "El sistema no pudo guardar la cotización. Probá de nuevo.");
            }
        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Cotización inválida", "Escribí un número válido.\nEjemplo: 1420");
        }
    }

    /** Lleva todos los precios al dólar guardado (sube o baja en la misma proporción que el dólar). */
    @FXML
    public void sincronizarPreciosConDolar(ActionEvent event) {
        if (cotizacionGuardada <= 0 || cotizacionBasePrecios <= 0) return;
        double variacion = (cotizacionGuardada - cotizacionBasePrecios) / cotizacionBasePrecios * 100;
        boolean sube = variacion > 0;

        boolean confirmo = confirmar("Poner los precios al día con el dólar",
                "Vas a " + (sube ? "subir" : "bajar") + " TODOS los precios un " + unDecimal.format(Math.abs(variacion)) + " %",
                "Precios calculados con el " + dolarBase() + ".\n"
                        + "Dólar de hoy: " + pesosSinDecimales(cotizacionGuardada) + "\n"
                        + "Un artículo de $ 10.000 pasa a " + FormatoMoneda.pesos(10000 * cotizacionGuardada / cotizacionBasePrecios) + ".\n\n"
                        + "Si después te arrepentís, tenés 7 días para volver atrás desde el historial de abajo.",
                sube ? "Sí, subir precios" : "Sí, bajar precios");
        if (!confirmo) return;

        PreciosApiService.Respuesta respuesta = preciosApi.actualizarPreciosSegunDolar();
        if (!respuesta.ok()) {
            mostrarAlerta(Alert.AlertType.ERROR, "No se actualizaron los precios", respuesta.cuerpo());
            return;
        }

        int cantidad = 0;
        try {
            cantidad = JsonParser.parseString(respuesta.cuerpo()).getAsJsonObject().get("cantidadArticulos").getAsInt();
        } catch (Exception ignorado) { }
        cargarAjustesDivisa();
        cargarHistorial(0);
        mostrarAlerta(Alert.AlertType.INFORMATION, "Precios al día",
                "Se actualizaron " + cantidad + " artículos. Tus precios ya están calculados con el dólar a "
                        + pesosSinDecimales(cotizacionGuardada) + ".");
    }

    // =====================================================================
    //  HISTORIAL
    // =====================================================================

    private void configurarTablaHistorial() {
        colHistFecha.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue(), "fecha")));
        colHistQue.setCellValueFactory(c -> new SimpleStringProperty(describir(c.getValue())));
        colHistArticulos.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue(), "cantidadArticulos")));
        colHistEstado.setCellValueFactory(c -> new SimpleStringProperty(estado(c.getValue())));
        colHistAccion.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));

        colHistAccion.setCellFactory(col -> new TableCell<>() {
            private final Button btnVolver = new Button("Volver atrás");
            {
                btnVolver.getStyleClass().add("btn-azul");
                btnVolver.setStyle("-fx-padding: 3 12; -fx-font-size: 13px;");
                btnVolver.setOnAction(e -> volverAtras(getItem()));
                setStyle("-fx-alignment: CENTER;");
            }

            @Override
            protected void updateItem(JsonObject fila, boolean empty) {
                super.updateItem(fila, empty);
                boolean sePuede = !empty && fila != null && fila.get("sePuedeDeshacer").getAsBoolean();
                setGraphic(sePuede ? btnVolver : null);
                setText(null);
            }
        });

        tablaHistorial.setFixedCellSize(34);
        tablaHistorial.setPlaceholder(new Label("Todavía no hiciste ningún cambio masivo de precios."));
    }

    private String describir(JsonObject fila) {
        double porcentaje = fila.get("porcentaje").getAsDouble();
        String signo = porcentaje >= 0 ? "+" : "−";
        String cuanto = signo + unDecimal.format(Math.abs(porcentaje)) + " %";
        if ("DOLAR".equals(texto(fila, "tipo"))) {
            String detalle = texto(fila, "detalle");
            return cuanto + " a todo  ·  " + (detalle.isBlank() ? "por el dólar" : detalle);
        }
        return cuanto + " a " + texto(fila, "categoria");
    }

    private String estado(JsonObject fila) {
        if ("DESHECHO".equals(texto(fila, "estado"))) {
            String cuando = texto(fila, "fechaDeshecho"); // "23/11/2026 20:21": en la tabla alcanza con el día
            return "Se volvió atrás el " + (cuando.length() >= 10 ? cuando.substring(0, 10) : cuando);
        }
        if (fila.get("sePuedeDeshacer").getAsBoolean()) {
            long dias = fila.get("diasRestantes").getAsLong();
            return dias <= 1 ? "Último día para volver atrás" : "Quedan " + dias + " días para volver atrás";
        }
        return "Firme (pasaron los 7 días)";
    }

    private void cargarHistorial(int pagina) {
        PreciosApiService.Respuesta respuesta = preciosApi.historial(pagina, CAMBIOS_POR_PAGINA);
        ObservableList<JsonObject> filas = FXCollections.observableArrayList();
        if (respuesta.ok()) {
            try {
                JsonObject json = JsonParser.parseString(respuesta.cuerpo()).getAsJsonObject();
                JsonArray contenido = json.getAsJsonArray("content");
                for (JsonElement e : contenido) filas.add(e.getAsJsonObject());
                totalPaginasHistorial = Math.max(1, json.get("totalPages").getAsInt());
                paginaHistorial = Math.min(pagina, totalPaginasHistorial - 1);
            } catch (Exception e) {
                System.err.println("Error al leer el historial de aumentos: " + e.getMessage());
            }
        } else {
            tablaHistorial.setPlaceholder(new Label(respuesta.cuerpo()));
        }

        tablaHistorial.setItems(filas);
        lblPaginaHistorial.setText("Página " + (paginaHistorial + 1) + " de " + totalPaginasHistorial);
        btnHistorialAnterior.setDisable(paginaHistorial <= 0);
        btnHistorialSiguiente.setDisable(paginaHistorial >= totalPaginasHistorial - 1);
    }

    @FXML
    public void historialAnterior(ActionEvent event) {
        if (paginaHistorial > 0) cargarHistorial(paginaHistorial - 1);
    }

    @FXML
    public void historialSiguiente(ActionEvent event) {
        if (paginaHistorial < totalPaginasHistorial - 1) cargarHistorial(paginaHistorial + 1);
    }

    private void volverAtras(JsonObject fila) {
        if (fila == null) return;

        boolean confirmo = confirmar("Volver atrás un cambio de precios",
                "Vas a deshacer: " + describir(fila),
                "Hecho el " + texto(fila, "fecha") + " sobre " + texto(fila, "cantidadArticulos") + " artículos.\n\n"
                        + "Los precios de esos artículos vuelven a como estaban antes de ese cambio.\n"
                        + "Si a alguno le cambiaste el precio a mano después, ese no se toca.",
                "Sí, volver atrás");
        if (!confirmo) return;

        PreciosApiService.Respuesta respuesta = preciosApi.deshacer(fila.get("id").getAsLong());
        if (!respuesta.ok()) {
            mostrarAlerta(Alert.AlertType.WARNING, "No se pudo volver atrás", respuesta.cuerpo());
            return;
        }

        String mensaje = "Los precios volvieron a como estaban.";
        try {
            JsonObject json = JsonParser.parseString(respuesta.cuerpo()).getAsJsonObject();
            int restaurados = json.get("restaurados").getAsInt();
            int sinTocar = json.get("sinTocar").getAsInt();
            mensaje = "Se restauró el precio de " + restaurados + " artículos."
                    + (sinTocar > 0 ? "\n\n" + sinTocar + " no se tocaron porque su precio había cambiado después." : "");
        } catch (Exception ignorado) { }

        cargarAjustesDivisa(); // si era un cambio por dólar, el cartel de "al día / atrasados" cambia
        cargarHistorial(paginaHistorial);
        mostrarAlerta(Alert.AlertType.INFORMATION, "Listo", mensaje);
    }

    // =====================================================================
    //  AYUDAS
    // =====================================================================

    private String texto(JsonObject o, String campo) {
        JsonElement e = o.get(campo);
        return e == null || e.isJsonNull() ? "" : e.getAsString();
    }

    private String pesosSinDecimales(double valor) {
        return "$ " + unDecimal.format(valor);
    }

    /** Confirmación con el botón de aceptar en palabras ("Sí, aumentar") y "Cancelar". */
    private boolean confirmar(String titulo, String encabezado, String detalle, String textoAceptar) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle(titulo);
        confirmacion.setHeaderText(encabezado);
        confirmacion.setContentText(detalle);
        ButtonType aceptar = new ButtonType(textoAceptar, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmacion.getButtonTypes().setAll(aceptar, cancelar);
        Optional<ButtonType> resultado = confirmacion.showAndWait();
        return resultado.isPresent() && resultado.get() == aceptar;
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
