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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.kordamp.ikonli.javafx.FontIcon;
import java.util.Optional;
import javafx.scene.control.ButtonType;

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

    private final ClienteApiService apiService = new ClienteApiService();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        System.out.println("¡Módulo de Clientes cargado con éxito!");
        configurarTabla();
        cargarClientesEnTabla();
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

    private void cargarClientesEnTabla() {
        String json = apiService.obtenerClientes();

        if (json != null && !json.equals("[]") && !json.isEmpty()) {
            try {
                // 1. Leemos la respuesta del servidor como un Objeto JSON completo
                JsonObject respuestaServidor = JsonParser.parseString(json).getAsJsonObject();

                // 2. Extraemos ÚNICAMENTE la lista que está adentro de "content"
                JsonArray arregloClientes = respuestaServidor.getAsJsonArray("content");

                // 3. Convertimos ese arreglo a nuestra lista de Java
                Type tipoLista = new TypeToken<List<Cliente>>(){}.getType();
                List<Cliente> listaClientes = gson.fromJson(arregloClientes, tipoLista);

                // 4. Llenamos la tabla de JavaFX
                ObservableList<Cliente> datosObservable = FXCollections.observableArrayList(listaClientes);
                tablaClientes.setItems(datosObservable);

                System.out.println("✅ Tabla cargada con " + listaClientes.size() + " clientes.");
            } catch (Exception e) {
                System.out.println("❌ Error al convertir el JSON a la tabla: " + e.getMessage());
                e.printStackTrace(); // Opcional: te ayuda a ver en la consola dónde falló exactamente
            }
        } else {
            System.out.println("⚠️ La base de datos está vacía o el JSON vino nulo.");
        }
    }

    @FXML
    public void buscarCliente(ActionEvent event) {
        String textoBusqueda = txtBuscarCliente.getText();
        System.out.println("Buscando en la base de datos: " + textoBusqueda);
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

            cargarClientesEnTabla();
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

        String info = "Teléfono: " + (cliente.getTelefono().isEmpty() ? "N/A" : cliente.getTelefono()) + "\n"
                + "Email: " + (cliente.getEmail().isEmpty() ? "N/A" : cliente.getEmail()) + "\n"
                + "Condición IVA: " + cliente.getCondicionIva() + "\n\n"
                + "--- ESTADÍSTICAS ---\n"
                + "Compras: Próximamente...\n";

        alerta.setContentText(info);
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
                cargarClientesEnTabla(); // Recargamos la tabla automáticamente
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

            cargarClientesEnTabla(); // Al cerrar, recarga la tabla
        } catch (Exception e) {
            System.err.println("Error al abrir el editor de Clientes.");
            e.printStackTrace();
        }
    }
}