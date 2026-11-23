package com.nakel.frontend.controller;

import com.nakel.frontend.model.Cliente;
import com.nakel.frontend.model.Vale;
import com.nakel.frontend.service.ClienteApiService;
import com.nakel.frontend.service.ImpresoraService;
import com.nakel.frontend.service.ValeApiService;
import com.nakel.frontend.util.FormatoMoneda;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Pestaña "Vales" de Configuración.
 *
 * Izquierda: emitir un vale a un cliente (atención, compensación, regalo...).
 * Derecha: todos los vales del sistema, con su saldo, vencimiento y estado.
 *
 * El "saldo a favor" de cada cliente es la suma de sus vales vigentes: por eso la forma
 * de darle saldo a alguien es emitirle un vale desde acá, no escribir un número a mano.
 */
public class TabValesController {

    // --- Emitir ---
    @FXML private TextField txtBuscarCliente;
    @FXML private ListView<Cliente> listaClientes;
    @FXML private Label lblClienteElegido;
    @FXML private TextField txtMonto;
    @FXML private TextField txtMotivo;
    @FXML private Button btnGenerar;

    // --- Listado ---
    @FXML private CheckBox chkSoloVigentes;
    @FXML private TableView<Vale> tablaVales;
    @FXML private TableColumn<Vale, String> colCodigo;
    @FXML private TableColumn<Vale, String> colCliente;
    @FXML private TableColumn<Vale, String> colMonto;
    @FXML private TableColumn<Vale, String> colEmision;
    @FXML private TableColumn<Vale, String> colVence;
    @FXML private TableColumn<Vale, String> colEstado;
    @FXML private TableColumn<Vale, String> colMotivo;
    @FXML private Pagination paginadorVales;

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ClienteApiService clienteApi = new ClienteApiService();
    private final ValeApiService valeApi = new ValeApiService();
    private final ObservableList<Vale> vales = FXCollections.observableArrayList();

    private Cliente clienteElegido;
    private PauseTransition esperaBusqueda;

