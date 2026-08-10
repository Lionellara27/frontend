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
import org.kordamp.ikonli.javafx.FontIcon; // 🔥 IMPORTANTE: El icono piola

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;

public class HistorialCajasController {

    @FXML private TableView<CajaDiaria> tablaCajas;
    @FXML private TableColumn<CajaDiaria, Long> colId;
    @FXML private TableColumn<CajaDiaria, String> colFecha;
    @FXML private TableColumn<CajaDiaria, Integer> colVentas;
    @FXML private TableColumn<CajaDiaria, String> colTotal;
    @FXML private TableColumn<CajaDiaria, String> colEstado;
    @FXML private TableColumn<CajaDiaria, Void> colAcciones;

    @FXML private Pagination paginadorHistorial;

    private final CajaApiService cajaApiService = new CajaApiService();
    private final Gson gson = new Gson();
    private final ObservableList<CajaDiaria> listaCajas = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTabla();
        if (paginadorHistorial != null) {
            paginadorHistorial.setPageFactory(paginaIndex -> {
                cargarHistorial(paginaIndex);
                return new javafx.scene.layout.VBox();
            });
        } else {
            cargarHistorial(0);
        }
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colVentas.setCellValueFactory(new PropertyValueFactory<>("cantidadVentas"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        // Formatear Fecha (Limpia la "T" del formato de fecha/hora)
        colFecha.setCellValueFactory(cellData -> {
            String fechaCruda = cellData.getValue().getFechaApertura();
            if (fechaCruda != null && fechaCruda.contains("T")) {
                String[] partes = fechaCruda.split("T");
                return new SimpleStringProperty(partes[0] + " " + partes[1].substring(0, 5));
            }
            return new SimpleStringProperty(fechaCruda);
        });

        // Formatear Total con signo $
        colTotal.setCellValueFactory(cellData -> {
            BigDecimal total = cellData.getValue().getTotalVentas();
            return new SimpleStringProperty(total != null ? "$ " + total.toString() : "$ 0.00");
        });

        // 🔥 ACÁ ESTÁ EL CAMBIO: Configurar el botón del Ojito con Ikonli
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnVer = new Button("", new FontIcon("fas-eye")); // Ojito profesional

            {
                btnVer.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");
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

        tablaCajas.setItems(listaCajas);
    }

    @FXML
    public void cargarHistorial(int numeroPagina) {
        System.out.println("🚀 [FRONTEND-CAJAS] Pidiendo historial al Backend. Página: " + numeroPagina);
        String json = cajaApiService.obtenerHistorial(numeroPagina, 20);
        System.out.println("📦 [FRONTEND-CAJAS] JSON Crudo recibido: " + json);
        if (json != null && !json.equals("[]") && !json.isEmpty()) {
            try {
                com.google.gson.JsonElement elementoParseado = JsonParser.parseString(json);
                JsonArray arregloCajas;

                if (elementoParseado.isJsonObject()) {
                    JsonObject respuestaServidor = elementoParseado.getAsJsonObject();
                    if (respuestaServidor.has("totalPages") && paginadorHistorial != null) {
                        int totalPaginas = respuestaServidor.get("totalPages").getAsInt();
                        paginadorHistorial.setPageCount(totalPaginas == 0 ? 1 : totalPaginas);
                    }
                    System.out.println("🔎 [FRONTEND-CAJAS] Objeto JSON detectado. Extrayendo 'content'...");
                    arregloCajas = respuestaServidor.getAsJsonArray("content");
                } else if (elementoParseado.isJsonArray()) {
                    System.out.println("🔎 [FRONTEND-CAJAS] Arreglo plano detectado.");
                    arregloCajas = elementoParseado.getAsJsonArray();
                    if (paginadorHistorial != null) {
                        paginadorHistorial.setPageCount(1);
                    }
                } else {
                    throw new RuntimeException("Formato JSON no reconocido");
                }

                Type listType = new TypeToken<List<CajaDiaria>>(){}.getType();
                List<CajaDiaria> cajas = gson.fromJson(arregloCajas, listType);
                System.out.println("✅ [FRONTEND-CAJAS] GSON parseó con éxito " + (cajas != null ? cajas.size() : 0) + " cajas.");
                listaCajas.clear();
                if (cajas != null) {
                    listaCajas.addAll(cajas);
                }
            } catch (Exception e) {
                System.out.println("❌ [FRONTEND-CAJAS] Error al parsear el JSON: " + e.getMessage());
                System.out.println("❌ Error al cargar historial paginado: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ [FRONTEND-CAJAS] El servidor devolvió un JSON vacío o nulo.");
            listaCajas.clear();
        }
    }

    // Sobrecarga limpia para que el botón "Actualizar" funcione sin romper nada
    @FXML
    public void cargarHistorial() {
        cargarHistorial(paginadorHistorial != null ? paginadorHistorial.getCurrentPageIndex() : 0);
    }

    // Abre el modal del detalle con los movimientos al hacer clic en el ojito
    private void abrirModalDetalle(CajaDiaria caja) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/modal-detalle-caja.fxml"));
            javafx.scene.Parent root = loader.load();

            ModalDetalleCajaController controller = loader.getController();
            controller.inicializarDatos(caja);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Detalle de Caja #" + caja.getId());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Error al abrir modal de caja: " + e.getMessage());
        }
    }
}