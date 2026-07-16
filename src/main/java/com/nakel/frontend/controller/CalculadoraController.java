package com.nakel.frontend.controller;

import com.nakel.frontend.model.DetalleCalculadora;
import com.nakel.frontend.model.Insumo;
import com.nakel.frontend.model.Categoria;
import com.nakel.frontend.service.InsumoApiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;

public class CalculadoraController {

    @FXML private ComboBox<Insumo> cmbInsumo;
    @FXML private TextField txtAncho;
    @FXML private TextField txtLargo;
    @FXML private TextField txtCantidad;

    @FXML private TableView<DetalleCalculadora> tablaReceta;
    @FXML private TableColumn<DetalleCalculadora, String> colInsumo;
    @FXML private TableColumn<DetalleCalculadora, String> colUso;
    @FXML private TableColumn<DetalleCalculadora, String> colSubtotal;

    @FXML private Label lblTotalCosto;
    @FXML private TextField txtMargen;
    @FXML private Label lblPrecioFinal;

    private ObservableList<DetalleCalculadora> listaReceta = FXCollections.observableArrayList();
    private double costoTotalReceta = 0.0;
//
    private final InsumoApiService insumoApi = new InsumoApiService();

    @FXML
    public void initialize() {
        // 1. Configurar la tabla
        colInsumo.setCellValueFactory(new PropertyValueFactory<>("nombreInsumo"));
        colUso.setCellValueFactory(new PropertyValueFactory<>("descripcionUso"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotalFormateado"));
        tablaReceta.setItems(listaReceta);
        tablaReceta.setPlaceholder(new Label("Agregue telas, avíos o mano de obra a la receta."));

        // 2. Simular carga de insumos (A futuro, acá llamás a InsumoApiService)
        //simularCargaInsumos();
        cargarInsumosDesdeBD();


        // 3. Escuchar la selección del ComboBox para bloquear/desbloquear cajitas
        cmbInsumo.getSelectionModel().selectedItemProperty().addListener((obs, viejo, nuevo) -> prepararCampos(nuevo));

        // 4. Escuchar cuando escriben el Margen de Ganancia para recalcular el precio final
        txtMargen.textProperty().addListener((obs, viejo, nuevo) -> calcularPrecioSugerido());
    }

    private void cargarInsumosDesdeBD() {

        try {

            cmbInsumo.getItems().clear();

            cmbInsumo.getItems().addAll(
                    insumoApi.obtenerListaInsumos()
            );

        } catch (Exception e) {

            e.printStackTrace();
            mostrarError("No se pudieron cargar los insumos.");

        }
    }

    private void prepararCampos(Insumo insumo) {
        if (insumo == null || insumo.getCategoria() == null) return;

        txtAncho.clear(); txtLargo.clear(); txtCantidad.clear();

        if ("SUPERFICIE".equals(insumo.getCategoria().getTipoMedicion())) {
            txtAncho.setDisable(false);
            txtLargo.setDisable(false);
            txtCantidad.setDisable(true);
        } else {
            txtAncho.setDisable(true);
            txtLargo.setDisable(true);
            txtCantidad.setDisable(false);
        }
    }

    @FXML
    public void agregarInsumoAReceta(ActionEvent event) {
        Insumo insumo = cmbInsumo.getValue();
        if (insumo == null) {
            mostrarError("Seleccione un insumo primero.");
            return;
        }

        try {
            double subtotal = 0.0;
            DetalleCalculadora detalle = null;

            if ("SUPERFICIE".equals(insumo.getCategoria().getTipoMedicion())) {
                int ancho = Integer.parseInt(txtAncho.getText());
                int largo = Integer.parseInt(txtLargo.getText());
                int cm2Usados = ancho * largo;

                // Usamos la matemática que agregaste en el frontend
                subtotal = insumo.getCostoPorCm2().doubleValue() * cm2Usados;
                detalle = new DetalleCalculadora(insumo, cm2Usados, "Superficie", subtotal, ancho, largo);

            } else {
                double cantidad = Double.parseDouble(txtCantidad.getText());
                subtotal = insumo.getCostoPorUnidad().doubleValue() * cantidad;
                detalle = new DetalleCalculadora(insumo, cantidad, "Unidad", subtotal, null, null);
            }

            listaReceta.add(detalle);
            recalcularCostoTotal();

            // Limpiamos la selección
            txtAncho.clear(); txtLargo.clear(); txtCantidad.clear();

        } catch (NumberFormatException e) {
            mostrarError("Llene correctamente los campos numéricos.");
        }
    }

    @FXML
    public void eliminarFila(ActionEvent event) {
        DetalleCalculadora seleccionado = tablaReceta.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            listaReceta.remove(seleccionado);
            recalcularCostoTotal();
        }
    }

    private void recalcularCostoTotal() {
        costoTotalReceta = 0.0;
        for (DetalleCalculadora det : listaReceta) {
            costoTotalReceta += det.getSubtotal();
        }
        lblTotalCosto.setText(String.format("$ %.2f", costoTotalReceta));
        calcularPrecioSugerido(); // Actualiza el precio final también
    }

    private void calcularPrecioSugerido() {
        try {
            if (txtMargen.getText() != null && !txtMargen.getText().isBlank()) {
                double margen = Double.parseDouble(txtMargen.getText());
                double gananciaPlata = costoTotalReceta * (margen / 100);
                double precioSugerido = costoTotalReceta + gananciaPlata;
                lblPrecioFinal.setText(String.format("$ %.2f", precioSugerido));
            } else {
                lblPrecioFinal.setText(String.format("$ %.2f", costoTotalReceta));
            }
        } catch (NumberFormatException e) {
            lblPrecioFinal.setText("$ ---"); // Si pone letras, mostramos rayitas
        }
    }

    @FXML
    public void guardarPresupuesto(ActionEvent event) {
        System.out.println("Costo Base: " + costoTotalReceta + " | Guardando Receta en BD...");
        // Acá podrías abrir un modal para ponerle nombre "Cartera Modelo X" y guardarlo como Articulo nuevo.
    }

    private void mostrarError(String msj) {
        Alert a = new Alert(Alert.AlertType.WARNING, msj);
        a.setHeaderText(null);
        a.showAndWait();
    }
}