package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.CajaDiaria;
import com.nakel.frontend.service.CajaApiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistorialCajasController {

    @FXML private TableView<CajaDiaria> tablaCajas;
    @FXML private TableColumn<CajaDiaria, Long> colId;
    @FXML private TableColumn<CajaDiaria, String> colFecha;
    @FXML private TableColumn<CajaDiaria, Integer> colVentas;
    @FXML private TableColumn<CajaDiaria, String> colTotal;
    @FXML private TableColumn<CajaDiaria, String> colEstado;
    @FXML private TableColumn<CajaDiaria, Void> colAcciones;

    @FXML private Pagination paginadorCajas;

    // Filtros
    @FXML private ComboBox<String> cmbMes;
    @FXML private ComboBox<String> cmbAnio;
    @FXML private ComboBox<String> cmbOrden;
    @FXML private Label lblResumen;

    private static final String TODOS = "Todos";
    private static final String[] MESES = {TODOS, "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};

    // Texto que ve la usuaria -> orden que se le pide al backend (campo,dirección)
    private static final Map<String, String> ORDENES = new LinkedHashMap<>();
    static {
        ORDENES.put("Más recientes primero", "fechaApertura,desc");
        ORDENES.put("Más antiguas primero", "fechaApertura,asc");
        ORDENES.put("Mayor ingreso primero", "totalVentas,desc");
        ORDENES.put("Menor ingreso primero", "totalVentas,asc");
        ORDENES.put("Más ventas primero", "cantidadVentas,desc");
    }

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");

    private final CajaApiService cajaApiService = new CajaApiService();
    private final Gson gson = new Gson();
    private final ObservableList<CajaDiaria> listaCajas = FXCollections.observableArrayList();

    // Evita recargar varias veces mientras se arman o limpian los filtros
    private boolean ajustandoFiltros = false;

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFiltros();

        if (paginadorCajas != null) {
            paginadorCajas.setPageFactory(paginaIndex -> {
                cargarHistorial(paginaIndex);
                return new javafx.scene.layout.VBox();
            });
        } else {
            cargarHistorial(0);
        }
    }

    private void configurarFiltros() {
        if (cmbMes == null || cmbAnio == null || cmbOrden == null) return;

        ajustandoFiltros = true;

        cmbMes.getItems().setAll(MESES);
        cmbMes.setValue(TODOS);

        // Años: desde 2025 hasta el actual, el más nuevo arriba
        cmbAnio.getItems().add(TODOS);
        for (int anio = LocalDate.now().getYear(); anio >= 2025; anio--) {
            cmbAnio.getItems().add(String.valueOf(anio));
        }
        cmbAnio.setValue(TODOS);

        cmbOrden.getItems().setAll(ORDENES.keySet());
        cmbOrden.setValue("Más recientes primero");

        ajustandoFiltros = false;

        // Cualquier cambio de filtro vuelve a la primera página y recarga
        cmbMes.valueProperty().addListener((obs, antes, ahora) -> recargarDesdeElPrincipio());
        cmbAnio.valueProperty().addListener((obs, antes, ahora) -> recargarDesdeElPrincipio());
        cmbOrden.valueProperty().addListener((obs, antes, ahora) -> recargarDesdeElPrincipio());
    }

    private void recargarDesdeElPrincipio() {
        if (ajustandoFiltros) return;
        if (paginadorCajas != null && paginadorCajas.getCurrentPageIndex() != 0) {
            paginadorCajas.setCurrentPageIndex(0); // esto ya dispara la carga de la página 0
        } else {
            cargarHistorial(0);
        }
    }

    @FXML
    public void limpiarFiltros() {
        if (cmbMes == null) return;
        ajustandoFiltros = true;
        cmbMes.setValue(TODOS);
        cmbAnio.setValue(TODOS);
        cmbOrden.setValue("Más recientes primero");
        ajustandoFiltros = false;
        recargarDesdeElPrincipio();
    }

    private int mesElegido() {
        if (cmbMes == null || cmbMes.getValue() == null) return 0;
        for (int i = 1; i < MESES.length; i++) {
            if (MESES[i].equals(cmbMes.getValue())) return i;
        }
        return 0;
    }

    private int anioElegido() {
        if (cmbAnio == null || cmbAnio.getValue() == null || TODOS.equals(cmbAnio.getValue())) return 0;
        try {
            return Integer.parseInt(cmbAnio.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String ordenElegido() {
        if (cmbOrden == null || cmbOrden.getValue() == null) return null;
        return ORDENES.get(cmbOrden.getValue());
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colVentas.setCellValueFactory(new PropertyValueFactory<>("cantidadVentas"));

        colEstado.setCellValueFactory(cellData -> {
            String estado = cellData.getValue().getEstado();
            return new SimpleStringProperty("ABIERTA".equalsIgnoreCase(estado) ? "Abierta" : "Cerrada");
        });

        // Fecha en formato argentino: 23/11/2026 - 15:09
        colFecha.setCellValueFactory(cellData -> new SimpleStringProperty(formatearFecha(cellData.getValue().getFechaApertura())));

        // Total en pesos: $ 29.521,25
        colTotal.setCellValueFactory(cellData -> {
            BigDecimal total = cellData.getValue().getTotalVentas();
            return new SimpleStringProperty(com.nakel.frontend.util.FormatoMoneda.pesos(total));
        });

        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnVer = new Button("", new FontIcon("fas-eye"));

            {
                com.nakel.frontend.util.Acciones.ver(btnVer, "Ver los movimientos de la caja");
                btnVer.setOnAction(event -> {
                    CajaDiaria cajaSeleccionada = getTableView().getItems().get(getIndex());
                    abrirModalDetalle(cajaSeleccionada);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(btnVer);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        tablaCajas.setPlaceholder(new Label("No hay cajas para el período elegido."));
        tablaCajas.setItems(listaCajas);
    }

    static String formatearFecha(String fechaIso) {
        if (fechaIso == null || fechaIso.isBlank()) return "-";
        try {
            return LocalDateTime.parse(fechaIso).format(FECHA_HORA);
        } catch (Exception e) {
            return fechaIso;
        }
    }

    public void cargarHistorial(int numeroPagina) {
        String json = cajaApiService.obtenerHistorial(numeroPagina, 20, mesElegido(), anioElegido(), ordenElegido());

        long totalCajas = 0;
        listaCajas.clear();

        if (json != null && !json.equals("[]") && !json.isEmpty()) {
            try {
                com.google.gson.JsonElement elementoParseado = JsonParser.parseString(json);
                JsonArray arregloCajas;

                if (elementoParseado.isJsonObject()) {
                    JsonObject respuestaServidor = elementoParseado.getAsJsonObject();
                    if (respuestaServidor.has("totalPages") && paginadorCajas != null) {
                        int totalPaginas = respuestaServidor.get("totalPages").getAsInt();
                        paginadorCajas.setPageCount(totalPaginas == 0 ? 1 : totalPaginas);
                    }
                    if (respuestaServidor.has("totalElements")) {
                        totalCajas = respuestaServidor.get("totalElements").getAsLong();
                    }
                    arregloCajas = respuestaServidor.getAsJsonArray("content");
                } else if (elementoParseado.isJsonArray()) {
                    arregloCajas = elementoParseado.getAsJsonArray();
                    if (paginadorCajas != null) {
                        paginadorCajas.setPageCount(1);
                    }
                    totalCajas = arregloCajas.size();
                } else {
                    throw new RuntimeException("Formato JSON no reconocido");
                }

                Type listType = new TypeToken<List<CajaDiaria>>(){}.getType();
                List<CajaDiaria> cajas = gson.fromJson(arregloCajas, listType);
                if (cajas != null) {
                    listaCajas.addAll(cajas);
                }
            } catch (Exception e) {
                System.out.println("Error al cargar el historial de cajas: " + e.getMessage());
                e.printStackTrace();
            }
        } else if (paginadorCajas != null) {
            paginadorCajas.setPageCount(1);
        }

        // Un solo total y bien claro: los ingresos de TODAS las cajas del período elegido,
        // sin importar en qué página estén. (Antes sumaba solo la página visible y confundía.)
        if (lblResumen != null) {
            if (totalCajas == 0) {
                lblResumen.setText("");
            } else {
                BigDecimal totalPeriodo = cajaApiService.obtenerTotalHistorial(mesElegido(), anioElegido());
                lblResumen.setText("Cantidad de cajas: " + totalCajas
                        + "     Total facturado: "
                        + (totalPeriodo != null ? com.nakel.frontend.util.FormatoMoneda.pesos(totalPeriodo) : "no disponible"));
            }
        }
    }

    // Botón "Actualizar": recarga la página en la que está parada
    @FXML
    public void cargarHistorial() {
        cargarHistorial(paginadorCajas != null ? paginadorCajas.getCurrentPageIndex() : 0);
    }

    // Abre el modal del detalle con los movimientos al hacer clic en el ojito
    private void abrirModalDetalle(CajaDiaria caja) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/modal-detalle-caja.fxml"));
            javafx.scene.Parent root = loader.load();

            ModalDetalleCajaController controller = loader.getController();
            controller.inicializarDatos(caja);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Detalle de Caja N° " + caja.getId());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            if (tablaCajas.getScene() != null) stage.initOwner(tablaCajas.getScene().getWindow());
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error al abrir modal de caja: " + e.getMessage());
        }
    }
}
