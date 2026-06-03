package com.nakel.frontend.controller;

import com.nakel.frontend.util.Navegador; // ¡Importamos tu Router!
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainController {

    // Enlazamos el espacio central (StackPane)
    @FXML
    private StackPane areaContenido;

    @FXML
    public void initialize() {
        // 1. Le entregamos el panel central al Router para que tome el control
        Navegador.setPanelCentral(this.areaContenido);

        // 2. Mostramos el mensaje inicial
        mostrarBienvenida();
    }

    // ==========================================================
    // NAVEGACIÓN LIMPIA (Usando el Router)
    // ==========================================================

    @FXML
    public void mostrarPuntoDeVenta(ActionEvent event) {
        Navegador.cargarVista("/com/nakel/frontend/view/venta-view.fxml");
    }

    @FXML
    public void mostrarClientes(ActionEvent event) {
        Navegador.cargarVista("/com/nakel/frontend/view/cliente-view.fxml");
    }

    @FXML
    public void mostrarInsumos(ActionEvent event) {
        Navegador.cargarVista("/com/nakel/frontend/view/insumo-view.fxml");
    }

    @FXML
    public void mostrarCalculadora(ActionEvent event) {
        Navegador.cargarVista("/com/nakel/frontend/view/calcular-produccion-view.fxml");
    }

    @FXML
    public void mostrarProveedores(ActionEvent event) {
        Navegador.cargarVista("/com/nakel/frontend/view/proveedor-view.fxml");
    }

    @FXML
    public void mostrarHistorialVentas(ActionEvent event) {
        Navegador.cargarVista("/com/nakel/frontend/view/historial-ventas-view.fxml");
    }

    @FXML
    public void mostrarCatalogo(ActionEvent event) {
        System.out.println("Abriendo Catálogo de Artículos...");
        Navegador.cargarVista("/com/nakel/frontend/view/articulo-view.fxml");
    }

    // ==========================================================
    // MENSAJE DE INICIO (Por defecto)
    // ==========================================================
    private void mostrarBienvenida() {
        VBox bienvenida = new VBox(15);
        bienvenida.setAlignment(Pos.CENTER);

        Label titulo = new Label("Panel Principal");
        titulo.getStyleClass().add("welcome-title");

        Label subtitulo = new Label("Bienvenida a Nakel ERP");
        subtitulo.getStyleClass().add("welcome-subtitle");

        bienvenida.getChildren().addAll(titulo, subtitulo);

        if(areaContenido != null) {
            areaContenido.getChildren().clear();
            areaContenido.getChildren().add(bienvenida);
        }
    }
}