package com.nakel.frontend.controller;

import com.nakel.frontend.model.Articulo;
import com.nakel.frontend.model.Categoria;
import com.nakel.frontend.model.Material;
import com.nakel.frontend.service.ArticuloApiService;
import com.nakel.frontend.service.ParametrosApiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Optional;

public class ArticuloController {

    // --- FILTROS DE BÚSQUEDA REALES ---
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<Categoria> cmbCategoria; // Ahora tipados con el objeto real
    @FXML private ComboBox<Material> cmbMaterial;   // Ahora tipados con el objeto real
    @FXML private ComboBox<String> cmbOrigen;

    // --- TABLA Y COLUMNAS ---
    @FXML private TableView<Articulo> tablaArticulos;
    @FXML private TableColumn<Articulo, String> colNro;
    @FXML private TableColumn<Articulo, String> colCodigo;
    @FXML private TableColumn<Articulo, String> colNombre;
    @FXML private TableColumn<Articulo, String> colCategoria;

    // Agregamos estas dos columnas que pedía tu PDF para que no queden en blanco
    @FXML private TableColumn<Articulo, String> colMaterial;
    @FXML private TableColumn<Articulo, String> colOrigen;

    @FXML private TableColumn<Articulo, Integer> colStock;
    @FXML private TableColumn<Articulo, Double> colPrecio;
    @FXML private TableColumn<Articulo, Articulo> colAcciones;

    @FXML private Label lblTotalArticulos;

    // --- INSTANCIAMOS LOS DOS SERVICIOS ---
    private final ArticuloApiService apiService = new ArticuloApiService();
    private final ParametrosApiService parametrosService = new ParametrosApiService();

    @FXML
    public void initialize() {
        System.out.println("Módulo de Catálogo Iniciado. Cargando Filtros reales...");

        tablaArticulos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 📋 CARGA DE COMBOS DE FILTROS DESDE SQLITE (Fase 3)
        cmbCategoria.getItems().addAll(parametrosService.obtenerCategorias());
        cmbMaterial.getItems().addAll(parametrosService.obtenerMateriales());

        // El origen queda fijo porque es un Enum ("PRODUCCION_PROPIA" o "REVENTA")
        cmbOrigen.getItems().addAll("PRODUCCION_PROPIA", "REVENTA");

        // Configuramos bindings de las columnas
        configurarColumnas();

        // Traemos los datos del Backend
        cargarTabla();
    }

