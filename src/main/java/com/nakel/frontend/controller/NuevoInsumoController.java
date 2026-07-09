package com.nakel.frontend.controller;

import com.nakel.frontend.model.Categoria; // ⚠️ Asegurate de tener esta clase creada
import com.nakel.frontend.model.Insumo;
import com.nakel.frontend.service.CategoriaApiService;
import com.nakel.frontend.service.InsumoApiService;
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
    private final Gson gson = new Gson();

    private final InsumoApiService insumoApi = new InsumoApiService();
    private Insumo insumoEnEdicion = null;
    // 1. AHORA EL COMBOBOX MANEJA OBJETOS "Categoria", NO STRINGS SUELTOS
    @FXML
    private ComboBox<Categoria> cmbCategoria;

    @FXML
    private TextField txtDescripcion;

    @FXML
    private Label lblMedidas;

    @FXML
    private HBox boxMedidas;

    @FXML
    private TextField txtAnchoPlancha;

    @FXML
    private TextField txtLargoPlancha;

    @FXML
    private ComboBox<String> cmbUnidad;

    @FXML
    private TextField txtCosto;

    @FXML
    public void initialize() {
        cmbUnidad.getItems().addAll("Metros", "Unidades", "Horas");

        // 2. Le enseñamos al ComboBox cómo mostrar los objetos en pantalla (solo el nombre)
        cmbCategoria.setConverter(new StringConverter<Categoria>() {
            @Override
            public String toString(Categoria cat) {
                return cat != null ? cat.getNombre() : "";
            }
            @Override
            public Categoria fromString(String string) {
                return null; // No lo usamos para escribir a mano
            }
        });

        // 3. Cargamos las categorías desde la Base de Datos
        cargarCategoriasDesdeBD();

        // 4. EL LISTENER INTELIGENTE LIMPIO
        cmbCategoria.valueProperty().addListener((observable, valorViejo, valorNuevo) -> {
            if (valorNuevo != null) {
                // Ya no preguntamos por el -1, directamente aplicamos la magia
                activarMagiaDinamica(valorNuevo);
            }
        });
    }

    private void cargarCategoriasDesdeBD() {
        cmbCategoria.getItems().clear();

        try {
            String jsonCategorias = categoriaService.obtenerCategorias();
            List<Categoria> categoriasDB = gson.fromJson(
                    jsonCategorias,
                    new TypeToken<ArrayList<Categoria>>() {}.getType()
            );

            if (categoriasDB != null) {
                cmbCategoria.getItems().addAll(categoriasDB);
            }
        } catch (Exception e) {
            System.err.println("Error al cargar categorías: " + e.getMessage());
        }

        // ❌ ACÁ BORRAMOS LAS 2 LÍNEAS DEL BOTÓN "CREAR NUEVA" QUE ESTABAN ANTES
    }


    private void activarMagiaDinamica(Categoria categoriaSeleccionada) {
        // Reseteo general
        lblMedidas.setVisible(false);
        lblMedidas.setManaged(false);
        boxMedidas.setVisible(false);
        boxMedidas.setManaged(false);
        txtDescripcion.setDisable(false);
        txtDescripcion.clear();

        // MAGIA ESCALABLE: Ya no importa el nombre, importa la REGLA de la categoría.
        String regla = categoriaSeleccionada.getTipoMedicion();

        if ("SUPERFICIE".equals(regla)) {
            lblMedidas.setVisible(true);
            lblMedidas.setManaged(true);
            boxMedidas.setVisible(true);
            boxMedidas.setManaged(true);
            cmbUnidad.setValue("Metros");

        } else if ("TIEMPO".equals(regla)) {
            txtDescripcion.setText("Valor Hora Taller / Confección");
            txtDescripcion.setDisable(true);
            cmbUnidad.setValue("Horas");

        } else if ("UNIDAD".equals(regla)) {
            cmbUnidad.setValue("Unidades");
        }
    }

    @FXML
    public void abrirModalNuevaCategoria(ActionEvent event) {
        try {
            // 1. Levantamos el FXML del modal de categorías (ajustá la ruta si el archivo se llama distinto)
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/nueva-categoria-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            // 2. Armamos la nueva ventana (Stage) en modo modal para bloquear la de atrás
            javafx.stage.Stage modalStage = new javafx.stage.Stage();
            modalStage.setTitle("Nueva Categoría de Insumo");
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.setResizable(false);

            // 3. LA CLAVE: showAndWait frena el código acá hasta que cierres ese pop-up
            modalStage.showAndWait();

            // 4. Al cerrar el modal, ejecutamos esto para que el ComboBox se actualice al instante con lo nuevo
            cargarCategoriasDesdeBD();

        } catch (Exception e) {
            System.err.println("❌ Error al abrir el Pop-up de Categoria Insumo.");
            e.printStackTrace();
        }
    }

    @FXML
    public void guardarInsumo(ActionEvent event) {
        // 1. Validación básica con pop-up en la cara
        if (cmbCategoria.getValue() == null) {
            mostrarAlerta("Atención", "Debe seleccionar una categoría válida antes de guardar.", Alert.AlertType.WARNING);
            return;
        }

        try {
// 2. Armamos el objeto Insumo (Editado o Nuevo)
            Insumo insumoAGuardar = (insumoEnEdicion != null) ? insumoEnEdicion : new Insumo();

            insumoAGuardar.setCategoria(cmbCategoria.getValue());
            insumoAGuardar.setNombre(txtDescripcion.getText());

            if (txtCosto.getText() != null && !txtCosto.getText().trim().isEmpty()) {
                insumoAGuardar.setCostoTotal(new java.math.BigDecimal(txtCosto.getText().trim()));
            }

            // 3. Lógica dinámica
            String tipoMedicion = cmbCategoria.getValue().getTipoMedicion();

            if ("SUPERFICIE".equals(tipoMedicion)) {
                String ancho = (txtAnchoPlancha.getText() != null && !txtAnchoPlancha.getText().isEmpty()) ? txtAnchoPlancha.getText() : "0";
                String largo = (txtLargoPlancha.getText() != null && !txtLargoPlancha.getText().isEmpty()) ? txtLargoPlancha.getText() : "0";
                insumoAGuardar.setAnchoCm(Integer.parseInt(ancho));
                insumoAGuardar.setLargoCm(Integer.parseInt(largo));
            } else if ("UNIDAD".equals(tipoMedicion)) {
                insumoAGuardar.setCantidad(1);
            }

            // 4. Mandamos a la base de datos
            insumoApi.guardarInsumoEnBaseDeDatos(insumoAGuardar);

            // 5. 🎉 POP-UP DE ÉXITO
            mostrarAlerta("¡Excelente!", "El insumo se guardó correctamente en la base de datos.", Alert.AlertType.INFORMATION);

            // 6. Cerramos la ventana
            cerrarModal(event);

        } catch (Exception e) {
            // ❌ ACÁ ESTÁ EL POP-UP DE ERROR QUE PEDÍAS
            mostrarAlerta("Error del sistema", "No se pudo guardar el insumo:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // 🔥 AGREGAR ESTE MÉTODO COMPLETO ACÁ
    public void cargarDatosParaEditar(Insumo insumo) {
        this.insumoEnEdicion = insumo;

        txtDescripcion.setText(insumo.getNombre());

        if (insumo.getCostoTotal() != null) {
            txtCosto.setText(insumo.getCostoTotal().toString());
        }

        if (insumo.getCategoria() != null) {
            for (Categoria cat : cmbCategoria.getItems()) {
                if (cat.getId() != null && cat.getId().equals(insumo.getCategoria().getId())) {
                    cmbCategoria.setValue(cat);
                    break;
                }
            }
        }

        if (insumo.getAnchoCm() != null && insumo.getAnchoCm() > 0) {
            txtAnchoPlancha.setText(insumo.getAnchoCm().toString());
        }
        if (insumo.getLargoCm() != null && insumo.getLargoCm() > 0) {
            txtLargoPlancha.setText(insumo.getLargoCm().toString());
        }
    }

    @FXML
    public void cerrarModal(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }


    // 🔥 EL DIBUJADOR DE POP-UPS
    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }


}