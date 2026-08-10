package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.Proveedor;
import com.nakel.frontend.service.ProveedorApiService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.kordamp.ikonli.javafx.FontIcon;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class ProveedorController {

    @FXML private TextField txtBuscarProveedor;
    @FXML private ComboBox<String> cmbCampoBusqueda;

    @FXML private TableView<Proveedor> tablaProveedores;
    @FXML private TableColumn<Proveedor, String> colRazonSocial;
    @FXML private TableColumn<Proveedor, String> colNombreContacto;
    @FXML private TableColumn<Proveedor, String> colTelefono;
    @FXML private TableColumn<Proveedor, String> colRubro;

    @FXML private Pagination paginadorProveedores;

    // 🔥 CAMBIO 1: Chau Saldo, Hola Comentarios
    @FXML private TableColumn<Proveedor, String> colComentarios;

    // 1. ACTIVAMOS LA COLUMNA DE ACCIONES
    @FXML private TableColumn<Proveedor, Void> colAcciones;

    private final ObservableList<Proveedor> masterData = FXCollections.observableArrayList();

    private final ProveedorApiService apiService = new ProveedorApiService();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        System.out.println("¡Módulo de Proveedores cargado con éxito!");

        tablaProveedores.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        configurarTabla();

        // 🔍 Opciones del buscador
        cmbCampoBusqueda.getItems().addAll(
                "Empresa",
                "Contacto"
        );
        cmbCampoBusqueda.setValue("Empresa");

        // 🔍 Cuando escribimos en el buscador
        txtBuscarProveedor.textProperty().addListener((observable, valorViejo, valorNuevo) -> {
            if (paginadorProveedores != null) {
                paginadorProveedores.setCurrentPageIndex(0);
            }

            cargarProveedoresEnTabla(0);
        });

        // 🔍 Cuando cambiamos Empresa / Contacto
        cmbCampoBusqueda.valueProperty().addListener((observable, valorViejo, valorNuevo) -> {
            if (paginadorProveedores != null) {
                paginadorProveedores.setCurrentPageIndex(0);
            }

            cargarProveedoresEnTabla(0);
        });

        // Conectamos la tabla con los datos
        tablaProveedores.setItems(masterData);

        // 📄 Configuración del paginador
        if (paginadorProveedores != null) {
            paginadorProveedores.setPageFactory(paginaIndex -> {
                cargarProveedoresEnTabla(paginaIndex);
                return new javafx.scene.layout.VBox();
            });
        } else {
            cargarProveedoresEnTabla(0);
        }
    }

    private void configurarTabla() {
        colRazonSocial.setCellValueFactory(new PropertyValueFactory<>("razonSocial"));
        colNombreContacto.setCellValueFactory(new PropertyValueFactory<>("nombreContacto"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colRubro.setCellValueFactory(new PropertyValueFactory<>("rubro"));

        // 🔥 CAMBIO 2: Enlazamos la columna con el campo "comentarios" del modelo
        colComentarios.setCellValueFactory(new PropertyValueFactory<>("comentarios"));

        // 2. LA FÁBRICA DE BOTONES CON IKONLI
        javafx.util.Callback<TableColumn<Proveedor, Void>, javafx.scene.control.TableCell<Proveedor, Void>> cellFactory = new javafx.util.Callback<>() {
            @Override
            public javafx.scene.control.TableCell<Proveedor, Void> call(final TableColumn<Proveedor, Void> param) {
                return new javafx.scene.control.TableCell<>() {

                    private final Button btnVer = new Button("", new FontIcon("fas-eye"));
                    private final Button btnEditar = new Button("", new FontIcon("fas-pen"));
                    private final Button btnEliminar = new Button("", new FontIcon("fas-trash"));
                    private final javafx.scene.layout.HBox panelAcciones = new javafx.scene.layout.HBox(5, btnVer, btnEditar, btnEliminar);

                    {
                        panelAcciones.setAlignment(javafx.geometry.Pos.CENTER);
                        btnVer.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                        btnEditar.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                        btnEliminar.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

                        btnVer.setOnAction(e -> mostrarDetalle(getTableView().getItems().get(getIndex())));
                        btnEditar.setOnAction(e -> editarProveedor(getTableView().getItems().get(getIndex())));
                        btnEliminar.setOnAction(e -> eliminarProveedor(getTableView().getItems().get(getIndex())));
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

    private void cargarProveedoresEnTabla(int numeroPagina) {
        String textoBusqueda = txtBuscarProveedor.getText() == null
                ? ""
                : txtBuscarProveedor.getText().trim();

        String campoBusqueda = cmbCampoBusqueda.getValue() == null
                ? "Empresa"
                : cmbCampoBusqueda.getValue();

        String json;

        if (textoBusqueda.isEmpty()) {
            // 📋 Sin búsqueda: paginación normal
            json = apiService.obtenerProveedores(numeroPagina, 20);
        } else {
            // 🔍 Búsqueda global en toda la base de datos
            json = apiService.buscarProveedores(
                    textoBusqueda,
                    campoBusqueda,
                    numeroPagina,
                    20
            );
        }

        if (json != null && !json.equals("[]") && !json.isEmpty()) {
            try {
                com.google.gson.JsonElement elementoParseado = JsonParser.parseString(json);
                JsonArray arregloProveedores;

                if (elementoParseado.isJsonObject()) {
                    JsonObject respuestaServidor = elementoParseado.getAsJsonObject();

                    if (respuestaServidor.has("totalPages") && paginadorProveedores != null) {
                        int totalPaginas = respuestaServidor.get("totalPages").getAsInt();

                        paginadorProveedores.setPageCount(
                                totalPaginas == 0 ? 1 : totalPaginas
                        );
                    }

                    arregloProveedores = respuestaServidor.getAsJsonArray("content");

                } else if (elementoParseado.isJsonArray()) {
                    arregloProveedores = elementoParseado.getAsJsonArray();

                    if (paginadorProveedores != null) {
                        paginadorProveedores.setPageCount(1);
                    }

                } else {
                    throw new RuntimeException("Formato JSON no reconocido");
                }

                Type tipoLista = new TypeToken<List<Proveedor>>() {}.getType();

                List<Proveedor> listaBackend =
                        gson.fromJson(arregloProveedores, tipoLista);

                if (listaBackend != null) {
                    masterData.setAll(listaBackend);
                }

            } catch (Exception e) {
                System.out.println("❌ Error al cargar proveedores: " + e.getMessage());
                e.printStackTrace();
            }

        } else {
            masterData.clear();

            if (paginadorProveedores != null) {
                paginadorProveedores.setPageCount(1);
            }
        }
    }

    @FXML
    public void buscarProveedor(ActionEvent event) {
        if (paginadorProveedores != null) {
            paginadorProveedores.setCurrentPageIndex(0);
        }

        cargarProveedoresEnTabla(0);
    }

    @FXML
    public void limpiarBusqueda() {
        txtBuscarProveedor.clear();
        cmbCampoBusqueda.setValue("Empresa");

        if (paginadorProveedores != null) {
            paginadorProveedores.setCurrentPageIndex(0);
        }

        cargarProveedoresEnTabla(0);
    }

    @FXML
    public void abrirModalNuevoProveedor(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/nuevo-proveedor-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage modalStage = new javafx.stage.Stage();
            modalStage.setTitle("Alta de Proveedor");
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.setResizable(false);
            modalStage.showAndWait();

            cargarProveedoresEnTabla(paginadorProveedores != null ? paginadorProveedores.getCurrentPageIndex() : 0);
        } catch (Exception e) {
            System.err.println("Error al abrir el Pop-up de Proveedores.");
            e.printStackTrace();
        }
    }

    // --- 3. LOS MÉTODOS DE ACCIÓN (OJITO, LÁPIZ Y TACHO) ---

    private void mostrarDetalle(Proveedor proveedor) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Detalle del Proveedor");
        alerta.setHeaderText(proveedor.getRazonSocial() + " (" + proveedor.getRubro() + ")");

        String cuit = (proveedor.getCuit() != null && !proveedor.getCuit().isEmpty()) ? proveedor.getCuit() : "N/A";
        String email = (proveedor.getEmail() != null && !proveedor.getEmail().isEmpty()) ? proveedor.getEmail() : "N/A";
        String telefono = (proveedor.getTelefono() != null && !proveedor.getTelefono().isEmpty()) ? proveedor.getTelefono() : "N/A";
        String contacto = (proveedor.getNombreContacto() != null && !proveedor.getNombreContacto().isEmpty()) ? proveedor.getNombreContacto() : "N/A";

        // Protegemos el comentario por si viene vacío o nulo
        String comentarios = (proveedor.getComentarios() != null && !proveedor.getComentarios().trim().isEmpty()) ? proveedor.getComentarios() : "Sin anotaciones.";

        // Armamos la tarjeta completa para el Ojito
        String info = "🏢 Contacto: " + contacto + "\n"
                + "📞 Teléfono: " + telefono + "\n"
                + "📧 Email: " + email + "\n"
                + "📄 CUIT: " + cuit + "\n\n"
                + "--- ESTADO DE CUENTA ---\n"
                + "🟢 Saldo a Favor: $" + proveedor.getSaldoFavor() + "\n"
                + "🔴 Saldo en Contra: $" + proveedor.getSaldoContra() + "\n\n"
                + "--- COMENTARIOS ---\n"
                + comentarios;

        // 🔥 ACÁ ESTÁ EL CAMBIO: En vez de un texto simple, creamos un TextArea
        javafx.scene.control.TextArea areaTexto = new javafx.scene.control.TextArea(info);
        areaTexto.setEditable(false);
        areaTexto.setWrapText(true); // Esto hace que baje de renglón automáticamente
        areaTexto.setMaxWidth(Double.MAX_VALUE);
        areaTexto.setMaxHeight(Double.MAX_VALUE);
        areaTexto.setPrefRowCount(10); // Le damos unos 10 renglones de alto para que se vea bien

        // Le enchufamos el TextArea a la alerta
        alerta.getDialogPane().setContent(areaTexto);

        alerta.showAndWait();
    }

    private void eliminarProveedor(Proveedor proveedor) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar Eliminación");
        alerta.setHeaderText("Vas a eliminar al proveedor: " + proveedor.getRazonSocial());
        alerta.setContentText("¿Estás completamente seguro? Esta acción no se puede deshacer.");

        Optional<ButtonType> resultado = alerta.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                apiService.eliminarProveedorDeBaseDeDatos(proveedor.getId());
                cargarProveedoresEnTabla(paginadorProveedores != null ? paginadorProveedores.getCurrentPageIndex() : 0);
            } catch (Exception e) {
                Alert error = new Alert(Alert.AlertType.ERROR, "No se pudo eliminar: " + e.getMessage());
                error.showAndWait();
            }
        }
    }

    private void editarProveedor(Proveedor proveedor) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/nuevo-proveedor-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            NuevoProveedorController controller = loader.getController();
            controller.cargarDatosParaEditar(proveedor);

            javafx.stage.Stage modalStage = new javafx.stage.Stage();
            modalStage.setTitle("Editar Proveedor");
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.setResizable(false);
            modalStage.showAndWait();

            cargarProveedoresEnTabla(paginadorProveedores != null ? paginadorProveedores.getCurrentPageIndex() : 0);
        } catch (Exception e) {
            System.err.println("Error al abrir el editor de Proveedores.");
            e.printStackTrace();
        }
    }
}