    private void configurarColumnas() {
        colNro.setCellFactory(col -> new TableCell<Articulo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });

        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));

        // Binding de datos anidados: Categoría (Real)
        colCategoria.setCellValueFactory(cellData -> {
            Categoria cat = cellData.getValue().getCategoria();
            return new SimpleStringProperty(cat != null ? cat.getNombre() : "Sin Categoría");
        });

        // 🛠️ SOLUCIÓN A COLUMNAS EN BLANCO (Binding de Material y Origen)
        if (colMaterial != null) {
            colMaterial.setCellValueFactory(cellData -> {
                Material mat = cellData.getValue().getMaterial();
                return new SimpleStringProperty(mat != null ? mat.getNombre() : "Sin Material");
            });
        }

        if (colOrigen != null) {
            colOrigen.setCellValueFactory(cellData -> {
                String origen = cellData.getValue().getOrigen();
                return new SimpleStringProperty(origen != null ? origen : "Sin Origen");
            });
        }

        // Botones de acción (Ver, Editar, Eliminar)
        colAcciones.setCellValueFactory(param -> new javafx.beans.property.ReadOnlyObjectWrapper<>(param.getValue()));
        colAcciones.setPrefWidth(150);
        colAcciones.setCellFactory(param -> new TableCell<Articulo, Articulo>() {
            private final Button btnVer = new Button("", new FontIcon("fas-eye"));
            private final Button btnEditar = new Button("", new FontIcon("fas-pen"));
            private final FontIcon iconoTacho = new FontIcon("fas-trash-alt");
            { iconoTacho.setIconColor(javafx.scene.paint.Color.web("#e74c3c")); }
            private final Button btnEliminar = new Button("", iconoTacho);
            private final HBox pane = new HBox(10, btnVer, btnEditar, btnEliminar);

            {
                pane.setAlignment(javafx.geometry.Pos.CENTER);
                btnVer.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");
                btnEditar.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");
                btnEliminar.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");

                btnVer.setOnAction(e -> { Articulo art = getItem(); if (art != null) mostrarDetalle(art); });
                btnEditar.setOnAction(e -> { Articulo art = getItem(); if (art != null) editarArticulo(art); });
                btnEliminar.setOnAction(e -> { Articulo art = getItem(); if (art != null) eliminarArticulo(art); });
            }

            @Override
            protected void updateItem(Articulo item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : pane);
            }
        });
    }

    // --- MODAL NUEVO ARTÍCULO REFACTORIZADO (Fase 3 y 4) ---
    @FXML
    public void abrirModalNuevoArticulo(ActionEvent event) {
        Dialog<Articulo> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Artículo");
        dialog.setHeaderText("Cargar nuevo producto al catálogo");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/nakel.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("mostrador-container");

        ButtonType btnGuardar = new ButtonType("💾 Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 20, 10, 10));

        // Campos de texto
        TextField txtCodigo = new TextField(); txtCodigo.setPromptText("Escriba el SKU final...");
        TextField txtNombre = new TextField(); txtNombre.setPromptText("Ej: Cartera Negra Imperial");
        TextField txtPrecio = new TextField(); txtPrecio.setPromptText("Ej: 50000");
        TextField txtStock = new TextField(); txtStock.setPromptText("Ej: 10");

        ComboBox<String> cmbOrigenDialog = new ComboBox<>();
        cmbOrigenDialog.getItems().addAll("PRODUCCION_PROPIA", "REVENTA");
        cmbOrigenDialog.setValue("PRODUCCION_PROPIA");

        // 🚀 COMBOS REALES CON OBJETOS DESDE LA BASE DE DATOS
        ComboBox<Categoria> cmbCategoriaDialog = new ComboBox<>();
        cmbCategoriaDialog.getItems().addAll(parametrosService.obtenerCategorias());
        cmbCategoriaDialog.setPromptText("Elegir Categoría...");

        ComboBox<Material> cmbMaterialDialog = new ComboBox<>();
        cmbMaterialDialog.getItems().addAll(parametrosService.obtenerMateriales());
        cmbMaterialDialog.setPromptText("Elegir Material...");

        // 🔥 LA MAGIA DEL SKU REAL (Usa el prefijo guardado en la base de datos)
        cmbCategoriaDialog.setOnAction(e -> {
            Categoria seleccionada = cmbCategoriaDialog.getValue();
            if (seleccionada != null && seleccionada.getPrefijoSku() != null) {
                txtCodigo.setText(seleccionada.getPrefijoSku() + "-"); // Pre-escribe ej: "1111-"
                txtCodigo.positionCaret(txtCodigo.getText().length()); // Cursor al final
            }
        });

        // Posiciones en la grilla del formulario
        grid.add(new Label("Categoría:"), 0, 0);  grid.add(cmbCategoriaDialog, 1, 0);
        grid.add(new Label("Material:"), 0, 1);   grid.add(cmbMaterialDialog, 1, 1); // ➕ Nuevo campo Material
        grid.add(new Label("Código/SKU:"), 0, 2);  grid.add(txtCodigo, 1, 2);
        grid.add(new Label("Nombre:"), 0, 3);     grid.add(txtNombre, 1, 3);
        grid.add(new Label("Precio ($):"), 0, 4);  grid.add(txtPrecio, 1, 4);
        grid.add(new Label("Stock Inic:"), 0, 5);  grid.add(txtStock, 1, 5);
        grid.add(new Label("Origen:"), 0, 6);      grid.add(cmbOrigenDialog, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                try {
                    Articulo nuevo = new Articulo();
                    nuevo.setCodigo(txtCodigo.getText().trim());
                    nuevo.setNombre(txtNombre.getText().trim());
                    nuevo.setPrecio(Double.parseDouble(txtPrecio.getText()));
                    nuevo.setStockActual(Integer.parseInt(txtStock.getText()));
                    nuevo.setOrigen(cmbOrigenDialog.getValue());
                    nuevo.setAlicuotaIva(21.0);

                    // 💾 ASIGNAMOS LOS OBJETOS COMPLETOS (Fase 4: Se envían como sub-objetos en el JSON)
                    nuevo.setCategoria(cmbCategoriaDialog.getValue());
                    nuevo.setMaterial(cmbMaterialDialog.getValue());

                    return nuevo;
                } catch (Exception e) {
                    System.out.println("⚠️ Error al validar datos del formulario");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(nuevoArticulo -> {
            if (nuevoArticulo != null) {
                boolean exito = apiService.guardarArticulo(nuevoArticulo);
                if (exito) {
                    cargarTabla(); // Refresca catálogo
                }
            }
        });
    }

    // --- SE MANTIENEN IGUALES EL RESTO DE MÉTODOS ---
    private void mostrarDetalle(Articulo articulo) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Detalle del Artículo");
        alerta.setHeaderText(articulo.getNombre() + " (SKU: " + articulo.getCodigo() + ")");
        String info = "Precio: $" + articulo.getPrecio() + "\n"
                + "Stock Actual: " + articulo.getStockActual() + " unidades\n"
                + "Categoría: " + (articulo.getCategoria() != null ? articulo.getCategoria().getNombre() : "N/A") + "\n"
                + "Material: " + (articulo.getMaterial() != null ? articulo.getMaterial().getNombre() : "N/A") + "\n"
                + "Origen: " + articulo.getOrigen() + "\n";
        alerta.setContentText(info);
        alerta.showAndWait();
    }

    private void editarArticulo(Articulo articulo) { System.out.println("Editar: " + articulo.getNombre()); }

    private void eliminarArticulo(Articulo articulo) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, "¿Seguro que desea eliminar " + articulo.getNombre() + "?", ButtonType.OK, ButtonType.CANCEL);
        alerta.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try { apiService.eliminarArticuloDeBaseDeDatos(articulo.getId()); cargarTabla(); } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    private void cargarTabla() {
        List<Articulo> listaBackend = apiService.obtenerTodos();
        if (listaBackend != null && !listaBackend.isEmpty()) {
            tablaArticulos.setItems(FXCollections.observableArrayList(listaBackend));
            lblTotalArticulos.setText("Total en catálogo: " + listaBackend.size() + " artículos");
        } else {
            tablaArticulos.getItems().clear();
            lblTotalArticulos.setText("Total en catálogo: 0 artículos");
        }
    }

    @FXML public void buscarArticulos(ActionEvent event) { System.out.println("Filtrando..."); }
    @FXML public void limpiarFiltros(ActionEvent event) { txtBuscar.clear(); cmbCategoria.setValue(null); cmbMaterial.setValue(null); cmbOrigen.setValue(null); cargarTabla(); }
}