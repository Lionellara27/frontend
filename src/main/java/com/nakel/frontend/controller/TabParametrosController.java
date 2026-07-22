package com.nakel.frontend.controller;

import com.nakel.frontend.model.Categoria;
import com.nakel.frontend.model.Material;
import com.nakel.frontend.service.ParametrosApiService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.List;

public class TabParametrosController {

    private ParametrosApiService apiService;

    // --- CAMPOS CATEGORÍA ---
    @FXML private ListView<Categoria> listCategorias;
    @FXML private TextField txtNuevaCategoria;
    @FXML private TextField txtNuevoPrefijo;

    // --- CAMPOS MATERIAL ---
    @FXML private ListView<Material> listMateriales;
    @FXML private TextField txtNuevoMaterial;

    @FXML
    public void initialize() {
        System.out.println("Pestaña Parámetros Iniciada. Conectando a SQLite...");
        apiService = new ParametrosApiService();
        cargarDatosDesdeBD();
    }

    private void cargarDatosDesdeBD() {
        listCategorias.getItems().clear();
        listMateriales.getItems().clear();

        List<Categoria> categoriasBD = apiService.obtenerCategorias();
        if (categoriasBD != null) listCategorias.getItems().addAll(categoriasBD);

        List<Material> materialesBD = apiService.obtenerMateriales();
        if (materialesBD != null) listMateriales.getItems().addAll(materialesBD);
    }

    @FXML
    public void agregarCategoria(ActionEvent event) {
        System.out.println("--- INTENTANDO GUARDAR CATEGORÍA ---");

        if (txtNuevaCategoria == null || txtNuevoPrefijo == null) {
            System.out.println("❌ ERROR: Las cajas de texto no están conectadas al FXML.");
            return;
        }

        String nombre = txtNuevaCategoria.getText().trim();
        String prefijo = txtNuevoPrefijo.getText().trim();

        System.out.println("📌 Datos -> Nombre: [" + nombre + "] | Prefijo: [" + prefijo + "]");

        if (nombre.isEmpty() || prefijo.isEmpty()) {
            System.out.println("⚠️ Faltan datos. Cancelando guardado.");
            return;
        }

        Categoria nuevaCat = new Categoria();
        nuevaCat.setNombre(nombre);
        nuevaCat.setPrefijoSku(prefijo);

        System.out.println("🚀 Enviando al Backend...");
        Categoria guardada = apiService.guardarCategoria(nuevaCat);

        if (guardada != null) {
            System.out.println("✅ ¡Guardado exitoso! ID: " + guardada.getId());
            listCategorias.getItems().add(guardada);
            txtNuevaCategoria.clear();
            txtNuevoPrefijo.clear();
        } else {
            System.out.println("❌ ERROR: El Backend devolvió null.");
        }
    }

    @FXML
    public void eliminarCategoria(ActionEvent event) {
        Categoria seleccion = listCategorias.getSelectionModel().getSelectedItem();
        if (seleccion != null) {
            try {
                apiService.eliminarCategoria(seleccion.getId());
                listCategorias.getItems().remove(seleccion);
            } catch (Exception e) {
                System.out.println("Error al eliminar: " + e.getMessage());
            }
        }
    }

    @FXML
    public void agregarMaterial(ActionEvent event) {
        String nuevo = txtNuevoMaterial.getText().trim();
        if (nuevo.isEmpty()) return;

        Material nuevoMat = new Material();
        nuevoMat.setNombre(nuevo);

        Material guardado = apiService.guardarMaterial(nuevoMat);
        if (guardado != null) {
            listMateriales.getItems().add(guardado);
            txtNuevoMaterial.clear();
        }
    }

    @FXML
    public void eliminarMaterial(ActionEvent event) {
        Material seleccion = listMateriales.getSelectionModel().getSelectedItem();
        if (seleccion != null) {
            try {
                apiService.eliminarMaterial(seleccion.getId());
                listMateriales.getItems().remove(seleccion);
            } catch (Exception e) {
                System.out.println("Error al eliminar: " + e.getMessage());
            }
        }
    }
}