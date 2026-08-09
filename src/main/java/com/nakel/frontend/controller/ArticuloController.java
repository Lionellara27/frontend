package com.nakel.frontend.controller;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.Articulo;
import com.nakel.frontend.model.Categoria;
import com.nakel.frontend.model.Material;
import com.nakel.frontend.service.ArticuloApiService;
import com.nakel.frontend.service.ParametrosApiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

public class ArticuloController {

    // --- FILTROS DE BÚSQUEDA REALES ---
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private ComboBox<Material> cmbMaterial;
    @FXML private ComboBox<String> cmbOrigen;

    // --- TABLA Y COLUMNAS ---
    @FXML private TableView<Articulo> tablaArticulos;
    @FXML private TableColumn<Articulo, String> colNro;
    @FXML private TableColumn<Articulo, String> colCodigo;
    @FXML private TableColumn<Articulo, String> colNombre;
    @FXML private TableColumn<Articulo, String> colCategoria;
    @FXML private TableColumn<Articulo, String> colMaterial;
    @FXML private TableColumn<Articulo, String> colOrigen;
    @FXML private TableColumn<Articulo, Integer> colStock;
    @FXML private TableColumn<Articulo, Double> colPrecio;
    @FXML private TableColumn<Articulo, Articulo> colAcciones;

    @FXML private Label lblTotalArticulos;
    @FXML private Pagination paginadorArticulos;

    // --- INSTANCIAMOS LOS DOS SERVICIOS ---
    private final ArticuloApiService apiService = new ArticuloApiService();
    private final ParametrosApiService parametrosService = new ParametrosApiService();

    private final Gson gson = new Gson();

    // 🔥 Agregamos listas observables para poder filtrar sin volver a consultar la API cada vez
    private ObservableList<Articulo> masterData = FXCollections.observableArrayList();
    private FilteredList<Articulo> filteredData;

    @FXML
    public void initialize() {
        System.out.println("Módulo de Catálogo Iniciado. Cargando Filtros reales...");

        tablaArticulos.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        // 🔥 Placeholder de los filtros
        // Cuando no hay selección muestran:
        // "Categoría..." | "Material..." | "Origen..."
        forzarTextoFantasma(cmbCategoria, "Categoría...");
        forzarTextoFantasma(cmbMaterial, "Material...");
        forzarTextoFantasma(cmbOrigen, "Origen...");

        // 📋 Cargar opciones de filtros
        cmbCategoria.getItems().addAll(
                parametrosService.obtenerCategorias()
        );

        cmbMaterial.getItems().addAll(
                parametrosService.obtenerMateriales()
        );

        cmbOrigen.getItems().addAll(
                "PRODUCCION_PROPIA",
                "REVENTA"
        );

        // 📊 Configurar columnas de la tabla
        configurarColumnas();

        // 📄 Configurar paginación
        if (paginadorArticulos != null) {

            paginadorArticulos.setPageFactory(paginaIndex -> {

                // paginaIndex comienza en 0
                // y se envía directamente al backend
                cargarTabla(paginaIndex);

                // La página de Pagination necesita devolver un Node.
                // La tabla ya está fuera del Pagination, por eso
                // devolvemos un contenedor vacío.
                return new javafx.scene.layout.VBox();
            });

        } else {

            // 🛟 Si por alguna razón no existe el Pagination,
            // cargamos directamente la primera página.
            cargarTabla(0);
        }
    }

    private static final int ARTICULOS_POR_PAGINA = 50;

