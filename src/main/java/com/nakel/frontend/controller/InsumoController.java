package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.Insumo;
import com.nakel.frontend.service.InsumoApiService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class InsumoController {

    @FXML private TextField txtBuscarInsumo;

    @FXML private TableView<Insumo> tablaInsumos;
    @FXML private TableColumn<Insumo, Long> colId;
    @FXML private TableColumn<Insumo, String> colDescripcion;
    @FXML private TableColumn<Insumo, String> colCategoria;
    @FXML private TableColumn<Insumo, String> colUnidad;
    @FXML private TableColumn<Insumo, BigDecimal> colCosto;

    // 🔥 LA NUEVA COLUMNA DE ACCIONES
    @FXML private TableColumn<Insumo, Void> colAcciones;

    private final ObservableList<Insumo> listaInsumos = FXCollections.observableArrayList();
    private final Gson gson = new Gson();

    // 🔥 EL SERVICIO REAL DESBLOQUEADO
    private final InsumoApiService apiService = new InsumoApiService();

    @FXML
    public void initialize() {
        System.out.println("¡Módulo de Gestión de Insumos cargado con éxito!");

        tablaInsumos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        configurarColumnasTabla();

        tablaInsumos.setItems(listaInsumos);

        cargarInsumosDesdeBackend();
    }

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
        if (colCosto != null) colCosto.setCellValueFactory(new PropertyValueFactory<>("costoTotal"));

        // Asignamos una unidad de medida por defecto según la regla (Opcional, hasta que el modelo lo traiga directo)
        if (colUnidad != null) {
            colUnidad.setCellValueFactory(cellData -> {
                // Entramos a la categoría para sacar el tipo de medición (con validación por las dudas)
                String regla = cellData.getValue().getCategoria() != null
                        ? cellData.getValue().getCategoria().getTipoMedicion()
                        : "UNIDAD";
                String unidad = "Unidades";
                if ("SUPERFICIE".equals(regla)) unidad = "Metros";
                if ("TIEMPO".equals(regla)) unidad = "Horas";
                return new javafx.beans.property.SimpleStringProperty(unidad);
            });
        }

        // 🎨 FÁBRICA DE BOTONES PARA LA COLUMNA ACCIONES
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
                            btnVer.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                            btnEditar.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                            btnEliminar.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

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

    private void cargarInsumosDesdeBackend() {
        listaInsumos.clear();
        String json = apiService.obtenerInsumos();

        if (json != null && !json.equals("[]") && !json.isEmpty()) {
            try {
                // Parseo inteligente igual que en Cliente
                JsonObject respuestaServidor = JsonParser.parseString(json).getAsJsonObject();

                // Asumimos que Spring Boot devuelve un formato paginado con "content"
                JsonArray arregloInsumos = respuestaServidor.has("content")
                        ? respuestaServidor.getAsJsonArray("content")
                        : JsonParser.parseString(json).getAsJsonArray(); // Fallback por si devuelve un array directo

                Type tipoLista = new TypeToken<List<Insumo>>(){}.getType();
                List<Insumo> deLaBaseDeDatos = gson.fromJson(arregloInsumos, tipoLista);

                listaInsumos.setAll(deLaBaseDeDatos);
                System.out.println("✅ Tabla cargada con " + deLaBaseDeDatos.size() + " insumos.");

            } catch (Exception e) {
                System.err.println("❌ Error al convertir el JSON a la tabla.");
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ La base de datos de insumos está vacía o el JSON vino nulo.");
        }
    }

    @FXML
    public void buscarInsumo(ActionEvent event) {
        String textoBusqueda = txtBuscarInsumo.getText().trim();
        // TODO: Conectar con apiService.buscarInsumos(textoBusqueda)
        System.out.println("Buscando insumo: " + textoBusqueda);
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

            cargarInsumosDesdeBackend();
        } catch (Exception e) {
            System.err.println("Error al abrir el Pop-up de Insumos.");
            e.printStackTrace();
        }
    }

    // --- ACCIONES DE LOS BOTONES ---

    private void mostrarDetalle(Insumo insumo) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Detalle del Insumo");
        alerta.setHeaderText(insumo.getNombre() + " (Cod: " + insumo.getId() + ")");

        // Extraemos los datos de la categoría de forma segura
        String nombreCategoria = insumo.getCategoria() != null ? insumo.getCategoria().getNombre() : "N/A";
        String tipoMedicion = insumo.getCategoria() != null ? insumo.getCategoria().getTipoMedicion() : "";

        String info = "Categoría: " + nombreCategoria + "\n"
                + "Costo Total: $" + insumo.getCostoTotal() + "\n\n";

        if ("SUPERFICIE".equals(tipoMedicion)) {
            info += "Dimensiones: " + insumo.getAnchoCm() + "x" + insumo.getLargoCm() + " cm\n";
        } else if ("UNIDAD".equals(tipoMedicion)) {
            info += "Cantidad original: " + insumo.getCantidad() + " unidades\n";
        }

        alerta.setContentText(info);
        alerta.showAndWait();
    }

    private void editarInsumo(Insumo insumo) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/nuevo-insumo-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            // 🔥 Acá levantás el controlador y le pasás el objeto
            NuevoInsumoController controller = loader.getController();
            controller.cargarDatosParaEditar(insumo);

            Stage modalStage = new Stage();
            modalStage.setTitle("Editar Insumo");
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.setResizable(false);
            modalStage.showAndWait();

            cargarInsumosDesdeBackend();
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
                cargarInsumosDesdeBackend();
            } catch (Exception e) {
                Alert error = new Alert(Alert.AlertType.ERROR, "No se pudo eliminar: " + e.getMessage());
                error.showAndWait();
            }
        }
    }
}