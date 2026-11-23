package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.Insumo;
import com.nakel.frontend.model.Categoria;
import com.nakel.frontend.model.Material;
import com.nakel.frontend.service.InsumoApiService;
import com.nakel.frontend.service.ParametrosApiService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class InsumoController {

    // --- FILTROS DE BÚSQUEDA ---
    @FXML private TextField txtBuscarInsumo;
    @FXML private ComboBox<String> cmbFiltroUnidad; // 🔥 Quedó como String para "Metros", "Unidades", etc.
    @FXML private ComboBox<Material> cmbFiltroMaterial;

    // --- TABLA Y COLUMNAS ---
    @FXML private TableView<Insumo> tablaInsumos;
    @FXML private TableColumn<Insumo, Long> colId;
    @FXML private TableColumn<Insumo, String> colDescripcion;
    @FXML private TableColumn<Insumo, String> colCategoria;
    @FXML private TableColumn<Insumo, String> colUnidad;
    @FXML private TableColumn<Insumo, BigDecimal> colCosto;
    @FXML private TableColumn<Insumo, Void> colAcciones;

    private final ObservableList<Insumo> listaInsumos = FXCollections.observableArrayList();
    private final Gson gson = new Gson();

    // --- SERVICIOS ---
    private final InsumoApiService apiService = new InsumoApiService();
    private final ParametrosApiService parametrosService = new ParametrosApiService();


    @FXML private Pagination paginadorInsumos;
    @FXML private Label lblTituloTotalInsumos;
    @FXML private Label lblTotalInsumos;

    /** Contador de abajo: total de insumos, o cuántos encontró el filtro. */
    private void mostrarTotal(long cantidad) {
        boolean filtrando = (txtBuscarInsumo.getText() != null && !txtBuscarInsumo.getText().isBlank())
                || cmbFiltroUnidad.getValue() != null || cmbFiltroMaterial.getValue() != null;
        lblTituloTotalInsumos.setText(filtrando ? "Insumos encontrados:" : "Total de insumos:");
        lblTotalInsumos.setText(String.valueOf(cantidad));
    }


    @FXML
    public void initialize() {
        System.out.println("¡Módulo de Gestión de Insumos cargado con éxito!");

        // F3 = ir al buscador (acá se filtra solo mientras se escribe)
        com.nakel.frontend.util.Atajos.buscarConF3(txtBuscarInsumo, null);

        tablaInsumos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        configurarColumnasTabla();

        // 1. Arreglamos el texto fantasma de los combos
        forzarTextoFantasma(cmbFiltroUnidad, "Filtrar Unidad...");
        forzarTextoFantasma(cmbFiltroMaterial, "Filtrar Material...");

        // 2. Cargamos los combos con los datos reales
        try {
            cmbFiltroUnidad.getItems().addAll("Metros", "Unidades", "Horas");
            cmbFiltroMaterial.getItems().addAll(parametrosService.obtenerMateriales());
        } catch (Exception e) {
            System.out.println("⚠️ Aviso: No se pudieron cargar los materiales desde la base de datos.");
        }

        // 3. Los filtros los resuelve el BACKEND sobre todos los insumos.
        //    Antes se filtraban acá los 20 de la página cargada: "Horas" no encontraba nada porque
        //    la mano de obra estaba en la última página.
        // 4. Al cambiar un filtro se vuelve a la página 1 y se le pide al backend la lista filtrada
        Runnable aplicarFiltros = () -> {
            if (paginadorInsumos.getCurrentPageIndex() != 0) {
                paginadorInsumos.setCurrentPageIndex(0); // esto ya dispara la carga
            } else {
                cargarInsumosDesdeBackend(0);
            }
        };

        // 5. El texto espera un instante a que se termine de escribir; los combos filtran al toque
        javafx.animation.PauseTransition esperaTexto = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        esperaTexto.setOnFinished(e -> aplicarFiltros.run());
        txtBuscarInsumo.textProperty().addListener((obs, oldV, newV) -> esperaTexto.playFromStart());
        cmbFiltroUnidad.valueProperty().addListener((obs, oldV, newV) -> aplicarFiltros.run());
        cmbFiltroMaterial.valueProperty().addListener((obs, oldV, newV) -> aplicarFiltros.run());

        // 6. Conectamos la tabla
        javafx.collections.transformation.SortedList<Insumo> datosOrdenados = new javafx.collections.transformation.SortedList<>(listaInsumos);
        datosOrdenados.comparatorProperty().bind(tablaInsumos.comparatorProperty());
        tablaInsumos.setItems(datosOrdenados);

        // 7. 🔥 CONFIGURAMOS EL PAGINADOR NATIVO
        paginadorInsumos.setPageFactory(paginaIndex -> {
            // Cada vez que tocan un número, llamamos al backend pidiendo esa página
            cargarInsumosDesdeBackend(paginaIndex);

            // JavaFX exige devolver un "nodo" visual acá, le pasamos una cajita invisible
            return new javafx.scene.layout.VBox();
        });
    }

    // 🔥 ACÁ ESTÁN TUS MÉTODOS RECUPERADOS 🔥
    private void configurarColumnasTabla() {
        if (colId != null) colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colDescripcion != null) colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcionCompleta"));

        if (colCategoria != null) {
            colCategoria.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleStringProperty(
                            cellData.getValue().getCategoria() != null ? cellData.getValue().getCategoria().getNombre() : "Sin Categoría"
                    )
            );
        }

        if (colCosto != null) {
            colCosto.setCellValueFactory(cellData ->
                    new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getCostoCalculado())
            );
            colCosto.setCellFactory(col -> new javafx.scene.control.TableCell<Insumo, java.math.BigDecimal>() {
                @Override
                protected void updateItem(java.math.BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(com.nakel.frontend.util.FormatoMoneda.pesos(item));
                    }
                }
            });
        }

        if (colUnidad != null) {
            colUnidad.setCellValueFactory(cellData -> {
                String regla = cellData.getValue().getCategoria() != null
                        ? cellData.getValue().getCategoria().getTipoMedicion()
                        : "UNIDAD";
                String unidad = "Unidades";
                if ("SUPERFICIE".equals(regla)) unidad = "Metros";
                if ("TIEMPO".equals(regla)) unidad = "Horas";
                return new javafx.beans.property.SimpleStringProperty(unidad);
            });
        }

        if (colAcciones != null) {
            javafx.util.Callback<TableColumn<Insumo, Void>, TableCell<Insumo, Void>> cellFactory = new javafx.util.Callback<>() {
                @Override
                public TableCell<Insumo, Void> call(final TableColumn<Insumo, Void> param) {
                    return new TableCell<>() {
                        private final Button btnVer = new Button("", new FontIcon("fas-eye"));
                        private final Button btnEditar = new Button("", new FontIcon("fas-pen"));
                        private final Button btnEliminar = new Button("", new FontIcon("fas-trash"));
                        private final HBox panelAcciones = new HBox(5, btnVer, btnEditar, btnEliminar);

                        {
                            panelAcciones.setAlignment(Pos.CENTER);
                            // Mismo aspecto, tachito rojo y cartelitos de ayuda que en el resto de las tablas
                            com.nakel.frontend.util.Acciones.ver(btnVer, "Ver el detalle");
                            com.nakel.frontend.util.Acciones.editar(btnEditar, "Editar el insumo");
                            com.nakel.frontend.util.Acciones.eliminar(btnEliminar, "Eliminar el insumo");

                            btnVer.setOnAction(e -> mostrarDetalle(getTableView().getItems().get(getIndex())));
                            btnEditar.setOnAction(e -> editarInsumo(getTableView().getItems().get(getIndex())));
                            btnEliminar.setOnAction(e -> eliminarInsumo(getTableView().getItems().get(getIndex())));
                        }

                        @Override
                        public void updateItem(Void item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                setGraphic(null);
                            } else {
                                setGraphic(panelAcciones);
                            }
                        }
                    };
                }
            };
            colAcciones.setCellFactory(cellFactory);
        }
    }

    // 🔥 AHORA RECIBE EL NÚMERO DE PÁGINA
    private void cargarInsumosDesdeBackend(int numeroPagina) {
        listaInsumos.clear();

        // Le pedimos al servicio la página exacta (de a 20 por página), ya filtrada por el backend
        String unidad = cmbFiltroUnidad.getValue();
        String tipoMedicion = unidad == null ? null
                : switch (unidad) {
                    case "Metros" -> "SUPERFICIE";
                    case "Horas" -> "TIEMPO";
                    default -> "UNIDAD";
                };
        Material material = cmbFiltroMaterial.getValue();
        String json = apiService.obtenerInsumosFiltrados(numeroPagina, 20, txtBuscarInsumo.getText(),
                tipoMedicion, material != null ? material.getId() : null);

        if (json != null && !json.equals("[]") && !json.isEmpty()) {
            try {
                JsonObject respuestaServidor = JsonParser.parseString(json).getAsJsonObject();

                // 🧠 MAGIA: Leemos cuántas páginas totales hay en la BD y actualizamos la vista
                if (respuestaServidor.has("totalPages")) {
                    int totalPaginas = respuestaServidor.get("totalPages").getAsInt();
                    // Si no hay nada, mostramos 1 página por defecto
                    if (paginadorInsumos != null) {
                        paginadorInsumos.setPageCount(totalPaginas == 0 ? 1 : totalPaginas);
                    }
                }

                JsonArray arregloInsumos = respuestaServidor.has("content")
                        ? respuestaServidor.getAsJsonArray("content")
                        : JsonParser.parseString(json).getAsJsonArray();

                Type tipoLista = new TypeToken<List<Insumo>>(){}.getType();
                List<Insumo> deLaBaseDeDatos = gson.fromJson(arregloInsumos, tipoLista);

                listaInsumos.setAll(deLaBaseDeDatos);
                // El total lo informa el backend (cuenta todas las páginas, no solo la que se ve)
                mostrarTotal(respuestaServidor.has("totalElements")
                        ? respuestaServidor.get("totalElements").getAsLong() : deLaBaseDeDatos.size());
                System.out.println("✅ Tabla cargada con " + deLaBaseDeDatos.size() + " insumos (Página " + (numeroPagina + 1) + ").");
            } catch (Exception e) {
                System.err.println("❌ Error al convertir el JSON a la tabla.");
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ La base de datos de insumos está vacía o el JSON vino nulo.");
            mostrarTotal(0);
        }
    }

    @FXML
    public void abrirModalNuevoInsumo(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/nuevo-insumo-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            Stage modalStage = new Stage();
            modalStage.setTitle("Cargar Insumo");
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.setResizable(false);
            modalStage.showAndWait();

            cargarInsumosDesdeBackend(paginadorInsumos.getCurrentPageIndex());
        } catch (Exception e) {
            System.err.println("Error al abrir el Pop-up de Insumos.");
            e.printStackTrace();
        }
    }

    @FXML
    public void limpiarFiltros(ActionEvent event) {
        if (txtBuscarInsumo != null) txtBuscarInsumo.clear();
        if (cmbFiltroUnidad != null) cmbFiltroUnidad.setValue(null);
        if (cmbFiltroMaterial != null) cmbFiltroMaterial.setValue(null);
    }

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

    private void mostrarDetalle(Insumo insumo) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Detalle del Insumo");
        alerta.setHeaderText(insumo.getNombre() + " (Cod: " + insumo.getId() + ")");

        String nombreCategoria = insumo.getCategoria() != null ? insumo.getCategoria().getNombre() : "N/A";
        String tipoMedicion = insumo.getCategoria() != null ? insumo.getCategoria().getTipoMedicion() : "";

        String info = "Categoría: " + nombreCategoria + "\n"
                + "Costo Total: " + com.nakel.frontend.util.FormatoMoneda.pesos(insumo.getCostoTotal()) + "\n";

        if ("SUPERFICIE".equals(tipoMedicion)) {
            String nombreMaterial = insumo.getMaterial() != null ? insumo.getMaterial().getNombre() : "Sin asignar";
            info += "Material: " + nombreMaterial + "\n\n";
            info += "Dimensiones Originales: " + insumo.getAnchoLoteCm() + "x" + insumo.getLargoLoteCm() + " cm\n";
            int areaOriginal = (insumo.getAnchoLoteCm() != null && insumo.getLargoLoteCm() != null) ? insumo.getAnchoLoteCm() * insumo.getLargoLoteCm() : 0;
            info += "Área Original Total: " + areaOriginal + " cm²\n";
            info += "Área Actual Disponible: " + (insumo.getAreaActualCm2() != null ? insumo.getAreaActualCm2() : 0) + " cm²\n";
        } else if ("UNIDAD".equals(tipoMedicion)) {
            info += "\nCantidad Original (Lote): " + insumo.getCantidadLote() + " unidades\n";
            info += "Stock Actual: " + insumo.getCantidadActual() + " unidades\n";
        } else if ("SERVICIO".equals(tipoMedicion) || "TIEMPO".equals(tipoMedicion)) {
            info += "\nCosto por Hora de Confección: " + com.nakel.frontend.util.FormatoMoneda.pesos(insumo.getCostoTotal()) + "\n";
        }

        alerta.setContentText(info);
        alerta.showAndWait();
    }

    private void editarInsumo(Insumo insumo) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/nuevo-insumo-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            // 🔥 ESTAS DOS LÍNEAS ESTABAN COMENTADAS. ¡ACTIVÁLAS!
            NuevoInsumoController controller = loader.getController();
            controller.cargarDatosParaEditar(insumo);

            Stage modalStage = new Stage();
            modalStage.setTitle("Editar Insumo");
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.setResizable(false);
            modalStage.showAndWait();

            // Refrescamos la tabla en la misma página
            cargarInsumosDesdeBackend(paginadorInsumos.getCurrentPageIndex());
        } catch (Exception e) {
            System.err.println("Error al abrir el editor de Insumos.");
            e.printStackTrace();
        }
    }

    private void eliminarInsumo(Insumo insumo) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar Eliminación");
        alerta.setHeaderText("Vas a eliminar el insumo: " + insumo.getNombre());
        alerta.setContentText("¿Estás completamente seguro? Esta acción no se puede deshacer.");

        Optional<ButtonType> resultado = alerta.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                apiService.eliminarInsumoDeBaseDeDatos(insumo.getId());
                cargarInsumosDesdeBackend(paginadorInsumos.getCurrentPageIndex());
            } catch (Exception e) {
                Alert error = new Alert(Alert.AlertType.ERROR, "No se pudo eliminar: " + e.getMessage());
                error.showAndWait();
            }
        }
    }
}