    private void configurarColumnas() {

        // 🔢 Número correlativo global según la página
        // Página 1 → 1-50
        // Página 2 → 51-100
        // Página 3 → 101-150
        colNro.setCellFactory(col -> new TableCell<Articulo, String>() {

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                    return;
                }

                int paginaActual = paginadorArticulos != null
                        ? paginadorArticulos.getCurrentPageIndex()
                        : 0;

                int numeroGlobal =
                        (paginaActual * ARTICULOS_POR_PAGINA)
                                + getIndex()
                                + 1;

                setText(String.valueOf(numeroGlobal));
            }
        });

        // 🔹 Código
        colCodigo.setCellValueFactory(
                new PropertyValueFactory<>("codigo")
        );

        // 🔹 Nombre
        colNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre")
        );

        // 🔹 Stock
        colStock.setCellValueFactory(
                new PropertyValueFactory<>("stockActual")
        );

        // 🔹 Precio
        colPrecio.setCellValueFactory(
                new PropertyValueFactory<>("precio")
        );

        // 🔹 Categoría
        colCategoria.setCellValueFactory(cellData -> {

            Categoria cat = cellData.getValue().getCategoria();

            return new SimpleStringProperty(
                    cat != null
                            ? cat.getNombre()
                            : "Sin Categoría"
            );
        });

        // 🔹 Material
        if (colMaterial != null) {

            colMaterial.setCellValueFactory(cellData -> {

                Material mat = cellData.getValue().getMaterial();

                return new SimpleStringProperty(
                        mat != null
                                ? mat.getNombre()
                                : "Sin Material"
                );
            });
        }

        // 🔹 Origen
        if (colOrigen != null) {

            colOrigen.setCellValueFactory(cellData -> {

                String origen = cellData.getValue().getOrigen();

                return new SimpleStringProperty(
                        origen != null
                                ? origen
                                : "Sin Origen"
                );
            });
        }

        // 🔹 Acciones
        colAcciones.setCellValueFactory(
                param -> new javafx.beans.property.ReadOnlyObjectWrapper<>(
                        param.getValue()
                )
        );

        colAcciones.setPrefWidth(150);

        colAcciones.setCellFactory(
                param -> new TableCell<Articulo, Articulo>() {

                    private final Button btnVer =
                            new Button("", new FontIcon("fas-eye"));

                    private final Button btnEditar =
                            new Button("", new FontIcon("fas-pen"));

                    private final FontIcon iconoTacho =
                            new FontIcon("fas-trash-alt");

                    {
                        iconoTacho.setIconColor(
                                javafx.scene.paint.Color.web("#e74c3c")
                        );
                    }

                    private final Button btnEliminar =
                            new Button("", iconoTacho);

                    private final HBox pane =
                            new HBox(10, btnVer, btnEditar, btnEliminar);

                    {
                        pane.setAlignment(javafx.geometry.Pos.CENTER);

                        btnVer.setStyle(
                                "-fx-cursor: hand; -fx-background-color: transparent;"
                        );

                        btnEditar.setStyle(
                                "-fx-cursor: hand; -fx-background-color: transparent;"
                        );

                        btnEliminar.setStyle(
                                "-fx-cursor: hand; -fx-background-color: transparent;"
                        );

                        // 👁️ Ver
                        btnVer.setOnAction(e -> {

                            Articulo art = getItem();

                            if (art != null) {
                                mostrarDetalle(art);
                            }
                        });

                        // ✏️ Editar
                        btnEditar.setOnAction(e -> {

                            Articulo art = getItem();

                            if (art != null) {
                                editarArticulo(art);
                            }
                        });

                        // 🗑️ Eliminar
                        btnEliminar.setOnAction(e -> {

                            Articulo art = getItem();

                            if (art != null) {
                                eliminarArticulo(art);
                            }
                        });
                    }

                    @Override
                    protected void updateItem(
                            Articulo item,
                            boolean empty
                    ) {
                        super.updateItem(item, empty);

                        setGraphic(
                                empty || item == null
                                        ? null
                                        : pane
                        );
                    }
                }
        );
    }

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

        TextField txtCodigo = new TextField(); txtCodigo.setPromptText("Escriba el SKU final...");
        TextField txtNombre = new TextField(); txtNombre.setPromptText("Ej: Cartera Negra Imperial");
        TextField txtPrecio = new TextField(); txtPrecio.setPromptText("Ej: 50000");
        TextField txtStock = new TextField(); txtStock.setPromptText("Ej: 10");

        ComboBox<String> cmbOrigenDialog = new ComboBox<>();
        cmbOrigenDialog.getItems().addAll("PRODUCCION_PROPIA", "REVENTA");
        // 🔥 SOLUCIÓN 2: Por defecto REVENTA como pidieron
        cmbOrigenDialog.setValue("REVENTA");

        ComboBox<Categoria> cmbCategoriaDialog = new ComboBox<>();
        cmbCategoriaDialog.getItems().addAll(parametrosService.obtenerCategorias());
        cmbCategoriaDialog.setPromptText("Elegir Categoría...");

        ComboBox<Material> cmbMaterialDialog = new ComboBox<>();
        cmbMaterialDialog.getItems().addAll(parametrosService.obtenerMateriales());
        cmbMaterialDialog.setPromptText("Elegir Material...");

        cmbCategoriaDialog.setOnAction(e -> {
            Categoria seleccionada = cmbCategoriaDialog.getValue();
            if (seleccionada != null && seleccionada.getPrefijoSku() != null) {
                txtCodigo.setText(seleccionada.getPrefijoSku() + "-");
                txtCodigo.positionCaret(txtCodigo.getText().length());
            }
        });

        grid.add(new Label("Categoría:"), 0, 0);  grid.add(cmbCategoriaDialog, 1, 0);
        grid.add(new Label("Material:"), 0, 1);   grid.add(cmbMaterialDialog, 1, 1);
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
                if (exito) { cargarTabla(paginadorArticulos != null ? paginadorArticulos.getCurrentPageIndex() : 0); }
            }
        });
    }

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

    // 🔥 SOLUCIÓN 3b: Aviso amistoso de edición temporal (hasta que armemos el modal si es necesario)
