package com.nakel.frontend.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class TabParametrosController {

    @FXML private ListView<String> listCategorias;
    @FXML private TextField txtNuevaCategoria;

    @FXML private ListView<String> listMateriales;
    @FXML private TextField txtNuevoMaterial;

    @FXML
    public void initialize() {
        System.out.println("Pestaña Parámetros Iniciada.");
        // Datos falsos para ver el diseño
        listCategorias.getItems().addAll("Mates", "Billeteras", "Lámparas", "Mochilas");
        listMateriales.getItems().addAll("Cuero Liso", "Tela Avión", "Madera Algarrobo", "Aluminio");
    }

    @FXML
    public void agregarCategoria(ActionEvent event) {
        String nueva = txtNuevaCategoria.getText().trim();
        if (!nueva.isEmpty() && !listCategorias.getItems().contains(nueva)) {
            listCategorias.getItems().add(nueva);
            txtNuevaCategoria.clear();
        }
    }

    @FXML
    public void eliminarCategoria(ActionEvent event) {
        String seleccion = listCategorias.getSelectionModel().getSelectedItem();
        if (seleccion != null) listCategorias.getItems().remove(seleccion);
    }

    @FXML
    public void agregarMaterial(ActionEvent event) {
        String nuevo = txtNuevoMaterial.getText().trim();
        if (!nuevo.isEmpty() && !listMateriales.getItems().contains(nuevo)) {
            listMateriales.getItems().add(nuevo);
            txtNuevoMaterial.clear();
        }
    }

    @FXML
    public void eliminarMaterial(ActionEvent event) {
        String seleccion = listMateriales.getSelectionModel().getSelectedItem();
        if (seleccion != null) listMateriales.getItems().remove(seleccion);
    }
}