package com.nakel.frontend.controller;

import com.nakel.frontend.model.Articulo;
import com.nakel.frontend.service.ArticuloApiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.util.List;

public class ArticuloController {

    // Filtros de búsqueda
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private ComboBox<String> cmbMaterial;
    @FXML private ComboBox<String> cmbOrigen;

    // Tabla y Columnas
    @FXML private TableView<Articulo> tablaArticulos; // <-- Ahora sabe que guarda Artículos
    @FXML private TableColumn<Articulo, String> colCodigo;
    @FXML private TableColumn<Articulo, String> colNombre;
    @FXML private TableColumn<Articulo, String> colCategoria;
    @FXML private TableColumn<Articulo, Integer> colStock;
    @FXML private TableColumn<Articulo, Double> colPrecio;
    @FXML private TableColumn<Articulo, Articulo> colAcciones; // <-- Columna vacía para meter los botones

    @FXML private Label lblTotalArticulos;

    //nuevas columnas:
    @FXML private TableColumn<Articulo, String> colNro;


    // Instanciamos el servicio
    private final ArticuloApiService apiService = new ArticuloApiService();



    @FXML
    public void initialize() {
        System.out.println("Módulo de Catálogo Iniciado.");

        tablaArticulos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Llenamos los combos provisorios
        cmbCategoria.getItems().addAll("Billeteras", "Mates", "Lámparas");
        cmbMaterial.getItems().addAll("Cuero Liso", "Cuero Peludo", "Madera", "Metal");
        cmbOrigen.getItems().addAll("PRODUCCION_PROPIA", "REVENTA");

        // 1. Configuramos cómo se lee cada columna
        configurarColumnas();

        // 2. Traemos los datos del Backend
        cargarTabla();
    }

    private void configurarColumnas() {
        // 1. Columna Nro: Autogenera el número de fila (1, 2, 3...)
        colNro.setCellFactory(col -> new TableCell<Articulo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });

