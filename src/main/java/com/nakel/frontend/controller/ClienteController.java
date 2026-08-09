package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.Cliente;
import com.nakel.frontend.service.ClienteApiService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.kordamp.ikonli.javafx.FontIcon;
import java.util.Optional;

import java.lang.reflect.Type;
import java.util.List;

public class ClienteController {

    @FXML private TextField txtBuscarCliente;

    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, Long> colCod;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colDni;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colEmail;

    // 1. ACTIVAMOS LA COLUMNA DE ACCIONES (Tipo Void porque no lee texto, dibuja botones)
    @FXML private TableColumn<Cliente, Void> colAcciones;

    @FXML private Pagination paginadorClientes;

    private final ClienteApiService apiService = new ClienteApiService();
    private final Gson gson = new Gson();

    // Declarás la lista maestra que contiene los datos puros
    private final ObservableList<Cliente> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        System.out.println("¡Módulo de Clientes cargado con éxito!");

        tablaClientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        configurarTabla();

        // 🔍 Buscar automáticamente mientras se escribe
        txtBuscarCliente.textProperty().addListener((observable, valorViejo, valorNuevo) -> {

            if (paginadorClientes != null) {
                paginadorClientes.setCurrentPageIndex(0);
            }

            cargarClientesEnTabla(0);
        });

        // 📋 La tabla muestra directamente los resultados
        // que devuelve el backend.
        tablaClientes.setItems(masterData);

