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

        // Cada categoría se muestra con su prefijo, que es con lo que arrancan sus códigos
        listCategorias.setCellFactory(lista -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Categoria categoria, boolean empty) {
                super.updateItem(categoria, empty);
                if (empty || categoria == null) {
                    setText(null);
                } else {
                    String prefijo = categoria.getPrefijoSku();
                    setText(categoria.getNombre() + (prefijo != null && !prefijo.isBlank() ? "   ·   prefijo " + prefijo : ""));
                }
            }
        });
        listCategorias.setStyle("-fx-font-size: 14px;");
        listMateriales.setStyle("-fx-font-size: 14px;");
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
            // Antes no avisaba nada: se tocaba "Agregar" y parecía que el botón no andaba
            avisar(javafx.scene.control.Alert.AlertType.INFORMATION, "Faltan datos",
                    "Escribí el nombre de la categoría y su prefijo (por ejemplo: Billetera y 1004).");
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
            avisar(javafx.scene.control.Alert.AlertType.ERROR, "No se pudo guardar la categoría", apiService.getUltimoError());
        }
    }

    private void avisar(javafx.scene.control.Alert.AlertType tipo, String titulo, String mensaje) {
        javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje != null ? mensaje : "Ocurrió un error.");
        alerta.showAndWait();
    }

    // Antes borraba con un solo clic y sin preguntar. Ahora pide confirmación.
    private boolean confirmarBorrado(String queCosa) {
        javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION,
                "¿Seguro que querés eliminar " + queCosa + "?",
                javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
        alerta.setTitle("Confirmar eliminación");
        alerta.setHeaderText(null);
        return alerta.showAndWait().orElse(javafx.scene.control.ButtonType.NO) == javafx.scene.control.ButtonType.YES;
    }

    @FXML
    public void eliminarCategoria(ActionEvent event) {
        Categoria seleccion = listCategorias.getSelectionModel().getSelectedItem();
        if (seleccion == null) {
            avisar(javafx.scene.control.Alert.AlertType.INFORMATION, "Eliminar categoría", "Primero elegí una categoría de la lista.");
            return;
        }
        if (!confirmarBorrado("la categoría \"" + seleccion.getNombre() + "\"")) return;

        try {
            apiService.eliminarCategoria(seleccion.getId());
            listCategorias.getItems().remove(seleccion);
        } catch (Exception e) {
            // El backend no deja borrar una categoría con artículos y explica por qué
            avisar(javafx.scene.control.Alert.AlertType.ERROR, "No se pudo eliminar", e.getMessage());
        }
    }

    @FXML
    public void agregarMaterial(ActionEvent event) {
        String nuevo = txtNuevoMaterial.getText().trim();
        if (nuevo.isEmpty()) {
            avisar(javafx.scene.control.Alert.AlertType.INFORMATION, "Falta el nombre", "Escribí el nombre del material (por ejemplo: Cuero).");
            return;
        }

        Material nuevoMat = new Material();
        nuevoMat.setNombre(nuevo);

        Material guardado = apiService.guardarMaterial(nuevoMat);
        if (guardado != null) {
            listMateriales.getItems().add(guardado);
            txtNuevoMaterial.clear();
        } else {
            avisar(javafx.scene.control.Alert.AlertType.ERROR, "No se pudo guardar el material", apiService.getUltimoError());
        }
    }

    @FXML
    public void eliminarMaterial(ActionEvent event) {
        Material seleccion = listMateriales.getSelectionModel().getSelectedItem();
        if (seleccion == null) {
            avisar(javafx.scene.control.Alert.AlertType.INFORMATION, "Eliminar material", "Primero elegí un material de la lista.");
            return;
        }
        if (!confirmarBorrado("el material \"" + seleccion.getNombre() + "\"")) return;

        try {
            apiService.eliminarMaterial(seleccion.getId());
            listMateriales.getItems().remove(seleccion);
        } catch (Exception e) {
            avisar(javafx.scene.control.Alert.AlertType.ERROR, "No se pudo eliminar", e.getMessage());
        }
    }
}