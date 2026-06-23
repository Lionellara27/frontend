package com.nakel.frontend.controller;

import com.nakel.frontend.model.AjustesDivisa;
import com.nakel.frontend.service.ConfiguracionApiService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.Optional;

public class TabDivisasController {

    @FXML private TextField txtCotizacionDolar;
    @FXML private CheckBox chkActualizacionAutomatica;
    @FXML private ComboBox<String> cmbCategoriaAumento;
    @FXML private TextField txtPorcentajeAumento;
    @FXML private Button btnGuardarCotizacion;
    @FXML private Button btnAplicarAumento;
    @FXML private Button btnSincronizarDolar;

    private final ConfiguracionApiService apiService = new ConfiguracionApiService();

    @FXML
    public void initialize() {
        System.out.println("Pestaña Divisas Iniciada.");
        cmbCategoriaAumento.getItems().addAll("Todas las categorías", "Billeteras", "Mates", "Lámparas");
        cmbCategoriaAumento.setValue("Todas las categorías");

        AjustesDivisa ajustes = apiService.obtenerAjustesDivisa();
        txtCotizacionDolar.setText(String.valueOf(ajustes.getCotizacionUsd()));
        chkActualizacionAutomatica.setSelected(ajustes.isModoAutomatico());

        actualizarEstadoCampoCotizacion();
    }

    @FXML
    public void toggleModoAutomatico(ActionEvent event) {
        actualizarEstadoCampoCotizacion();
    }

    private void actualizarEstadoCampoCotizacion() {
        txtCotizacionDolar.setDisable(chkActualizacionAutomatica.isSelected());
    }

    @FXML
    public void guardarCotizacion(ActionEvent event) {
        try {
            Double valor = Double.parseDouble(txtCotizacionDolar.getText());
            boolean exito = apiService.guardarAjustesDivisa(new AjustesDivisa(valor, chkActualizacionAutomatica.isSelected()));

            if (exito) {
                new Alert(Alert.AlertType.INFORMATION, "Ajustes de divisa guardados correctamente.").showAndWait();
            } else {
                new Alert(Alert.AlertType.ERROR, "No se pudo guardar en el servidor.").showAndWait();
            }
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Ingresá un número válido para la cotización.").showAndWait();
        }
    }

    @FXML
    public void aplicarAumentoMasivo(ActionEvent event) {
        String categoria = cmbCategoriaAumento.getValue();
        String porcentaje = txtPorcentajeAumento.getText();

        if (porcentaje.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Por favor, ingresá un porcentaje.").showAndWait();
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Aumento Masivo");
        confirmacion.setHeaderText("Vas a aumentar: " + categoria);
        confirmacion.setContentText("¿Estás seguro de incrementar un " + porcentaje + "%?");

        Optional<ButtonType> result = confirmacion.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.out.println("🚀 Aumentando " + porcentaje + "% a " + categoria);
        }
    }

    @FXML
    public void sincronizarPreciosConDolar(ActionEvent event) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Sincronizar con Dólar");
        confirmacion.setHeaderText("Actualización por desfasaje");
        confirmacion.setContentText("¿Recalcular precios basándose en $" + txtCotizacionDolar.getText() + "?");

        Optional<ButtonType> result = confirmacion.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.out.println("🔄 Sincronizando con cotización: " + txtCotizacionDolar.getText());
        }
    }
}