// ✏️ AHORA SÍ ANDA: Reutilizamos el modal pero le cargamos los datos previos
    private void editarArticulo(Articulo articuloViejo) {
        Dialog<Articulo> dialog = new Dialog<>();
        dialog.setTitle("Editar Artículo");
        dialog.setHeaderText("Modificando: " + articuloViejo.getNombre());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/nakel.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("mostrador-container");

        ButtonType btnActualizar = new ButtonType("🔄 Actualizar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnActualizar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 20, 10, 10));

        // Campos de texto YA PRE-CARGADOS
        TextField txtCodigo = new TextField(articuloViejo.getCodigo());
        TextField txtNombre = new TextField(articuloViejo.getNombre());
        TextField txtPrecio = new TextField(String.valueOf(articuloViejo.getPrecio()));
        TextField txtStock = new TextField(String.valueOf(articuloViejo.getStockActual()));

        ComboBox<String> cmbOrigenDialog = new ComboBox<>();
        cmbOrigenDialog.getItems().addAll("PRODUCCION_PROPIA", "REVENTA");
        cmbOrigenDialog.setValue(articuloViejo.getOrigen());

        ComboBox<Categoria> cmbCategoriaDialog = new ComboBox<>();
        cmbCategoriaDialog.getItems().addAll(parametrosService.obtenerCategorias());
        // Buscamos la categoría vieja en la lista nueva para seleccionarla
        if (articuloViejo.getCategoria() != null) {
            for (Categoria c : cmbCategoriaDialog.getItems()) {
                if (c.getId().equals(articuloViejo.getCategoria().getId())) cmbCategoriaDialog.setValue(c);
            }
        }

        ComboBox<Material> cmbMaterialDialog = new ComboBox<>();
        cmbMaterialDialog.getItems().addAll(parametrosService.obtenerMateriales());
        // Buscamos el material viejo en la lista nueva para seleccionarlo
        if (articuloViejo.getMaterial() != null) {
            for (Material m : cmbMaterialDialog.getItems()) {
                if (m.getId().equals(articuloViejo.getMaterial().getId())) cmbMaterialDialog.setValue(m);
            }
        }

        grid.add(new Label("Categoría:"), 0, 0);  grid.add(cmbCategoriaDialog, 1, 0);
        grid.add(new Label("Material:"), 0, 1);   grid.add(cmbMaterialDialog, 1, 1);
        grid.add(new Label("Código/SKU:"), 0, 2);  grid.add(txtCodigo, 1, 2);
        grid.add(new Label("Nombre:"), 0, 3);     grid.add(txtNombre, 1, 3);
        grid.add(new Label("Precio ($):"), 0, 4);  grid.add(txtPrecio, 1, 4);
        grid.add(new Label("Stock Actual:"), 0, 5);  grid.add(txtStock, 1, 5);
        grid.add(new Label("Origen:"), 0, 6);      grid.add(cmbOrigenDialog, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnActualizar) {
                try {
                    // Actualizamos el objeto que ya existía (mismo ID)
                    articuloViejo.setCodigo(txtCodigo.getText().trim());
                    articuloViejo.setNombre(txtNombre.getText().trim());
                    articuloViejo.setPrecio(Double.parseDouble(txtPrecio.getText()));
                    articuloViejo.setStockActual(Integer.parseInt(txtStock.getText()));
                    articuloViejo.setOrigen(cmbOrigenDialog.getValue());
                    articuloViejo.setCategoria(cmbCategoriaDialog.getValue());
                    articuloViejo.setMaterial(cmbMaterialDialog.getValue());
                    return articuloViejo;
                } catch (Exception e) {
                    System.out.println("⚠️ Error al validar datos del formulario");
                    return null;
                }
            }
            return null;
        });

        // Llamamos al servicio para guardar (PUT)
        dialog.showAndWait().ifPresent(artActualizado -> {
            if (artActualizado != null) {
                boolean exito = apiService.actualizarArticulo(artActualizado); // 👈 Llama al nuevo método PUT
                if (exito) {
                    cargarTabla(paginadorArticulos != null ? paginadorArticulos.getCurrentPageIndex() : 0); // Refresca catálogo si anduvo bien
                }
            }
        });
    }

    // 🔥 SOLUCIÓN 3a: Borrado Seguro (Ataja errores de Base de Datos y Claves Foráneas)
    private void eliminarArticulo(Articulo articulo) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, "¿Seguro que desea eliminar '" + articulo.getNombre() + "' del sistema de forma permanente?", ButtonType.YES, ButtonType.NO);
        alerta.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    boolean exito = apiService.eliminarArticuloDeBaseDeDatos(articulo.getId());
                    if (exito) {
                        cargarTabla(paginadorArticulos != null ? paginadorArticulos.getCurrentPageIndex() : 0);
                    } else {
                        Alert error = new Alert(Alert.AlertType.ERROR, "No se puede eliminar el artículo porque tiene un historial de ventas asociado o hubo un error en la base de datos.\n\nPara quitarlo del catálogo, edite su stock a 0.");
                        error.setHeaderText("Operación Rechazada por Seguridad");
                        error.showAndWait();
                    }
                } catch (Exception e) {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Error interno al contactar al servidor: " + e.getMessage());
                    error.showAndWait();
                }
            }
        });
    }

    private void cargarTabla(int numeroPagina) {

        String textoBuscado = txtBuscar.getText() != null
                ? txtBuscar.getText().trim()
                : "";

        Categoria categoriaSeleccionada = cmbCategoria.getValue();
        Material materialSeleccionado = cmbMaterial.getValue();
        String origenSeleccionado = cmbOrigen.getValue();

        Long categoriaId = categoriaSeleccionada != null
                ? categoriaSeleccionada.getId()
                : null;

        Long materialId = materialSeleccionado != null
                ? materialSeleccionado.getId()
                : null;

        String json = apiService.buscarArticulos(
                textoBuscado,
                categoriaId,
                materialId,
                origenSeleccionado,
                numeroPagina,
                50
        );

        if (json != null && !json.isEmpty()) {

            try {

                JsonElement elementoParseado = JsonParser.parseString(json);

                if (!elementoParseado.isJsonObject()) {
                    throw new RuntimeException("El backend no devolvió una respuesta paginada válida.");
                }

                JsonObject respuestaServidor =
                        elementoParseado.getAsJsonObject();

                // 📄 Cantidad de páginas después de aplicar TODOS los filtros
                if (respuestaServidor.has("totalPages")
                        && paginadorArticulos != null) {

                    int totalPaginas =
                            respuestaServidor.get("totalPages").getAsInt();

                    paginadorArticulos.setPageCount(
                            totalPaginas == 0 ? 1 : totalPaginas
                    );
                }

                // 🔢 Cantidad TOTAL después de aplicar los filtros
                int totalBD = respuestaServidor.has("totalElements")
                        ? respuestaServidor.get("totalElements").getAsInt()
                        : 0;

                // 📦 Los artículos de ESTA página solamente
                JsonArray arregloArticulos =
                        respuestaServidor.getAsJsonArray("content");

                Type tipoLista =
                        new TypeToken<List<Articulo>>() {}.getType();

                List<Articulo> listaBackend =
                        gson.fromJson(arregloArticulos, tipoLista);

                if (listaBackend == null) {
                    listaBackend = List.of();
                }

                // 🧹 Reemplazamos solamente los 50 artículos de la página actual
                masterData.setAll(listaBackend);

                // 📋 La tabla muestra directamente la página recibida
                tablaArticulos.setItems(masterData);

                // 🔢 Contador global
                lblTotalArticulos.setText(
                        "Total en catálogo: " + totalBD + " artículos"
                );

            } catch (Exception e) {

                System.out.println(
                        "❌ Error al cargar artículos: " + e.getMessage()
                );

                e.printStackTrace();

                masterData.clear();

                if (paginadorArticulos != null) {
                    paginadorArticulos.setPageCount(1);
                }

                lblTotalArticulos.setText(
                        "Total en catálogo: 0 artículos"
                );
            }

        } else {

            masterData.clear();

            if (paginadorArticulos != null) {
                paginadorArticulos.setPageCount(1);
            }

            lblTotalArticulos.setText(
                    "Total en catálogo: 0 artículos"
            );
        }
    }

    // 🔥 SOLUCIÓN 1: La Magia de la Lupa (Buscador Multi-Filtro)
    @FXML
    public void buscarArticulos(ActionEvent event) {

        System.out.println("🔍 Buscando artículos en el backend...");

        // 🔄 Cada nueva búsqueda comienza desde la primera página
        if (paginadorArticulos != null) {
            paginadorArticulos.setCurrentPageIndex(0);
        }

        // 🚀 cargarTabla() lee automáticamente:
        // - texto
        // - categoría
        // - material
        // - origen
        //
        // y se los manda al backend.
        cargarTabla(0);
    }

    @FXML
    public void limpiarFiltros(ActionEvent event) {

        // 🧹 Limpiamos el campo de búsqueda
        txtBuscar.clear();

        // 🧹 Quitamos las selecciones de los ComboBox
        // Vuelven a mostrar "Categoría...", "Material..." y "Origen..."
        cmbCategoria.setValue(null);
        cmbMaterial.setValue(null);
        cmbOrigen.setValue(null);

        // 🔄 Volvemos a la primera página
        if (paginadorArticulos != null) {
            paginadorArticulos.setCurrentPageIndex(0);
        }

        // 🔥 Volvemos a cargar el catálogo completo desde el backend
        cargarTabla(0);
    }

    // 🪄 HERRAMIENTA MÁGICA: Obliga a JavaFX a mostrar el texto gris siempre que esté vacío
    private <T> void forzarTextoFantasma(ComboBox<T> combo, String texto) {
        combo.setPromptText(texto);
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(texto);
                } else {
                    setText(item.toString());
                }
            }
        });
    }
}