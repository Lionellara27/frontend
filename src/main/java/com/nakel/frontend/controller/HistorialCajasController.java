package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.CajaDiaria; // 🔥 Ahora importa de tu Model
import com.nakel.frontend.service.CajaApiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;

public class HistorialCajasController {

    @FXML private TableView<CajaDiaria> tablaCajas;
    @FXML private TableColumn<CajaDiaria, Long> colId;
    @FXML private TableColumn<CajaDiaria, String> colEstado;
    @FXML private TableColumn<CajaDiaria, String> colApertura;
    @FXML private TableColumn<CajaDiaria, String> colCierre;
    @FXML private TableColumn<CajaDiaria, BigDecimal> colSaldoInicial;
    @FXML private TableColumn<CajaDiaria, BigDecimal> colEfectivo;
    @FXML private TableColumn<CajaDiaria, BigDecimal> colMercadoPago;
    @FXML private TableColumn<CajaDiaria, BigDecimal> colTransferencia;
    @FXML private TableColumn<CajaDiaria, BigDecimal> colTotalVentas;
    @FXML private TableColumn<CajaDiaria, BigDecimal> colSaldoFinal;
    @FXML private TableColumn<CajaDiaria, Integer> colCantVentas;

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
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colApertura.setCellValueFactory(new PropertyValueFactory<>("fechaApertura"));
        colCierre.setCellValueFactory(new PropertyValueFactory<>("fechaCierre"));
        colSaldoInicial.setCellValueFactory(new PropertyValueFactory<>("saldoInicial"));
        colEfectivo.setCellValueFactory(new PropertyValueFactory<>("totalEfectivo"));
        colMercadoPago.setCellValueFactory(new PropertyValueFactory<>("totalMercadoPago"));
        colTransferencia.setCellValueFactory(new PropertyValueFactory<>("totalTransferencias"));
        colTotalVentas.setCellValueFactory(new PropertyValueFactory<>("totalVentas"));
        colSaldoFinal.setCellValueFactory(new PropertyValueFactory<>("saldoFinal"));
        colCantVentas.setCellValueFactory(new PropertyValueFactory<>("cantidadVentas"));

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
}