    @FXML
    public void initialize() {
        FormatoMoneda.aplicar(txtMonto);

        // Cada cliente se muestra como "Juan Pérez - 20334455"
        listaClientes.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Cliente cliente, boolean vacio) {
                super.updateItem(cliente, vacio);
                setText(vacio || cliente == null ? null : textoDe(cliente));
            }
        });
        listaClientes.setPlaceholder(new Label("Escribí arriba para buscar un cliente."));
        listaClientes.getSelectionModel().selectedItemProperty().addListener((obs, antes, elegido) -> {
            if (elegido != null) elegir(elegido);
        });

        // Buscar mientras se escribe, con una pequeña espera para no consultar en cada tecla
        esperaBusqueda = new PauseTransition(Duration.millis(300));
        esperaBusqueda.setOnFinished(e -> buscarClientes());
        txtBuscarCliente.textProperty().addListener((obs, antes, ahora) -> esperaBusqueda.playFromStart());

        configurarTabla();
        chkSoloVigentes.selectedProperty().addListener((obs, antes, ahora) -> recargarDesdeElPrincipio());

        paginadorVales.setPageFactory(pagina -> {
            cargarVales(pagina);
            return new javafx.scene.layout.VBox();
        });

        buscarClientes();
    }

    private static String textoDe(Cliente c) {
        return c.getNombre() + (c.getCuit() != null && !c.getCuit().isBlank() ? " - " + c.getCuit() : "");
    }

    private void buscarClientes() {
        String texto = txtBuscarCliente.getText() == null ? "" : txtBuscarCliente.getText().trim();
        List<Cliente> encontrados = clienteApi.buscarClientesPaginados(texto, 0, 15);

        // "Consumidor Final" no es una persona: un vale a su nombre no le serviría a nadie
        encontrados = encontrados.stream()
                .filter(c -> c.getNombre() != null && !"00000000".equals(c.getCuit()))
                .toList();

        listaClientes.getItems().setAll(encontrados);
    }

    private void elegir(Cliente cliente) {
        clienteElegido = cliente;
        lblClienteElegido.setText("Vale para: " + textoDe(cliente)
                + "\nSaldo a favor actual: " + FormatoMoneda.pesos(cliente.getSaldoAFavor()));
        lblClienteElegido.setStyle("-fx-font-weight: bold; -fx-text-fill: #155643;");
    }

    private void deselegir() {
        clienteElegido = null;
        listaClientes.getSelectionModel().clearSelection();
        lblClienteElegido.setText("Todavía no elegiste un cliente");
        lblClienteElegido.setStyle("-fx-font-weight: bold; -fx-text-fill: #B26A00;");
    }

    // Alta rápida de cliente, la misma ventana que usa el mostrador
    @FXML
    public void abrirClienteNuevo(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/cliente-express-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            Dialog<Cliente> dialog = new Dialog<>();
            dialog.setTitle("Cliente nuevo");
            dialog.setHeaderText("Alta rápida de cliente");
            dialog.getDialogPane().setContent(root);

            ButtonType btnGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

            ClienteExpressController controladorModal = loader.getController();
            dialog.setResultConverter(boton -> boton == btnGuardar ? controladorModal.procesarGuardado() : null);

            dialog.showAndWait().ifPresent(clienteReal -> {
                if (clienteReal != null) {
                    listaClientes.getItems().setAll(clienteReal);
                    listaClientes.getSelectionModel().select(clienteReal);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            avisar(Alert.AlertType.ERROR, "Cliente nuevo", "No se pudo abrir la ventana de alta de cliente.");
        }
    }

    @FXML
    public void generarVale(ActionEvent event) {
        if (clienteElegido == null || clienteElegido.getId() == null) {
            avisar(Alert.AlertType.WARNING, "Falta el cliente", "Elegí de la lista para qué cliente es el vale.");
            return;
        }

        double monto;
        try {
            monto = FormatoMoneda.leer(txtMonto);
        } catch (Exception e) {
            avisar(Alert.AlertType.WARNING, "Falta el monto", "Escribí de cuánto es el vale. Ejemplo: 5.000");
            txtMonto.requestFocus();
            return;
        }
        if (monto <= 0) {
            avisar(Alert.AlertType.WARNING, "Monto inválido", "El vale tiene que ser por más de $ 0.");
            return;
        }

        String motivo = (txtMotivo.getText() == null || txtMotivo.getText().isBlank())
                ? "Vale emitido a mano"
                : txtMotivo.getText().trim();

        // Confirmación: un vale es plata, mejor releer antes de emitirlo
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION);
        confirmar.setTitle("Confirmar vale");
        confirmar.setHeaderText("¿Emitir este vale?");
        confirmar.setContentText("Cliente: " + textoDe(clienteElegido)
                + "\nMonto: " + FormatoMoneda.pesos(monto)
                + "\nMotivo: " + motivo
                + "\n\nEl cliente lo va a poder usar como pago durante un año.");
        if (confirmar.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        Vale vale = valeApi.generarVale(monto, clienteElegido.getId(), motivo);
        if (vale == null) {
            String detalle = valeApi.getUltimoError();
            avisar(Alert.AlertType.ERROR, "No se pudo generar el vale",
                    (detalle != null ? detalle : "El servidor no respondió.")
                            + "\n\nLos datos siguen en pantalla: probá de nuevo.");
            return;
        }

        mostrarValeGenerado(vale, clienteElegido, motivo);

        txtMonto.clear();
        txtMotivo.clear();
        deselegir();
        buscarClientes();
        recargarDesdeElPrincipio();
    }

    private void mostrarValeGenerado(Vale vale, Cliente cliente, String motivo) {
        ButtonType imprimir = new ButtonType("Imprimir comprobante", ButtonBar.ButtonData.LEFT);
        ButtonType cerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.OK_DONE);

        Alert listo = new Alert(Alert.AlertType.INFORMATION, "", imprimir, cerrar);
        listo.setTitle("Vale generado");
        listo.setHeaderText("Vale generado: " + vale.getCodigo());
        listo.setContentText("Cliente: " + textoDe(cliente)
                + "\nMonto: " + FormatoMoneda.pesos(vale.getMonto())
                + "\nVálido hasta: " + fecha(vale.getFechaVencimiento())
                + "\n\nAnotá o imprimí el código: es lo que se pide al cobrar.");

        if (listo.showAndWait().orElse(cerrar) == imprimir) {
            StringBuilder texto = new StringBuilder();
            texto.append("------ NAKEL SOFTWARE ------\n");
            texto.append("       VALE DE COMPRA\n");
            texto.append("----------------------------\n");
            texto.append("Cliente: ").append(cliente.getNombre()).append("\n");
            texto.append("Monto: ").append(FormatoMoneda.pesos(vale.getMonto())).append("\n");
            texto.append("Valido hasta: ").append(fecha(vale.getFechaVencimiento())).append("\n");
            texto.append("----------------------------\n");
            texto.append("CODIGO DEL VALE:\n");
            texto.append(">>> ").append(vale.getCodigo()).append(" <<<\n");
            texto.append("----------------------------\n");
            texto.append("\n\n\n\n\n");
            try {
                ImpresoraService.imprimirTexto(texto.toString());
            } catch (Exception e) {
                avisar(Alert.AlertType.ERROR, "Error de impresión", e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------
    // Listado de vales
    // ------------------------------------------------------------------

    private void configurarTabla() {
        colCodigo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getClienteNombre() != null ? c.getValue().getClienteNombre() : "Sin cliente"));
        colMonto.setCellValueFactory(c -> new SimpleStringProperty(FormatoMoneda.pesos(c.getValue().getMonto())));
        colEmision.setCellValueFactory(c -> new SimpleStringProperty(fecha(c.getValue().getFechaEmision())));
        colVence.setCellValueFactory(c -> new SimpleStringProperty(fecha(c.getValue().getFechaVencimiento())));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(nombreEstado(c.getValue().getEstado())));
        colMotivo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getMotivo() != null ? c.getValue().getMotivo() : ""));

        tablaVales.setPlaceholder(new Label("No hay vales para mostrar."));
        tablaVales.setItems(vales);
    }

    private static String nombreEstado(String estado) {
        if (estado == null) return "-";
        switch (estado) {
            case "ACTIVO": return "Vigente";
            case "USADO": return "Usado";
            case "VENCIDO": return "Vencido";
            default: return estado;
        }
    }

    private static String fecha(String fechaIso) {
        if (fechaIso == null || fechaIso.isBlank()) return "-";
        try {
            return LocalDateTime.parse(fechaIso).format(FECHA);
        } catch (Exception e) {
            return fechaIso;
        }
    }

    private void cargarVales(int pagina) {
        vales.setAll(valeApi.listarVales(pagina, 15, chkSoloVigentes.isSelected()));
        paginadorVales.setPageCount(valeApi.getUltimasPaginas());
    }

    private void recargarDesdeElPrincipio() {
        if (paginadorVales.getCurrentPageIndex() != 0) {
            paginadorVales.setCurrentPageIndex(0); // esto ya dispara la carga
        } else {
            cargarVales(0);
        }
    }

    @FXML
    public void recargar(ActionEvent event) {
        cargarVales(paginadorVales.getCurrentPageIndex());
    }

    private void avisar(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
