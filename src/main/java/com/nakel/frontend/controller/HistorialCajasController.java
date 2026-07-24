package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.CajaDiaria;
import com.nakel.frontend.service.CajaApiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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

    private final CajaApiService cajaApiService = new CajaApiService();
    private final Gson gson = new Gson();
    private final ObservableList<CajaDiaria> listaCajas = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTabla();
        cargarHistorial();
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
    public void cargarHistorial() {
        String json = cajaApiService.obtenerHistorial();
        if (json != null && !json.isEmpty()) {
            Type listType = new TypeToken<List<CajaDiaria>>(){}.getType();
            List<CajaDiaria> cajas = gson.fromJson(json, listType);
            listaCajas.clear();
            if (cajas != null) {
                listaCajas.addAll(cajas);
            }
        }
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