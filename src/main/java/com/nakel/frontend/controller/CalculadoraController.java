package com.nakel.frontend.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;

public class CalculadoraController {

    // ==========================================
    // 1. ENLACES CON EL FXML
    // ==========================================
    @FXML private ComboBox<String> cmbInsumo;
    @FXML private VBox contenedorCortes; // El lienzo vacío para inyectar cortes
    @FXML private TextField txtHoras;
    @FXML private TextField txtValorHora;
    @FXML private TextField txtCostoAvios;
    @FXML private Label lblTotalCosto;

    // ==========================================
    // 2. VARIABLES PARA LA MEMORIA DINÁMICA
    // ==========================================
    private final List<TextField> listaTxtLargos = new ArrayList<>();
    private final List<TextField> listaTxtAnchos = new ArrayList<>();

    // Simulación de costo por cm² (En Fase 2 esto viene de la Base de Datos)
    private final double COSTO_CM2_CUERO = 0.50;

    // ==========================================
    // 3. INICIALIZACIÓN
    // ==========================================
    @FXML
    public void initialize() {
        System.out.println("¡Calculadora Dinámica lista para operar!");

        if (cmbInsumo != null) {
            cmbInsumo.getItems().addAll("Cuero Vacuno Texturado", "Lona Premium", "Forrería Microfibra");
        }

        // Arrancamos con un corte por defecto
        agregarFilaCorte();

        // Escuchamos si tipea en las horas o avíos para recalcular al instante
        configurarEscuchadores();
    }

    // ==========================================
    // 4. LÓGICA DE CORTES INFINITOS
    // ==========================================
    @FXML
    public void agregarFilaCorte() {
        int numeroCorte = listaTxtLargos.size() + 1;

        // Creamos las cajitas de texto desde el código
        Label lblNombre = new Label("Corte " + numeroCorte + ":");
        lblNombre.setStyle("-fx-text-fill: white; -fx-pref-width: 70;");

        TextField txtLargo = new TextField();
        txtLargo.setPromptText("Largo (cm)");
        txtLargo.setPrefWidth(100);
        txtLargo.getStyleClass().add("text-field");

        Label lblX = new Label("x");
        lblX.setStyle("-fx-text-fill: #D4AF37; -fx-font-weight: bold;");

        TextField txtAncho = new TextField();
        txtAncho.setPromptText("Ancho (cm)");
        txtAncho.setPrefWidth(100);
        txtAncho.getStyleClass().add("text-field");

        Label lblCm = new Label("cm");
        lblCm.setStyle("-fx-text-fill: #cccccc;");

        // Hacemos que avisen si alguien escribe números ahí
        txtLargo.textProperty().addListener((obs, oldVal, newVal) -> calcularTotal());
        txtAncho.textProperty().addListener((obs, oldVal, newVal) -> calcularTotal());

        // Guardamos las cajas en nuestra lista
        listaTxtLargos.add(txtLargo);
        listaTxtAnchos.add(txtAncho);

        // Armamos la fila horizontal y la metemos en la pantalla
        HBox filaCorte = new HBox(10);
        filaCorte.setAlignment(Pos.CENTER_LEFT);
        filaCorte.getChildren().addAll(lblNombre, txtLargo, lblX, txtAncho, lblCm);

        contenedorCortes.getChildren().add(filaCorte);
    }

    // ==========================================
    // 5. MOTOR MATEMÁTICO EN TIEMPO REAL
    // ==========================================
    private void calcularTotal() {
        double totalMaterialArea = 0;

        // Sumamos todos los pedacitos de cuero que agregó
        for (int i = 0; i < listaTxtLargos.size(); i++) {
            try {
                String largoStr = listaTxtLargos.get(i).getText();
                String anchoStr = listaTxtAnchos.get(i).getText();

                if (!largoStr.isEmpty() && !anchoStr.isEmpty()) {
                    double largo = Double.parseDouble(largoStr);
                    double ancho = Double.parseDouble(anchoStr);
                    totalMaterialArea += (largo * ancho);
                }
            } catch (NumberFormatException e) {
                // Ignoramos si tipeó letras
            }
        }

        double costoMaterial = totalMaterialArea * COSTO_CM2_CUERO;
        double costoManoObra = 0;
        double costoAvios = 0;

        try {
            if (txtHoras != null && !txtHoras.getText().isEmpty() && txtValorHora != null && !txtValorHora.getText().isEmpty()) {
                costoManoObra = Double.parseDouble(txtHoras.getText()) * Double.parseDouble(txtValorHora.getText());
            }
            if (txtCostoAvios != null && !txtCostoAvios.getText().isEmpty()) {
                costoAvios = Double.parseDouble(txtCostoAvios.getText());
            }
        } catch (NumberFormatException e) {
            // Ignoramos errores de tipeo
        }

        double costoTotalGral = costoMaterial + costoManoObra + costoAvios;

        if (lblTotalCosto != null) {
            lblTotalCosto.setText(String.format("$ %.2f", costoTotalGral));
        }
    }

    private void configurarEscuchadores() {
        if (txtHoras != null) txtHoras.textProperty().addListener((o, old, n) -> calcularTotal());
        if (txtValorHora != null) txtValorHora.textProperty().addListener((o, old, n) -> calcularTotal());
        if (txtCostoAvios != null) txtCostoAvios.textProperty().addListener((o, old, n) -> calcularTotal());
    }

    // ==========================================
    // 6. GUARDAR (Acción del botón final)
    // ==========================================
    @FXML
    public void guardarPresupuesto(ActionEvent event) {
        System.out.println("Convirtiendo presupuesto en nuevo Artículo...");
        // Acá a futuro abrimos un popup para pedirle el nombre de la cartera y guardarlo en la Base de Datos
    }
}