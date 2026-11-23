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

    // Estaban en el diseño pero nunca se habían conectado: quedaban siempre vacías
    @FXML private TableColumn<Cliente, String> colSaldoAFavor;
    @FXML private TableColumn<Cliente, String> colSaldoPendiente;

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

        // F3 = ir al buscador y buscar
        com.nakel.frontend.util.Atajos.buscarConF3(txtBuscarCliente, () -> buscarCliente(null));

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

        // Saldo a favor = suma de los vales vigentes del cliente (lo calcula el backend). Sin saldo se muestra un guion.
        if (colSaldoAFavor != null) {
            colSaldoAFavor.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    cell.getValue().getSaldoAFavor() > 0.009
                            ? com.nakel.frontend.util.FormatoMoneda.pesos(cell.getValue().getSaldoAFavor()) : "-"));
        }
        if (colSaldoPendiente != null) {
            colSaldoPendiente.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    cell.getValue().getSaldoPendiente() > 0.009
                            ? com.nakel.frontend.util.FormatoMoneda.pesos(cell.getValue().getSaldoPendiente()) : "-"));
        }

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
                        // Mismo aspecto, tachito rojo y cartelitos de ayuda que en el resto de las tablas
                        com.nakel.frontend.util.Acciones.ver(btnVer, "Ver el detalle");
                        com.nakel.frontend.util.Acciones.editar(btnEditar, "Editar el cliente");
                        com.nakel.frontend.util.Acciones.eliminar(btnEliminar, "Eliminar el cliente");

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

                // El total lo informa el backend (cuenta todas las páginas, no solo la que se ve)
                long total = respuestaServidor.has("totalElements")
                        ? respuestaServidor.get("totalElements").getAsLong()
                        : masterData.size();
                mostrarTotal(total, !textoBusqueda.isEmpty());

            } catch (Exception e) {

                System.out.println(
                        "Error al cargar clientes: "
                                + e.getMessage()
                );

                e.printStackTrace();
            }

        } else {

            masterData.clear();
            mostrarTotal(0, !textoBusqueda.isEmpty());

            if (paginadorClientes != null) {
                paginadorClientes.setPageCount(1);
            }
        }
    }

    @FXML private Label lblTituloTotalClientes;
    @FXML private Label lblTotalClientes;

    /** Contador de abajo: total de clientes, o cuántos encontró la búsqueda. */
    private void mostrarTotal(long cantidad, boolean esBusqueda) {
        lblTituloTotalClientes.setText(esBusqueda ? "Clientes encontrados:" : "Total de clientes:");
        lblTotalClientes.setText(String.valueOf(cantidad));
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

    // "2026-08-11T10:08:16" -> "11/08/2026"
    private static String fechaCorta(String fechaIso) {
        if (fechaIso == null || fechaIso.isBlank()) return "-";
        try {
            return java.time.LocalDateTime.parse(fechaIso).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return fechaIso;
        }
    }

    private void mostrarDetalle(Cliente cliente) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Detalle del Cliente");
        alerta.setHeaderText(cliente.getNombre() + " (DNI/CUIT: " + cliente.getCuit() + ")");

        // Usamos StringBuilder que es más prolijo para armar textos largos
        StringBuilder info = new StringBuilder();
        String telefono = cliente.getTelefono() == null || cliente.getTelefono().isBlank() ? "N/A" : cliente.getTelefono();
        String email = cliente.getEmail() == null || cliente.getEmail().isBlank() ? "N/A" : cliente.getEmail();
        info.append("Teléfono: ").append(telefono).append("\n");
        info.append("Email: ").append(email).append("\n");
        info.append("Condición IVA: ").append(cliente.getCondicionIva()).append("\n\n");

        info.append("--- ESTADO DE CUENTA ---\n");

        // 1. Saldo a favor CON SU DETALLE: es la suma de los vales vigentes, y acá se ve cuáles son.
        //    (Un total sin detalle confunde: puede incluir vales viejos que el cliente todavía no usó.)
        java.util.List<com.nakel.frontend.model.Vale> vales =
                new com.nakel.frontend.service.ValeApiService().valesVigentesDeCliente(cliente.getId());

        info.append("SALDO A FAVOR: ").append(com.nakel.frontend.util.FormatoMoneda.pesos(cliente.getSaldoAFavor())).append("\n");
        if (vales != null && !vales.isEmpty()) {
            info.append(vales.size() == 1 ? "Sale de 1 vale vigente:\n" : "Sale de " + vales.size() + " vales vigentes:\n");
            for (com.nakel.frontend.model.Vale vale : vales) {
                info.append("   • ").append(vale.getCodigo())
                        .append("   ").append(com.nakel.frontend.util.FormatoMoneda.pesos(vale.getMonto()))
                        .append("   emitido el ").append(fechaCorta(vale.getFechaEmision()))
                        .append(", vence el ").append(fechaCorta(vale.getFechaVencimiento()));
                if (vale.getMotivo() != null && !vale.getMotivo().isBlank()) {
                    info.append("\n       ").append(vale.getMotivo());
                }
                info.append("\n");
            }
        } else if (vales != null) {
            info.append("No tiene vales vigentes.\n");
        }

        // 2. Deuda
        info.append("\nSALDO PENDIENTE: ").append(com.nakel.frontend.util.FormatoMoneda.pesos(cliente.getSaldoPendiente())).append("\n");

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