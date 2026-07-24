package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.CajaDiaria;
import com.nakel.frontend.model.MovimientoCaja;
import com.nakel.frontend.service.MovimientoCajaApiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;

public class ModalDetalleCajaController {

    @FXML private Label lblFechaApertura;
    @FXML private Label lblFechaCierre;
    @FXML private Label lblEstado;
    @FXML private Label lblTotalVentas;
    @FXML private Label lblEgresos;
    @FXML private Label lblSaldoFinal;

    @FXML private TableView<MovimientoCaja> tablaMovimientos;
    @FXML private TableColumn<MovimientoCaja, String> colHora;
    @FXML private TableColumn<MovimientoCaja, String> colTipo;
    @FXML private TableColumn<MovimientoCaja, String> colConcepto;
    @FXML private TableColumn<MovimientoCaja, BigDecimal> colMonto;

    private final MovimientoCajaApiService movimientoApiService = new MovimientoCajaApiService();
    private final Gson gson = new Gson();
    private final ObservableList<MovimientoCaja> listaMovimientos = FXCollections.observableArrayList();

    public void inicializarDatos(CajaDiaria caja) {
        lblFechaApertura.setText(caja.getFechaApertura() != null ? caja.getFechaApertura() : "-");
        lblFechaCierre.setText(caja.getFechaCierre() != null ? caja.getFechaCierre() : "Abierta actualmente");
        lblEstado.setText(caja.getEstado());
        lblTotalVentas.setText("$ " + (caja.getTotalVentas() != null ? caja.getTotalVentas() : "0.00"));
        lblEgresos.setText("$ " + (caja.getTotalEgresos() != null ? caja.getTotalEgresos() : "0.00"));
        lblSaldoFinal.setText("$ " + (caja.getSaldoFinal() != null ? caja.getSaldoFinal() : "0.00"));

        configurarTabla();
        cargarMovimientos(caja.getId());
    }

    private void configurarTabla() {
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colConcepto.setCellValueFactory(new PropertyValueFactory<>("concepto"));
        colMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));

        colHora.setCellValueFactory(cellData -> {
            String fechaCruda = cellData.getValue().getFechaHora();
            if (fechaCruda != null && fechaCruda.contains("T")) {
                return new SimpleStringProperty(fechaCruda.split("T")[1].substring(0, 5));
            }
            return new SimpleStringProperty(fechaCruda);
        });

        tablaMovimientos.setItems(listaMovimientos);
    }

    private void cargarMovimientos(Long cajaId) {
        String json = movimientoApiService.obtenerMovimientosPorCaja(cajaId);
        if (json != null && !json.isEmpty()) {
            Type listType = new TypeToken<List<MovimientoCaja>>(){}.getType();
            List<MovimientoCaja> movimientos = gson.fromJson(json, listType);
            listaMovimientos.clear();
            if (movimientos != null) {
                listaMovimientos.addAll(movimientos);
            }
        }
    }

    @FXML
    public void cerrarModal() {
        Stage stage = (Stage) lblEstado.getScene().getWindow();
        stage.close();
    }
}