        // 2. Datos simples: Vinculamos el nombre de la variable de la clase Articulo
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));

        // 3. Datos anidados: Categoría
        colCategoria.setCellValueFactory(cellData -> {
            Articulo.Categoria cat = cellData.getValue().getCategoria();
            return new javafx.beans.property.SimpleStringProperty(cat != null ? cat.getNombre() : "Sin Categoría");
        });

        // 4. 🔥 LA MAGIA: Botones en la columna de Acciones
        // IMPORTANTE: Definimos la columna como <Articulo, Articulo> para poder acceder al objeto
        colAcciones.setCellValueFactory(param -> new javafx.beans.property.ReadOnlyObjectWrapper<>(param.getValue()));

        colAcciones.setCellFactory(param -> new TableCell<Articulo, Articulo>() {
            private final Button btnEditar = new Button("✏️");
            private final Button btnAjuste = new Button("🔄");
            private final Button btnEliminar = new Button("🗑️");
            private final HBox pane = new HBox(5, btnEditar, btnAjuste, btnEliminar);

            {
                btnEditar.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");
                btnAjuste.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");
                btnEliminar.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");

                btnEditar.setOnAction(e -> {
                    Articulo art = getItem();
                    if (art != null) System.out.println("✏️ Editar: " + art.getNombre());
                });

                btnAjuste.setOnAction(e -> {
                    Articulo art = getItem();
                    if (art != null) System.out.println("🔄 Ajuste para: " + art.getNombre());
                });

                btnEliminar.setOnAction(e -> {
                    Articulo art = getItem();
                    if (art != null) System.out.println("🗑️ Borrar ID: " + art.getId());
                });
            }

            @Override
            protected void updateItem(Articulo item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : pane);
            }
        });
    }

    private void cargarTabla() {
        System.out.println("Conectando con Backend para traer catálogo...");
        List<Articulo> listaBackend = apiService.obtenerTodos();

        if (listaBackend != null && !listaBackend.isEmpty()) {
            ObservableList<Articulo> articulosObservables = FXCollections.observableArrayList(listaBackend);
            tablaArticulos.setItems(articulosObservables);
            lblTotalArticulos.setText("Total en catálogo: " + listaBackend.size() + " artículos");
            System.out.println("✅ Catálogo cargado. (" + listaBackend.size() + " ítems)");
        } else {
            System.out.println("⚠️ El catálogo está vacío o no hay conexión.");
            lblTotalArticulos.setText("Total en catálogo: 0 artículos");
        }
    }

    // --- MÉTODOS DE BOTONES (Se mantienen iguales) ---

    @FXML
    public void buscarArticulos(ActionEvent event) {
        System.out.println("Buscando artículos...");
        // Futuro: Filtrar la listaObservable localmente o ir al backend
    }

    @FXML
    public void limpiarFiltros(ActionEvent event) {
        txtBuscar.clear();
        cmbCategoria.setValue(null);
        cmbMaterial.setValue(null);
        cmbOrigen.setValue(null);
        // Volver a cargar la tabla completa
        cargarTabla();
    }

    @FXML
    public void abrirModalNuevoArticulo(ActionEvent event) {
        Dialog<Articulo> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Artículo");
        dialog.setHeaderText("Cargar nuevo producto");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/nakel.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("mostrador-container");

        ButtonType btnGuardar = new ButtonType("💾 Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 20, 10, 10));

        // Campos
        TextField txtCodigo = new TextField(); txtCodigo.setPromptText("Se autogenera...");
        TextField txtNombre = new TextField(); txtNombre.setPromptText("Ej: Cartera Negra");
        TextField txtPrecio = new TextField(); txtPrecio.setPromptText("Ej: 50000");
        TextField txtStock = new TextField(); txtStock.setPromptText("Ej: 10");

        ComboBox<String> cmbOrigenDialog = new ComboBox<>();
        cmbOrigenDialog.getItems().addAll("PRODUCCION_PROPIA", "REVENTA");
        cmbOrigenDialog.setValue("PRODUCCION_PROPIA");

        // LOS COMBOS QUE FALTABAN
        ComboBox<String> cmbCatTemporal = new ComboBox<>();
        cmbCatTemporal.getItems().addAll("Billeteras", "Mates", "Carteras");
        cmbCatTemporal.setPromptText("Elegir...");

        // 🔥 LA MAGIA DEL SKU 🔥
        cmbCatTemporal.setOnAction(e -> {
            String seleccion = cmbCatTemporal.getValue();
            if ("Billeteras".equals(seleccion)) {
                txtCodigo.setText("1111-"); // Le pre-escribe el inicio
            } else if ("Mates".equals(seleccion)) {
                txtCodigo.setText("2222-");
            } else if ("Carteras".equals(seleccion)) {
                txtCodigo.setText("3333-");
            }
            // Manda el cursor al final para que la dueña siga escribiendo
            txtCodigo.positionCaret(txtCodigo.getText().length());
        });

        grid.add(new Label("Categoría:"), 0, 0); grid.add(cmbCatTemporal, 1, 0);
        grid.add(new Label("Código/SKU:"), 0, 1); grid.add(txtCodigo, 1, 1);
        grid.add(new Label("Nombre:"), 0, 2); grid.add(txtNombre, 1, 2);
        grid.add(new Label("Precio ($):"), 0, 3); grid.add(txtPrecio, 1, 3);
        grid.add(new Label("Stock Inic:"), 0, 4); grid.add(txtStock, 1, 4);
        grid.add(new Label("Origen:"), 0, 5); grid.add(cmbOrigenDialog, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                try {
                    Articulo nuevo = new Articulo();
                    nuevo.setCodigo(txtCodigo.getText());
                    nuevo.setNombre(txtNombre.getText());
                    nuevo.setPrecio(Double.parseDouble(txtPrecio.getText()));
                    nuevo.setStockActual(Integer.parseInt(txtStock.getText()));
                    nuevo.setOrigen(cmbOrigenDialog.getValue());
                    nuevo.setAlicuotaIva(21.0);

                    /* NOTA: Acá a futuro asociaremos el objeto Categoría real de la base de datos
                       buscando el ID correspondiente según lo que eligió en cmbCatTemporal. */

                    return nuevo;
                } catch (Exception e) {
                    System.out.println("⚠️ Error en los datos ingresados");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(nuevoArticulo -> {
            boolean exito = apiService.guardarArticulo(nuevoArticulo);
            if (exito) {
                cargarTabla();
            }
        });
    }
}