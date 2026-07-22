package com.nakel.frontend.controller;

import com.nakel.frontend.model.Categoria;
import com.nakel.frontend.model.Insumo;
import com.nakel.frontend.model.Material; // 🔥 NUEVO
import com.nakel.frontend.service.CategoriaApiService;
import com.nakel.frontend.service.InsumoApiService;
import com.nakel.frontend.service.ParametrosApiService; // 🔥 NUEVO (Para traer los materiales)
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.util.StringConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class NuevoInsumoController {

    private final CategoriaApiService categoriaService = new CategoriaApiService();
    // Reutilizamos tu servicio de parámetros para traer la lista de Materiales
    private final ParametrosApiService parametrosService = new ParametrosApiService();
    private final InsumoApiService insumoApi = new InsumoApiService();
    private final Gson gson = new Gson();

    private Insumo insumoEnEdicion = null;

    @FXML private TextField txtCantidad;
    @FXML private Label lblCantidad;

    @FXML private ComboBox<Categoria> cmbCategoria;

    // 🔥 NUEVO: Campos para el Material
    @FXML private ComboBox<Material> cmbMaterial;
    @FXML private Label lblMaterial;
    @FXML private HBox boxMaterial; // Si metés el combo y el botón + en un HBox

    @FXML private TextField txtDescripcion;
    @FXML private Label lblMedidas;
    @FXML private HBox boxMedidas;
    @FXML private TextField txtAnchoPlancha;
    @FXML private TextField txtLargoPlancha;
    @FXML private ComboBox<String> cmbUnidad;
    @FXML private TextField txtCosto;

    @FXML
    public void initialize() {
        cmbUnidad.getItems().addAll("Metros", "Unidades", "Horas");

        // 1. Convertidor para Categorías
        cmbCategoria.setConverter(new StringConverter<Categoria>() {
            @Override
            public String toString(Categoria cat) { return cat != null ? cat.getNombre() : ""; }
            @Override
            public Categoria fromString(String string) { return null; }
        });

        // 🔥 NUEVO: Convertidor para Materiales
        if (cmbMaterial != null) {
            cmbMaterial.setConverter(new StringConverter<Material>() {
                @Override
                public String toString(Material mat) { return mat != null ? mat.getNombre() : ""; }
                @Override
                public Material fromString(String string) { return null; }
            });
        }

        // 2. Cargamos ambas listas desde la Base de Datos
        cargarCategoriasDesdeBD();
        cargarMaterialesDesdeBD(); // 🔥 NUEVO

        // 3. EL LISTENER INTELIGENTE
        cmbCategoria.valueProperty().addListener((observable, valorViejo, valorNuevo) -> {
            if (valorNuevo != null) {
                activarMagiaDinamica(valorNuevo);
            }
        });
    }

    private void cargarCategoriasDesdeBD() {
        cmbCategoria.getItems().clear();
        try {
            String jsonCategorias = categoriaService.obtenerCategorias();
            List<Categoria> categoriasDB = gson.fromJson(jsonCategorias, new TypeToken<ArrayList<Categoria>>() {}.getType());
            if (categoriasDB != null) cmbCategoria.getItems().addAll(categoriasDB);
        } catch (Exception e) {
            System.err.println("Error al cargar categorías: " + e.getMessage());
        }
    }

    // 🔥 NUEVO: Función para traer los materiales al ComboBox
    private void cargarMaterialesDesdeBD() {
        if (cmbMaterial == null) return; // Por si todavía no lo pusiste en el FXML
        cmbMaterial.getItems().clear();
        try {
            List<Material> materialesDB = parametrosService.obtenerMateriales();
            if (materialesDB != null) {
                cmbMaterial.getItems().addAll(materialesDB);
            }
        } catch (Exception e) {
            System.err.println("Error al cargar materiales: " + e.getMessage());
        }
    }

    private void activarMagiaDinamica(Categoria categoriaSeleccionada) {
        // Reseteo general
        lblMedidas.setVisible(false); lblMedidas.setManaged(false);
        boxMedidas.setVisible(false); boxMedidas.setManaged(false);
        lblCantidad.setVisible(false); lblCantidad.setManaged(false);
        txtCantidad.setVisible(false); txtCantidad.setManaged(false);

        // Ocultamos Material por defecto
        if (lblMaterial != null) { lblMaterial.setVisible(false); lblMaterial.setManaged(false); }
        if (boxMaterial != null) { boxMaterial.setVisible(false); boxMaterial.setManaged(false); }
        if (cmbMaterial != null) { cmbMaterial.setVisible(false); cmbMaterial.setManaged(false); }

        txtDescripcion.setDisable(false);

        // ✅ Solo limpiamos la descripción si es un ALTA NUEVA
        if (insumoEnEdicion == null) {
            txtDescripcion.clear();
        }

        String regla = categoriaSeleccionada.getTipoMedicion();

        if ("SUPERFICIE".equals(regla)) {
            lblMedidas.setVisible(true); lblMedidas.setManaged(true);
            boxMedidas.setVisible(true); boxMedidas.setManaged(true);
            cmbUnidad.setValue("Metros");

            if (lblMaterial != null) { lblMaterial.setVisible(true); lblMaterial.setManaged(true); }
            if (boxMaterial != null) { boxMaterial.setVisible(true); boxMaterial.setManaged(true); }
            if (cmbMaterial != null) { cmbMaterial.setVisible(true); cmbMaterial.setManaged(true); }

        } else if ("TIEMPO".equals(regla)) {
            txtDescripcion.setText("Valor Hora Taller / Confección");
            txtDescripcion.setDisable(true);
            cmbUnidad.setValue("Horas");

        } else if ("UNIDAD".equals(regla)) {
            lblCantidad.setVisible(true); lblCantidad.setManaged(true);
            txtCantidad.setVisible(true); txtCantidad.setManaged(true);
            cmbUnidad.setValue("Unidades");
        }
    }

    @FXML
    public void abrirModalNuevaCategoria(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/nueva-categoria-modal.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage modalStage = new javafx.stage.Stage();
            modalStage.setTitle("Nueva Categoría de Insumo");
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.setResizable(false);
            modalStage.showAndWait();

            cargarCategoriasDesdeBD();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔥 NUEVO: Botón + del Material
    @FXML
    public void abrirModalNuevoMaterial(ActionEvent event) {
        try {
            // Nota: Podés reutilizar la pestaña de parámetros o crear un mini modal para Material acá
            // Por ahora llamamos a un modal ficticio de nuevo-material, si no lo tenés, lo armamos!
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/nuevo-material-modal.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage modalStage = new javafx.stage.Stage();
            modalStage.setTitle("Nuevo Material");
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.setResizable(false);
            modalStage.showAndWait();

            cargarMaterialesDesdeBD(); // Recarga los combos!
        } catch (Exception e) {
            System.err.println("❌ No se encontró la vista del modal de material.");
            e.printStackTrace();
        }
    }

    @FXML
    public void guardarInsumo(ActionEvent event) {
        if (cmbCategoria.getValue() == null) {
            mostrarAlerta("Atención", "Debe seleccionar una categoría válida antes de guardar.", Alert.AlertType.WARNING);
            return;
        }

        try {
            Insumo insumoAGuardar = (insumoEnEdicion != null) ? insumoEnEdicion : new Insumo();
            insumoAGuardar.setCategoria(cmbCategoria.getValue());
            insumoAGuardar.setNombre(txtDescripcion.getText());

            if (txtCosto.getText() != null && !txtCosto.getText().trim().isEmpty()) {
                insumoAGuardar.setCostoTotal(new java.math.BigDecimal(txtCosto.getText().trim()));
            }

            String tipoMedicion = cmbCategoria.getValue().getTipoMedicion();

            if ("SUPERFICIE".equals(tipoMedicion)) {
                String ancho = (txtAnchoPlancha.getText() != null && !txtAnchoPlancha.getText().isEmpty()) ? txtAnchoPlancha.getText() : "0";
                String largo = (txtLargoPlancha.getText() != null && !txtLargoPlancha.getText().isEmpty()) ? txtLargoPlancha.getText() : "0";

                insumoAGuardar.setAnchoLoteCm(Integer.parseInt(ancho));
                insumoAGuardar.setLargoLoteCm(Integer.parseInt(largo));

                if (insumoAGuardar.getAreaActualCm2() == null) {
                    insumoAGuardar.setAreaActualCm2(Integer.parseInt(ancho) * Integer.parseInt(largo));
                }

                // 🔥 Guardamos el Material Seleccionado
                if (cmbMaterial != null && cmbMaterial.getValue() != null) {
                    insumoAGuardar.setMaterial(cmbMaterial.getValue());
                }

            } else if ("UNIDAD".equals(tipoMedicion)) {
                String cantStr = (txtCantidad.getText() != null) ? txtCantidad.getText().trim() : "1";
                int cant = cantStr.isEmpty() ? 1 : Integer.parseInt(cantStr);

                insumoAGuardar.setCantidadLote(cant);
                insumoAGuardar.setCantidadActual(cant);

                // 🔥 Como es UNIDAD, forzamos a nulo el material por las dudas
                insumoAGuardar.setMaterial(null);
            }

            insumoApi.guardarInsumoEnBaseDeDatos(insumoAGuardar);
            mostrarAlerta("¡Excelente!", "El insumo se guardó correctamente en la base de datos.", Alert.AlertType.INFORMATION);
            cerrarModal(event);

        } catch (Exception e) {
            mostrarAlerta("Error del sistema", "No se pudo guardar el insumo:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void cargarDatosParaEditar(Insumo insumo) {
        this.insumoEnEdicion = insumo;
        txtDescripcion.setText(insumo.getNombre());

        if (insumo.getCostoTotal() != null) txtCosto.setText(insumo.getCostoTotal().toString());

        if (insumo.getCategoria() != null) {
            for (Categoria cat : cmbCategoria.getItems()) {
                if (cat.getId() != null && cat.getId().equals(insumo.getCategoria().getId())) {
                    cmbCategoria.setValue(cat);
                    txtDescripcion.setText(insumo.getNombre()); //agregado
                    break;
                }
            }
        }

        // 🔥 Si tiene Material, se lo seleccionamos
        if (insumo.getMaterial() != null && cmbMaterial != null) {
            for (Material mat : cmbMaterial.getItems()) {
                if (mat.getId() != null && mat.getId().equals(insumo.getMaterial().getId())) {
                    cmbMaterial.setValue(mat);
                    break;
                }
            }
        }

        if (insumo.getAnchoLoteCm() != null && insumo.getAnchoLoteCm() > 0) txtAnchoPlancha.setText(insumo.getAnchoLoteCm().toString());
        if (insumo.getLargoLoteCm() != null && insumo.getLargoLoteCm() > 0) txtLargoPlancha.setText(insumo.getLargoLoteCm().toString());
        if (insumo.getCantidadLote() != null && insumo.getCantidadLote() > 0) txtCantidad.setText(insumo.getCantidadLote().toString());
    }

    @FXML
    public void cerrarModal(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}