package com.nakel.frontend.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;

public class MainController {

    // 1. Enlazamos el espacio central que dejamos preparado en el FXML
    @FXML
    private StackPane areaContenido;

    // 2. Este método se ejecuta solo apenas arranca la pantalla
    @FXML
    public void initialize() {
        mostrarBienvenida();
    }

    // 3. El método que dispara el botón del Mostrador
    @FXML
    public void mostrarMostrador(ActionEvent event) {
        // RUTA A TU FXML (Ajustala según tu carpeta resources)
        cargarVista("/com/nakel/frontend/view/venta-view.fxml");
    }

    // 4. El método que dispara el botón de Clientes
    @FXML
    public void mostrarClientes(ActionEvent event) {
        // RUTA A TU FXML (Ajustala según tu carpeta resources)
        cargarVista("/com/nakel/frontend/view/cliente-view.fxml");
    }

    @FXML
    public void mostrarInsumos(ActionEvent event) {
        cargarVista("/com/nakel/frontend/view/insumo-view.fxml"); // Ajustá la ruta
    }

    @FXML
    public void mostrarCalculadora(ActionEvent event) {
        cargarVista("/com/nakel/frontend/view/calcular-produccion-view.fxml");
    }

    // ==========================================================
    // MOTOR DE CAMBIO DE PANTALLAS
    // ==========================================================
    private void cargarVista(String rutaFxml) {
        try {
            // Buscamos el archivo FXML
            URL archivoUrl = getClass().getResource(rutaFxml);

            if (archivoUrl == null) {
                System.err.println("¡Error! No se encontró el archivo FXML en la ruta: " + rutaFxml);
                return;
            }

            // Cargamos la vista y la inyectamos en el centro
            Pane nuevaVista = FXMLLoader.load(archivoUrl);
            areaContenido.getChildren().clear();
            areaContenido.getChildren().add(nuevaVista);

        } catch (IOException e) {
            System.err.println("¡Ups! Hubo un problema al cargar la pantalla.");
            e.printStackTrace();
        }
    }

    // ==========================================================
    // MENSAJE DE INICIO (Por defecto)
    // ==========================================================
    private void mostrarBienvenida() {
        VBox bienvenida = new VBox(15);
        bienvenida.setAlignment(Pos.CENTER);

        Label titulo = new Label("Panel Principal");
        titulo.getStyleClass().add("welcome-title"); // Usa tu CSS

        Label subtitulo = new Label("Bienvenida a Nakel ERP");
        subtitulo.getStyleClass().add("welcome-subtitle"); // Usa tu CSS

        bienvenida.getChildren().addAll(titulo, subtitulo);

        if(areaContenido != null) {
            areaContenido.getChildren().clear();
            areaContenido.getChildren().add(bienvenida);
        }
    }
}