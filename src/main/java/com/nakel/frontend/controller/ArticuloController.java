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
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.*;
import java.io.File;
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
    @FXML private TableColumn<Articulo, String> colGanancia;
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

        // F3 = ir al buscador y buscar
        com.nakel.frontend.util.Atajos.buscarConF3(txtBuscar, () -> buscarArticulos(null));
        // Enter en el buscador también busca: es lo que manda un lector de código de barras al terminar de leer
        txtBuscar.setOnAction(e -> buscarArticulos(null));

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
        // Se muestra como plata: 10000.0 -> $ 10.000,00
        colPrecio.setCellFactory(com.nakel.frontend.util.FormatoMoneda.celdaPesos());

        // 🔹 Ganancia por unidad: "$ 3.000,00 (30 %)" o "Sin costo" si falta cargar cuánto costó
        colGanancia.setCellValueFactory(cellData -> new SimpleStringProperty(textoGanancia(cellData.getValue())));
        colGanancia.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                // En gris lo que falta cargar; en rojo si se vende por debajo del costo
                String color = empty || item == null ? "" : item.startsWith("Sin") ? "#9AA4AE" : item.startsWith("−") ? "#C0392B" : "#155643";
                setStyle(color.isEmpty() ? "" : "-fx-text-fill: " + color + ";");
            }
        });

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

                // En la tabla va en corto: "PRODUCCION_PROPIA" no entraba y se cortaba
                return new SimpleStringProperty(
                        origen == null ? "Sin origen"
                                : "PRODUCCION_PROPIA".equals(origen) ? "Propia"
                                : "REVENTA".equals(origen) ? "Reventa" : origen
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

                        // Mismo aspecto y cartelitos de ayuda que en el resto de las tablas
                        com.nakel.frontend.util.Acciones.ver(btnVer, "Ver el detalle");
                        com.nakel.frontend.util.Acciones.editar(btnEditar, "Editar el artículo");
                        com.nakel.frontend.util.Acciones.eliminar(btnEliminar, "Dar de baja el artículo");

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

        ButtonType btnGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 20, 10, 10));

        TextField txtCodigo = new TextField(); txtCodigo.setPromptText("Escriba el SKU final...");
        TextField txtNombre = new TextField(); txtNombre.setPromptText("Ej: Cartera Negra Imperial");
        TextField txtPrecio = new TextField(); txtPrecio.setPromptText("Ej: 50.000");
        com.nakel.frontend.util.FormatoMoneda.aplicar(txtPrecio); // 50000 -> 50.000 mientras se escribe
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
        TextField[] extras = agregarCostoYMinimo(grid, cmbOrigenDialog, null);
        prepararCodigoParaLector(txtCodigo, txtNombre, cmbCategoriaDialog);

        dialog.getDialogPane().setContent(grid);

        // El guardado se hace ANTES de que el diálogo se cierre. Si un dato está mal o el servidor
        // rechaza (código repetido, etc.), se avisa y la ventana queda abierta con todo lo cargado.
        // Antes el diálogo se cerraba sin decir nada y se perdía lo escrito.
        final boolean[] guardado = {false};
        Button botonGuardar = (Button) dialog.getDialogPane().lookupButton(btnGuardar);
        botonGuardar.addEventFilter(ActionEvent.ACTION, ev -> {
            Articulo nuevo = leerFormularioArticulo(new Articulo(), txtCodigo, txtNombre, txtPrecio, txtStock,
                    cmbOrigenDialog, cmbCategoriaDialog, cmbMaterialDialog, extras[0], extras[1]);
            if (nuevo == null) {
                ev.consume();
                return;
            }
            nuevo.setAlicuotaIva(21.0);

            if (!apiService.guardarArticulo(nuevo)) {
                avisarErrorArticulo("No se pudo guardar el artículo", apiService.getUltimoError());
                ev.consume();
                return;
            }
            guardado[0] = true;
        });

        dialog.setResultConverter(dialogButton -> null);
        dialog.showAndWait();

        if (guardado[0]) {
            cargarTabla(paginadorArticulos != null ? paginadorArticulos.getCurrentPageIndex() : 0);
        }
    }

    // Lee y valida el formulario de alta/edición. Si algo está mal avisa y devuelve null (no cierra nada).
    private Articulo leerFormularioArticulo(Articulo destino, TextField txtCodigo, TextField txtNombre,
                                            TextField txtPrecio, TextField txtStock, ComboBox<String> cmbOrigen,
                                            ComboBox<Categoria> cmbCategoria, ComboBox<Material> cmbMaterial,
                                            TextField txtCosto, TextField txtStockMinimo) {
        String codigo = txtCodigo.getText() == null ? "" : txtCodigo.getText().trim();
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();

        if (codigo.isEmpty() || codigo.endsWith("-")) {
            avisarErrorArticulo("Falta el código", "Completá el Código/SKU del artículo.");
            return null;
        }
        if (nombre.isEmpty()) {
            avisarErrorArticulo("Falta el nombre", "Escribí el nombre del artículo.");
            return null;
        }

        double precio;
        try {
            // Acepta "50000", "50.000", "50000,50" y "50000.50"
            precio = PagoMixtoController.parsearMonto(txtPrecio.getText());
        } catch (Exception e) {
            avisarErrorArticulo("Precio inválido", "El precio tiene que ser un número. Ejemplo: 50000 o 50000,50");
            return null;
        }
        if (precio < 0) {
            avisarErrorArticulo("Precio inválido", "El precio no puede ser negativo.");
            return null;
        }

        int stock;
        try {
            stock = Integer.parseInt(txtStock.getText().trim());
        } catch (Exception e) {
            avisarErrorArticulo("Stock inválido", "El stock tiene que ser un número entero. Ejemplo: 10");
            return null;
        }
        if (stock < 0) {
            avisarErrorArticulo("Stock inválido", "El stock no puede ser negativo.");
            return null;
        }

        // El costo es opcional: vacío = "todavía no lo cargué"
        Double costo = null;
        if (txtCosto.getText() != null && !txtCosto.getText().isBlank()) {
            try {
                costo = PagoMixtoController.parsearMonto(txtCosto.getText());
            } catch (Exception e) {
                avisarErrorArticulo("Costo inválido", "El costo tiene que ser un número. Si no lo sabés, dejalo vacío.");
                return null;
            }
            if (costo < 0) {
                avisarErrorArticulo("Costo inválido", "El costo no puede ser negativo.");
                return null;
            }
        }

        int stockMinimo;
        try {
            stockMinimo = Integer.parseInt(txtStockMinimo.getText().trim());
        } catch (Exception e) {
            avisarErrorArticulo("Stock mínimo inválido", "El stock mínimo tiene que ser un número entero. Ejemplo: 2");
            return null;
        }
        if (stockMinimo < 0) {
            avisarErrorArticulo("Stock mínimo inválido", "El stock mínimo no puede ser negativo.");
            return null;
        }

        destino.setCosto(costo);
        destino.setStockMinimo(stockMinimo);
        destino.setCodigo(codigo);
        destino.setNombre(nombre);
        destino.setPrecio(precio);
        destino.setStockActual(stock);
        destino.setOrigen(cmbOrigen.getValue());
        destino.setCategoria(cmbCategoria.getValue());
        destino.setMaterial(cmbMaterial.getValue());
        return destino;
    }

    /**
     * Agrega al formulario los renglones de costo y de stock mínimo, y devuelve sus dos casilleros.
     * El costo se llama "Precio de compra" en la reventa y "Costo de producción" en lo fabricado.
     */
    private TextField[] agregarCostoYMinimo(GridPane grid, ComboBox<String> cmbOrigen, Articulo existente) {
        Label lblCosto = new Label();
        TextField txtCosto = new TextField();
        txtCosto.setPromptText("Opcional. Ej: 30.000");
        com.nakel.frontend.util.FormatoMoneda.aplicar(txtCosto);
        if (existente != null && existente.getCosto() != null) {
            com.nakel.frontend.util.FormatoMoneda.poner(txtCosto, existente.getCosto());
        }
        Runnable nombrar = () -> lblCosto.setText("PRODUCCION_PROPIA".equals(cmbOrigen.getValue())
                ? "Costo producción ($):" : "Precio de compra ($):");
        nombrar.run();
        cmbOrigen.valueProperty().addListener((obs, antes, ahora) -> nombrar.run());

        TextField txtMinimo = new TextField(String.valueOf(
                existente != null && existente.getStockMinimo() != null ? existente.getStockMinimo() : 2));
        txtMinimo.setPromptText("Ej: 2");

        Label ayuda = new Label("El costo sirve para saber cuánto ganás; si no lo sabés, dejalo vacío. "
                + "El stock mínimo es desde cuántas unidades el sistema te avisa que hay que reponer.");
        ayuda.getStyleClass().add("dato-ayuda");
        ayuda.setWrapText(true);
        ayuda.setMaxWidth(440);
        ayuda.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        grid.add(lblCosto, 0, 7);                    grid.add(txtCosto, 1, 7);
        grid.add(new Label("Stock mínimo:"), 0, 8);  grid.add(txtMinimo, 1, 8);
        grid.add(ayuda, 0, 9, 2, 1);
        return new TextField[]{txtCosto, txtMinimo};
    }

    /**
     * Deja el casillero de código listo para usar con un lector de código de barras.
     *
     * El lector escribe el código y aprieta Enter. En una ventana de alta, Enter equivale a "Guardar",
     * así que sin esto intentaba guardar el artículo apenas se escaneaba, con el resto de los datos vacíos.
     * Ahora ese Enter solo pasa al casillero del nombre.
     *
     * Además, si ya estaba puesto el prefijo de la categoría ("1004-") y se escanea un código de fábrica,
     * quedaba "1004-7791234567890". Un código propio lleva 3 o 4 números después del guion; si son 8 o más,
     * es un código escaneado y se saca el prefijo.
     */
    private void prepararCodigoParaLector(TextField txtCodigo, TextField txtNombre, ComboBox<Categoria> cmbCategoria) {
        txtCodigo.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, tecla -> {
            if (tecla.getCode() != javafx.scene.input.KeyCode.ENTER) return;
            tecla.consume(); // que no dispare "Guardar"

            Categoria categoria = cmbCategoria.getValue();
            String texto = txtCodigo.getText() == null ? "" : txtCodigo.getText().trim();
            if (categoria != null && categoria.getPrefijoSku() != null) {
                String prefijo = categoria.getPrefijoSku() + "-";
                if (texto.startsWith(prefijo) && texto.substring(prefijo.length()).matches("\\d{8,}")) {
                    texto = texto.substring(prefijo.length());
                }
            }
            txtCodigo.setText(texto);
            txtNombre.requestFocus();
        });
    }

    /** "$ 3.000,00 (30 %)", "−$ 500,00 (−5 %)" o "Sin costo". */
    private String textoGanancia(Articulo articulo) {
        Double ganancia = articulo.getGanancia();
        if (ganancia == null) return "Sin costo";
        double precio = articulo.getPrecio() != null ? articulo.getPrecio() : 0;
        String porcentaje = precio > 0 ? " (" + Math.round(ganancia / precio * 100) + " %)" : "";
        // Sin centavos: en la tabla alcanza y así entra junto con el porcentaje
        String plata = com.nakel.frontend.util.FormatoMoneda.pesos(Math.round(Math.abs(ganancia))).replace(",00", "");
        return (ganancia < 0 ? "−" : "") + plata + porcentaje.replace("(-", "(−");
    }

    private void avisarErrorArticulo(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText((mensaje != null ? mensaje : "Ocurrió un error.")
                + "\n\nLos datos siguen en pantalla: corregí y volvé a guardar.");
        alerta.showAndWait();
    }

    private void mostrarDetalle(Articulo articulo) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Detalle del Artículo");
        alerta.setHeaderText(articulo.getNombre() + " (SKU: " + articulo.getCodigo() + ")");
        String info = "Precio: " + com.nakel.frontend.util.FormatoMoneda.pesos(articulo.getPrecio()) + "\n"
                + ("PRODUCCION_PROPIA".equals(articulo.getOrigen()) ? "Costo de producción: " : "Precio de compra: ")
                + (articulo.getCosto() != null ? com.nakel.frontend.util.FormatoMoneda.pesos(articulo.getCosto()) : "sin cargar") + "\n"
                + "Ganancia por unidad: " + textoGanancia(articulo) + "\n"
                + "Stock Actual: " + articulo.getStockActual() + " unidades\n"
                + "Stock mínimo: " + (articulo.getStockMinimo() != null ? articulo.getStockMinimo() : "-") + " unidades\n"
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

        ButtonType btnActualizar = new ButtonType("Actualizar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnActualizar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 20, 10, 10));

        // Campos de texto YA PRE-CARGADOS
        TextField txtCodigo = new TextField(articuloViejo.getCodigo());
        TextField txtNombre = new TextField(articuloViejo.getNombre());
        TextField txtPrecio = new TextField();
        com.nakel.frontend.util.FormatoMoneda.aplicar(txtPrecio);
        com.nakel.frontend.util.FormatoMoneda.poner(txtPrecio, articuloViejo.getPrecio()); // 25000.0 -> 25.000
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
        TextField[] extras = agregarCostoYMinimo(grid, cmbOrigenDialog, articuloViejo);
        prepararCodigoParaLector(txtCodigo, txtNombre, cmbCategoriaDialog);

        dialog.getDialogPane().setContent(grid);

        // Igual que en el alta: se guarda antes de cerrar y, si falla, la ventana queda abierta.
        // Se arma una COPIA con el mismo ID: el artículo de la tabla no se toca hasta que el servidor confirme.
        final boolean[] guardado = {false};
        Button botonActualizar = (Button) dialog.getDialogPane().lookupButton(btnActualizar);
        botonActualizar.addEventFilter(ActionEvent.ACTION, ev -> {
            Articulo copia = new Articulo();
            copia.setId(articuloViejo.getId());
            copia.setAlicuotaIva(articuloViejo.getAlicuotaIva());

            Articulo editado = leerFormularioArticulo(copia, txtCodigo, txtNombre, txtPrecio, txtStock,
                    cmbOrigenDialog, cmbCategoriaDialog, cmbMaterialDialog, extras[0], extras[1]);
            if (editado == null) {
                ev.consume();
                return;
            }

            if (!apiService.actualizarArticulo(editado)) { // 👈 PUT
                avisarErrorArticulo("No se pudo actualizar el artículo", apiService.getUltimoError());
                ev.consume();
                return;
            }
            guardado[0] = true;
        });

        dialog.setResultConverter(dialogButton -> null);
        dialog.showAndWait();

        if (guardado[0]) {
            cargarTabla(paginadorArticulos != null ? paginadorArticulos.getCurrentPageIndex() : 0); // Refresca catálogo
        }
    }

    // 🔥 SOLUCIÓN 3a: Borrado Seguro (Ataja errores de Base de Datos y Claves Foráneas)
    private void eliminarArticulo(Articulo articulo) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, "¿Seguro que desea dar de baja '" + articulo.getNombre() + "'?\n\nDeja de aparecer en el catálogo y en el mostrador. Las ventas viejas que lo incluyen se conservan.", ButtonType.YES, ButtonType.NO);
        alerta.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    boolean exito = apiService.eliminarArticuloDeBaseDeDatos(articulo.getId());
                    if (exito) {
                        cargarTabla(paginadorArticulos != null ? paginadorArticulos.getCurrentPageIndex() : 0);
                    } else {
                        Alert error = new Alert(Alert.AlertType.ERROR, "No se pudo dar de baja el artículo. Revisá que el sistema esté funcionando y probá de nuevo.");
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
                        "Error al cargar artículos: " + e.getMessage()
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


    @FXML
    public void imprimirReporteStock(ActionEvent event) {
        // Antes de armar la hoja se pregunta qué imprimir
        Integer[] eleccion = preguntarQueStockImprimir();
        if (eleccion == null) return; // canceló

        String rutaPdf = apiService.descargarReportePdf(eleccion[0]);

        if (rutaPdf != null) {
            try {
                File archivoPdf = new File(rutaPdf);
                if (archivoPdf.exists() && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(archivoPdf);
                }
            } catch (Exception e) {
                System.out.println("No se pudo abrir el PDF automáticamente.");
            }
        } else {
            Alert error = new Alert(Alert.AlertType.ERROR, "Error al generar el reporte de stock.");
            error.showAndWait();
        }
    }

    // =====================================================================
    //  ETIQUETAS CON CÓDIGO DE BARRAS
    // =====================================================================

    /** Pregunta qué artículos y cuántas etiquetas de cada uno, y abre el PDF para imprimir. */
    @FXML
    public void imprimirEtiquetas(ActionEvent event) {
        Articulo seleccionado = tablaArticulos.getSelectionModel().getSelectedItem();

        // --- 1. Qué artículos ---
        ToggleGroup grupoCuales = new ToggleGroup();
        RadioButton rbSeleccionado = new RadioButton(seleccionado != null
                ? "Solo el artículo elegido en la tabla: " + seleccionado.getNombre()
                : "Solo un artículo (primero elegilo en la tabla con un clic)");
        rbSeleccionado.setDisable(seleccionado == null);
        RadioButton rbCategoria = new RadioButton("Los de una categoría:");
        RadioButton rbTodos = new RadioButton("Todos los artículos");
        for (RadioButton rb : new RadioButton[]{rbSeleccionado, rbCategoria, rbTodos}) rb.setToggleGroup(grupoCuales);
        (seleccionado != null ? rbSeleccionado : rbCategoria).setSelected(true);

        ComboBox<Categoria> cmbCategoria = new ComboBox<>();
        cmbCategoria.getItems().addAll(parametrosService.obtenerCategorias());
        cmbCategoria.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Categoria c) { return c == null ? "" : c.getNombre(); }
            @Override public Categoria fromString(String s) { return null; }
        });
        if (!cmbCategoria.getItems().isEmpty()) cmbCategoria.getSelectionModel().select(0);
        cmbCategoria.disableProperty().bind(rbCategoria.selectedProperty().not());
        javafx.scene.layout.HBox filaCategoria = new javafx.scene.layout.HBox(8, rbCategoria, cmbCategoria);
        filaCategoria.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // --- 2. Cuántas de cada uno ---
        ToggleGroup grupoCopias = new ToggleGroup();
        RadioButton rbStock = new RadioButton("Una por cada unidad en stock (si hay 10, salen 10)");
        RadioButton rbUna = new RadioButton("Una sola por artículo");
        RadioButton rbFija = new RadioButton("Esta cantidad de cada uno:");
        for (RadioButton rb : new RadioButton[]{rbStock, rbUna, rbFija}) rb.setToggleGroup(grupoCopias);
        rbStock.setSelected(true);
        TextField txtCantidad = new TextField("5");
        txtCantidad.getStyleClass().add("text-field");
        txtCantidad.setPrefWidth(70);
        txtCantidad.disableProperty().bind(rbFija.selectedProperty().not());
        txtCantidad.textProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo != null && !nuevo.matches("\\d{0,3}")) txtCantidad.setText(viejo);
        });
        javafx.scene.layout.HBox filaFija = new javafx.scene.layout.HBox(8, rbFija, txtCantidad);
        filaFija.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // --- 3. Detalles ---
        CheckBox chkPrecio = new CheckBox("Mostrar el precio en la etiqueta");
        chkPrecio.setSelected(true);
        CheckBox chkSoloStock = new CheckBox("Saltear los artículos sin stock");
        chkSoloStock.setSelected(true);

        Label t1 = new Label("1. ¿De qué artículos?");
        Label t2 = new Label("2. ¿Cuántas etiquetas de cada uno?");
        Label t3 = new Label("3. Detalles");
        for (Label t : new Label[]{t1, t2, t3}) t.getStyleClass().add("etiqueta-formulario");
        Label ayuda = new Label("Sale una hoja A4 con 24 etiquetas de 70 x 37 mm. Sirve el papel autoadhesivo de esa medida, "
                + "o papel común para recortar por las líneas. Todas las etiquetas de un mismo artículo llevan el mismo código.");
        ayuda.getStyleClass().add("dato-ayuda");
        ayuda.setWrapText(true);
        ayuda.setMaxWidth(470);
        ayuda.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        javafx.scene.layout.VBox contenido = new javafx.scene.layout.VBox(9,
                t1, rbSeleccionado, filaCategoria, rbTodos,
                new Separator(), t2, rbStock, rbUna, filaFija,
                new Separator(), t3, chkPrecio, chkSoloStock, ayuda);
        contenido.getStyleClass().add("formulario-dialogo");
        contenido.setStyle("-fx-font-size: 14px;");
        contenido.setPadding(new javafx.geometry.Insets(6, 4, 6, 4));

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Imprimir etiquetas");
        dialogo.setHeaderText("Etiquetas con código de barras");
        ButtonType btnImprimir = new ButtonType("Imprimir", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getDialogPane().getButtonTypes().addAll(btnImprimir, btnCancelar);
        dialogo.getDialogPane().setContent(contenido);

        // Se arma el PDF antes de cerrar: si algo no cierra, la ventana queda abierta con lo elegido
        final String[] rutaPdf = {null};
        dialogo.getDialogPane().lookupButton(btnImprimir).addEventFilter(ActionEvent.ACTION, ev -> {
            StringBuilder parametros = new StringBuilder();
            if (rbSeleccionado.isSelected() && seleccionado != null) {
                parametros.append("idArticulo=").append(seleccionado.getId());
            } else if (rbCategoria.isSelected()) {
                if (cmbCategoria.getValue() == null) {
                    ev.consume();
                    new Alert(Alert.AlertType.WARNING, "Elegí la categoría.").showAndWait();
                    return;
                }
                parametros.append("categoriaId=").append(cmbCategoria.getValue().getId());
            }
            if (rbFija.isSelected()) {
                if (txtCantidad.getText().isBlank() || Integer.parseInt(txtCantidad.getText()) < 1) {
                    ev.consume();
                    new Alert(Alert.AlertType.WARNING, "Escribí cuántas etiquetas querés de cada artículo.").showAndWait();
                    return;
                }
                parametros.append("&copias=FIJA&cantidad=").append(txtCantidad.getText().trim());
            } else {
                parametros.append("&copias=").append(rbStock.isSelected() ? "STOCK" : "UNA");
            }
            parametros.append("&conPrecio=").append(chkPrecio.isSelected());
            parametros.append("&soloConStock=").append(chkSoloStock.isSelected());

            rutaPdf[0] = apiService.descargarEtiquetasPdf(parametros.toString());
            if (rutaPdf[0] == null) {
                ev.consume();
                Alert aviso = new Alert(Alert.AlertType.WARNING);
                aviso.setTitle("No se generaron las etiquetas");
                aviso.setHeaderText(null);
                aviso.setContentText(apiService.getUltimoError() != null ? apiService.getUltimoError() : "No se pudieron generar las etiquetas.");
                aviso.showAndWait();
            }
        });

        dialogo.showAndWait();
        if (rutaPdf[0] != null) {
            try {
                File archivoPdf = new File(rutaPdf[0]);
                if (archivoPdf.exists() && Desktop.isDesktopSupported()) Desktop.getDesktop().open(archivoPdf);
            } catch (Exception e) {
                new Alert(Alert.AlertType.INFORMATION, "Las etiquetas quedaron guardadas en:\n" + rutaPdf[0]).showAndWait();
            }
        }
    }

    /**
     * Ventanita con tres opciones para el reporte de stock.
     * Devuelve un arreglo de un elemento con el filtro (null = todos, 0 = con stock, N = más de N),
     * o null si se canceló.
     */
    private Integer[] preguntarQueStockImprimir() {
        ToggleGroup grupo = new ToggleGroup();
        RadioButton rbConStock = new RadioButton("Solo los que tienen stock (más de 0)");
        RadioButton rbTodos = new RadioButton("Todos los artículos (también los que están en 0)");
        RadioButton rbMasDe = new RadioButton("Solo los que tienen más de");
        rbConStock.setToggleGroup(grupo);
        rbTodos.setToggleGroup(grupo);
        rbMasDe.setToggleGroup(grupo);
        rbConStock.setSelected(true);

        TextField txtCantidad = new TextField("10");
        txtCantidad.getStyleClass().add("text-field");
        txtCantidad.setPrefWidth(70);
        txtCantidad.disableProperty().bind(rbMasDe.selectedProperty().not());
        // Solo números
        txtCantidad.textProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo != null && !nuevo.matches("\\d{0,5}")) txtCantidad.setText(viejo);
        });
        javafx.scene.layout.HBox filaMasDe = new javafx.scene.layout.HBox(8, rbMasDe, txtCantidad, new Label("unidades"));
        filaMasDe.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.layout.VBox opciones = new javafx.scene.layout.VBox(14, rbConStock, rbTodos, filaMasDe);
        opciones.getStyleClass().add("formulario-dialogo");
        opciones.setStyle("-fx-font-size: 14px;");
        opciones.setPadding(new javafx.geometry.Insets(6, 4, 6, 4));

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Imprimir stock");
        dialogo.setHeaderText("¿Qué artículos querés en la hoja?");
        ButtonType btnImprimir = new ButtonType("Imprimir", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getDialogPane().getButtonTypes().addAll(btnImprimir, btnCancelar);
        dialogo.getDialogPane().setContent(opciones);

        // Si eligió "más de" y dejó la cantidad vacía, no se cierra
        dialogo.getDialogPane().lookupButton(btnImprimir).addEventFilter(ActionEvent.ACTION, ev -> {
            if (rbMasDe.isSelected() && txtCantidad.getText().isBlank()) {
                ev.consume();
                new Alert(Alert.AlertType.WARNING, "Escribí la cantidad (por ejemplo 10).").showAndWait();
            }
        });

        if (dialogo.showAndWait().orElse(btnCancelar) != btnImprimir) return null;
        if (rbTodos.isSelected()) return new Integer[]{null};
        if (rbMasDe.isSelected()) return new Integer[]{Integer.parseInt(txtCantidad.getText().trim())};
        return new Integer[]{0};
    }
}