        // 📄 Configuración del paginador
        if (paginadorClientes != null) {
            paginadorClientes.setPageFactory(paginaIndex -> {
                cargarClientesEnTabla(paginaIndex);
                return new javafx.scene.layout.VBox();
            });
        } else {
            cargarClientesEnTabla(0);
        }
    }

    private void configurarTabla() {
        colCod.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colDni.setCellValueFactory(new PropertyValueFactory<>("cuit"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        //colAcciones.setPrefWidth(150);

        // 2. LA FÁBRICA DE BOTONES CON IKONLI (REALES)
        javafx.util.Callback<TableColumn<Cliente, Void>, javafx.scene.control.TableCell<Cliente, Void>> cellFactory = new javafx.util.Callback<>() {
            @Override
            public javafx.scene.control.TableCell<Cliente, Void> call(final TableColumn<Cliente, Void> param) {
                return new javafx.scene.control.TableCell<>() {

                    // Creamos los botones inyectando los íconos vectoriales de FontAwesome
                    private final Button btnVer = new Button("", new FontIcon("fas-eye"));
                    private final Button btnEditar = new Button("", new FontIcon("fas-pen"));
                    private final Button btnEliminar = new Button("", new FontIcon("fas-trash"));

                    private final javafx.scene.layout.HBox panelAcciones = new javafx.scene.layout.HBox(5, btnVer, btnEditar, btnEliminar);

                    {
                        panelAcciones.setAlignment(javafx.geometry.Pos.CENTER);

                        // Estilo opcional para que parezcan íconos flotantes sin borde de botón
                        btnVer.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                        btnEditar.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                        btnEliminar.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

                        btnVer.setOnAction(e -> mostrarDetalle(getTableView().getItems().get(getIndex())));
                        btnEditar.setOnAction(e -> editarCliente(getTableView().getItems().get(getIndex())));
                        btnEliminar.setOnAction(e -> eliminarCliente(getTableView().getItems().get(getIndex())));
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

    private void cargarClientesEnTabla(int numeroPagina) {

        String textoBusqueda = txtBuscarCliente.getText() == null
                ? ""
                : txtBuscarCliente.getText().trim();

        String json;

        if (textoBusqueda.isEmpty()) {

            // 📋 Sin búsqueda:
            // trae los clientes normalmente paginados.
            json = apiService.obtenerClientes(numeroPagina, 20);

        } else {

            // 🔍 Con búsqueda:
            // busca en TODA la base de datos y después pagina.
            json = apiService.buscarClientes(
                    textoBusqueda,
                    numeroPagina,
                    20
            );
        }

        if (json != null && !json.equals("[]") && !json.isEmpty()) {

            try {

                JsonObject respuestaServidor =
                        JsonParser.parseString(json).getAsJsonObject();

                // 📄 Actualizamos la cantidad de páginas
                // según los resultados de la búsqueda.
                if (respuestaServidor.has("totalPages")
                        && paginadorClientes != null) {

                    int totalPaginas =
                            respuestaServidor.get("totalPages").getAsInt();

                    paginadorClientes.setPageCount(
                            totalPaginas == 0 ? 1 : totalPaginas
                    );
                }

                JsonArray arregloClientes =
                        respuestaServidor.getAsJsonArray("content");

                Type tipoLista =
                        new TypeToken<List<Cliente>>() {}.getType();

                List<Cliente> listaClientes =
                        gson.fromJson(arregloClientes, tipoLista);

                if (listaClientes != null) {
                    masterData.setAll(listaClientes);
                } else {
                    masterData.clear();
                }

            } catch (Exception e) {

                System.out.println(
                        "❌ Error al cargar clientes: "
                                + e.getMessage()
                );

                e.printStackTrace();
            }

        } else {

            masterData.clear();

            if (paginadorClientes != null) {
                paginadorClientes.setPageCount(1);
            }
        }
    }

    @FXML
    public void buscarCliente(ActionEvent event) {

        // 🔄 Siempre que se hace una nueva búsqueda,
        // volvemos a la primera página.
        if (paginadorClientes != null) {
            paginadorClientes.setCurrentPageIndex(0);
        }

        // 🔍 Ejecutamos la búsqueda contra el backend.
        cargarClientesEnTabla(0);
    }

    @FXML
    public void abrirModalNuevoCliente(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/nuevo-cliente-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage modalStage = new javafx.stage.Stage();
            modalStage.setTitle("Alta de Cliente");
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.setResizable(false);
            modalStage.showAndWait();

            cargarClientesEnTabla(paginadorClientes != null ? paginadorClientes.getCurrentPageIndex() : 0);
        } catch (Exception e) {
            System.err.println("Error al abrir el Pop-up de Clientes.");
            e.printStackTrace();
        }
    }

// --- 3. LOS MÉTODOS DE ACCIÓN (POSTA) ---

    private void mostrarDetalle(Cliente cliente) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Detalle del Cliente");
        alerta.setHeaderText(cliente.getNombre() + " (DNI/CUIT: " + cliente.getCuit() + ")");

        // Usamos StringBuilder que es más prolijo para armar textos largos
        StringBuilder info = new StringBuilder();
        info.append("Teléfono: ").append(cliente.getTelefono().isEmpty() ? "N/A" : cliente.getTelefono()).append("\n");
        info.append("Email: ").append(cliente.getEmail().isEmpty() ? "N/A" : cliente.getEmail()).append("\n");
        info.append("Condición IVA: ").append(cliente.getCondicionIva()).append("\n\n");

        // --- ACÁ AGREGAMOS LA BILLETERA / CUENTA CORRIENTE ---
        info.append("--- ESTADO DE CUENTA ---\n");

        // 1. Evaluamos si tiene plata a favor
        if (cliente.getSaldoAFavor() > 0) {
            info.append("✅ SALDO A FAVOR: $").append(String.format("%.2f", cliente.getSaldoAFavor())).append("\n");
        } else {
            info.append("SALDO A FAVOR: $0.00\n");
        }

        // 2. Evaluamos si debe plata
        if (cliente.getSaldoPendiente() > 0) {
            info.append("❌ SALDO PENDIENTE: $").append(String.format("%.2f", cliente.getSaldoPendiente())).append("\n");
        } else {
            info.append("SALDO PENDIENTE: $0.00\n");
        }

        info.append("\n--- ESTADÍSTICAS ---\n");
        info.append("Compras: Próximamente...\n");

        alerta.setContentText(info.toString());
        alerta.showAndWait();
    }

    private void eliminarCliente(Cliente cliente) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Confirmar Eliminación");
        alerta.setHeaderText("Vas a eliminar a " + cliente.getNombre());
        alerta.setContentText("¿Estás completamente seguro? Esta acción no se puede deshacer.");

        Optional<ButtonType> resultado = alerta.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            // Llamamos al Backend para que lo borre de verdad
            try {
                apiService.eliminarClienteDeBaseDeDatos(cliente.getId());
                cargarClientesEnTabla(paginadorClientes != null ? paginadorClientes.getCurrentPageIndex() : 0); // Recargamos la tabla automáticamente
            } catch (Exception e) {
                Alert error = new Alert(Alert.AlertType.ERROR, "No se pudo eliminar: " + e.getMessage());
                error.showAndWait();
            }
        }
    }

    private void editarCliente(Cliente cliente) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/nuevo-cliente-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            // 🔥 TRUCAZO: Le pasamos los datos al controlador del modal ANTES de abrirlo
            NuevoClienteController controller = loader.getController();
            controller.cargarDatosParaEditar(cliente);

            javafx.stage.Stage modalStage = new javafx.stage.Stage();
            modalStage.setTitle("Editar Cliente"); // Cambiamos el título
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.setResizable(false);
            modalStage.showAndWait();

            cargarClientesEnTabla(paginadorClientes != null ? paginadorClientes.getCurrentPageIndex() : 0);// Al cerrar, recarga la tabla
        } catch (Exception e) {
            System.err.println("Error al abrir el editor de Clientes.");
            e.printStackTrace();
        }
    }
}