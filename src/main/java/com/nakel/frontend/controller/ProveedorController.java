package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.Proveedor;
import com.nakel.frontend.service.ProveedorApiService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import org.kordamp.ikonli.javafx.FontIcon;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class ProveedorController {

    @FXML private TextField txtBuscarProveedor;

    @FXML private TableView<Proveedor> tablaProveedores;
    @FXML private TableColumn<Proveedor, String> colRazonSocial;
    @FXML private TableColumn<Proveedor, String> colNombreContacto;
    @FXML private TableColumn<Proveedor, String> colTelefono;
    @FXML private TableColumn<Proveedor, String> colRubro;
    @FXML private TableColumn<Proveedor, BigDecimal> colSaldo;

    // 1. ACTIVAMOS LA COLUMNA DE ACCIONES
    @FXML private TableColumn<Proveedor, Void> colAcciones;

    private final ProveedorApiService apiService = new ProveedorApiService();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        System.out.println("¡Módulo de Proveedores cargado con éxito!");

        tablaProveedores.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        configurarTabla();
        cargarProveedoresEnTabla();
    }

    private void configurarTabla() {
        colRazonSocial.setCellValueFactory(new PropertyValueFactory<>("razonSocial"));
        colNombreContacto.setCellValueFactory(new PropertyValueFactory<>("nombreContacto"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colRubro.setCellValueFactory(new PropertyValueFactory<>("rubro"));
        colSaldo.setCellValueFactory(new PropertyValueFactory<>("saldoPendiente"));

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

    private void cargarProveedoresEnTabla() {
        String json = apiService.obtenerProveedores();

        if (json != null && !json.equals("[]") && !json.isEmpty()) {
            try {
                // A diferencia de Clientes, acá el Backend nos devuelve la lista directa
                Type tipoLista = new TypeToken<List<Proveedor>>(){}.getType();
                List<Proveedor> listaProveedores = gson.fromJson(json, tipoLista);

                ObservableList<Proveedor> datosObservable = FXCollections.observableArrayList(listaProveedores);
                tablaProveedores.setItems(datosObservable);

                System.out.println("✅ Tabla cargada con " + listaProveedores.size() + " proveedores.");
            } catch (Exception e) {
                System.out.println("❌ Error al convertir el JSON a la tabla: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Si está vacío, limpiamos la tabla para que no queden datos fantasma
            tablaProveedores.setItems(FXCollections.observableArrayList());
            System.out.println("⚠️ La base de datos está vacía o el JSON vino nulo.");
        }
    }

    @FXML
    public void buscarProveedor(ActionEvent event) {
        String textoBusqueda = txtBuscarProveedor.getText();
        System.out.println("Buscando en la base de datos: " + textoBusqueda);
        // Cuando activemos el endpoint del buscador en el backend, conectamos esto.
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

            cargarProveedoresEnTabla();
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

        String info = "🏢 Contacto: " + contacto + "\n"
                + "📞 Teléfono: " + telefono + "\n"
                + "📧 Email: " + email + "\n"
                + "📄 CUIT: " + cuit + "\n\n"
                + "--- ESTADO DE CUENTA ---\n"
                + "💰 Saldo Cta. Cte.: $" + proveedor.getSaldoPendiente() + "\n";

        alerta.setContentText(info);
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
                cargarProveedoresEnTabla(); // Recargamos la tabla automáticamente
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

            // Pasa los datos al controlador del modal antes de abrirlo
            NuevoProveedorController controller = loader.getController();
            controller.cargarDatosParaEditar(proveedor);

            javafx.stage.Stage modalStage = new javafx.stage.Stage();
            modalStage.setTitle("Editar Proveedor");
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.setResizable(false);
            modalStage.showAndWait();

            cargarProveedoresEnTabla(); // Al cerrar, recarga la tabla
        } catch (Exception e) {
            System.err.println("Error al abrir el editor de Proveedores.");
            e.printStackTrace();
        }
    }
}