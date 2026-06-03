package com.nakel.frontend.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class ArticuloController {

    // Filtros de búsqueda
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ComboBox<String> cmbMaterial;
    @FXML private ComboBox<String> cmbOrigen;

    // Tabla y Totales
    @FXML private TableView<?> tablaArticulos;
    @FXML private Label lblTotalArticulos;

    @FXML
    public void initialize() {
        System.out.println("Módulo de Catálogo Iniciado.");

        // Ajustamos la tabla para que ocupe todo el ancho
        tablaArticulos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Llenamos los combos con datos de prueba (Luego vendrán de tu API Spring Boot)
        cmbCategoria.getItems().addAll("Billeteras", "Mates", "Lámparas");
        cmbMaterial.getItems().addAll("Cuero Liso", "Cuero Peludo", "Madera", "Metal");
        cmbOrigen.getItems().addAll("PRODUCCION_PROPIA", "REVENTA");
    }

    @FXML
    public void buscarArticulos(ActionEvent event) {
        System.out.println("Buscando artículos...");
        System.out.println("Texto: " + txtBuscar.getText());
        System.out.println("Origen: " + cmbOrigen.getValue());
        // Acá a futuro llamaremos a la API: api/articulos/buscar?...
    }

    @FXML
    public void limpiarFiltros(ActionEvent event) {
        txtBuscar.clear();
        cmbCategoria.setValue(null);
        cmbMaterial.setValue(null);
        cmbOrigen.setValue(null);
        System.out.println("Filtros limpiados.");
    }

    @FXML
    public void abrirModalNuevoArticulo(ActionEvent event) {
        System.out.println("Abriendo ventana para crear un Nuevo Artículo...");
        // Acá abriremos el modal (ventana emergente) para cargar el producto,
        // donde ella podrá tipear el nombre, escanear el SKU o generarlo si es de producción propia.
    }
}