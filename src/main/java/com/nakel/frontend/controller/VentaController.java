package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.nakel.frontend.model.Articulo;
import com.nakel.frontend.model.DetalleVenta;
import com.nakel.frontend.model.LineaTicket;
import com.nakel.frontend.model.Pago;
import com.nakel.frontend.service.ArticuloApiService;
import com.nakel.frontend.service.VentaApiService;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.List;

public class VentaController {

    private final ArticuloApiService apiService = new ArticuloApiService();

    // Cabecera AFIP
// 🔥 ¡ATENCIÓN! Cambiar de <String> a <Cliente>
    @FXML private ComboBox<com.nakel.frontend.model.Cliente> cmbCliente;

    // 🔒 Variables para la paginación del ComboBox de Clientes
    private javafx.animation.PauseTransition debounceCliente;
    private int paginaActualCliente = 0;
    private String busquedaActualCliente = "";
    private boolean actualizandoCliente = false;
    //parte new arriba
    @FXML private ComboBox<String> cmbTipoFactura;

    // Buscador y Tabla Principal
    @FXML private TextField txtCodigoBarras;
    @FXML private TableView<LineaTicket> tablaTicket;

    // 🔥 NUEVO: La barra de búsqueda de la lupita (¡Asegurate de ponerle el fx:id en SceneBuilder!)
    @FXML private TextField txtBuscarInventario;
    @FXML private TableView<Articulo> tablaInventario;

    // Totales y Cobro
    @FXML private Label lblTotal;
    @FXML private ComboBox<String> cmbMedioPago;
    @FXML private javafx.scene.control.CheckBox chkRegalo;
    @FXML private Button btnCobrar;

    // Sin emojis: el ícono lo pone util/Iconos según el texto (caja registradora / presupuesto)
    private static final String TEXTO_COBRAR = "COBRAR (F12)";
    private static final String TEXTO_PRESUPUESTAR = "PRESUPUESTAR (F12)";

    private boolean esModoPresupuesto() {
        return cmbTipoFactura != null && "Presupuesto".equals(cmbTipoFactura.getValue());
    }

    // Un presupuesto no se cobra: se apagan el medio de pago y "es para regalo",
    // y el botón pasa a decir PRESUPUESTAR. Al volver a "Ticket de Venta" todo se restablece.
    private void actualizarModoComprobante() {
        boolean presupuesto = esModoPresupuesto();

        if (cmbMedioPago != null) cmbMedioPago.setDisable(presupuesto);
        if (chkRegalo != null) {
            if (presupuesto) chkRegalo.setSelected(false);
            chkRegalo.setDisable(presupuesto);
        }
        if (btnCobrar != null) btnCobrar.setText(presupuesto ? TEXTO_PRESUPUESTAR : TEXTO_COBRAR);
    }

    // Segunda tabla
    @FXML private TableColumn<Articulo, String> colInvCodigo;
    @FXML private TableColumn<Articulo, String> colInvNombre;
    @FXML private TableColumn<Articulo, Double> colInvPrecio;

    private final ArticuloApiService articuloApi = new ArticuloApiService();
    private final Gson gson = new Gson();

    @FXML private TableColumn<LineaTicket, String> colCodigo;
    @FXML private TableColumn<LineaTicket, String> colNombre;
    @FXML private TableColumn<LineaTicket, Integer> colCantidad;
    @FXML private TableColumn<LineaTicket, Double> colPrecio;
    @FXML private TableColumn<LineaTicket, Double> colSubtotal;
    @FXML private TableColumn<LineaTicket, Void> colAccion;

    @FXML private Pagination paginadorInventario;

    // 🔥 NUEVO: Listas para el buscador del inventario
    private ObservableList<Articulo> masterInventario = FXCollections.observableArrayList();
    private FilteredList<Articulo> filteredInventario;

    // 🔥 NUEVO: Lista para el buscador de clientes
    private ObservableList<String> clientesMaster = FXCollections.observableArrayList();

    // 🔥 NUEVO: Guardamos los objetos originales para sacarles el ID cuando hagamos el Vale
    private List<com.nakel.frontend.model.Cliente> listaClientesReales = new ArrayList<>();

    @FXML
    public void initialize() {
        configurarEventos();
        configurarTabla();
        configurarTablaInventario();
        cargarDatosIniciales();

        // 🔥 CONFIGURAMOS EL PAGINADOR NATIVO DEL MOSTRADOR
        if (paginadorInventario != null) {
            paginadorInventario.setPageFactory(paginaIndex -> {
                String textoBusqueda = txtCodigoBarras.getText() != null ? txtCodigoBarras.getText().trim() : "";
                cargarInventarioParaVenta(textoBusqueda, paginaIndex);

                // JavaFX exige devolver un nodo visual, pasamos una caja invisible
                return new javafx.scene.layout.VBox();
            });
        } else {
            // Fallback por si el FXML no inicializó el componente todavía
            cargarInventarioParaVenta("", 0);
        }

        System.out.println("Terminal de Punto de Venta (POS) Iniciada.");
    }

    private void configurarEventos() {
        // 🔥 1. BÚSQUEDA DINÁMICA (RAM): Filtra al instante la página actual mientras escribe
        txtCodigoBarras.textProperty().addListener((observable, oldValue, newValue) -> {
            if (filteredInventario != null) {
                filteredInventario.setPredicate(articulo -> {
                    if (newValue == null || newValue.isBlank()) return true;

                    String busqueda = newValue.toLowerCase().trim();
                    return (articulo.getNombre() != null && articulo.getNombre().toLowerCase().contains(busqueda)) ||
                            (articulo.getCodigo() != null && articulo.getCodigo().toLowerCase().contains(busqueda)) ||
                            (articulo.getCategoria() != null && articulo.getCategoria().getNombre() != null && articulo.getCategoria().getNombre().toLowerCase().contains(busqueda)) ||
                            (articulo.getMaterial() != null && articulo.getMaterial().getNombre() != null && articulo.getMaterial().getNombre().toLowerCase().contains(busqueda));
                });
            }
        });

        // 🔥 2. BÚSQUEDA GLOBAL (BACKEND): ENTER para ir a buscar a la Base de Datos
        txtCodigoBarras.setOnAction(event -> {
            String texto = txtCodigoBarras.getText() != null ? txtCodigoBarras.getText().trim() : "";
            // Reseteamos visualmente a la página 1 (índice 0) y disparamos la búsqueda
            if (paginadorInventario != null) {
                paginadorInventario.setCurrentPageIndex(0);
            }
            if (texto.isEmpty()) {
                cargarInventarioParaVenta(texto, 0); // Enter con el casillero vacío: vuelve a mostrar todo
            } else {
                // Enter con algo escrito es lo que manda un lector de código de barras. Hace lo mismo que el
                // botón "Buscar": si es el código exacto de un artículo lo agrega al ticket; si no, muestra
                // las coincidencias. (Antes el Enter solo filtraba la lista y nunca agregaba nada.)
                procesarBusquedaInventario();
            }
        });

        // 3. Doble click → agregar
        tablaInventario.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Articulo seleccionado = tablaInventario.getSelectionModel().getSelectedItem();
                if (seleccionado != null) {
                    agregarAlTicket(seleccionado);
                }
            }
        });

        // 4. ENTER en tabla → agregar
        tablaInventario.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Articulo seleccionado = tablaInventario.getSelectionModel().getSelectedItem();
                if (seleccionado != null) {
                    agregarAlTicket(seleccionado);
                }
                event.consume();
            }
        });

        // 5. Atajos de teclado del mostrador. Valen solo mientras esta pantalla está a la vista
        //    (antes el ESC quedaba enganchado a la ventana y seguía actuando desde otras pantallas).
        com.nakel.frontend.util.Atajos.registrar(txtCodigoBarras, KeyCode.ESCAPE, () -> limpiarBusquedaInventario(null));
        com.nakel.frontend.util.Atajos.buscarConF3(txtCodigoBarras, () -> buscarProducto(null));   // F3 = Buscar
        com.nakel.frontend.util.Atajos.registrar(txtCodigoBarras, KeyCode.F12, () -> cobrarVenta(null)); // F12 = Cobrar / Presupuestar
    }

    private void procesarBusquedaInventario() {
        String texto = txtCodigoBarras.getText();

        if (texto == null || texto.isBlank()) {
            return;
        }

        String busqueda = texto.trim();
//        System.out.println("🕵️‍♂️ [DEBUG] Pistola Láser / Lupa disparó la búsqueda en el Backend: '" + busqueda + "'");

        try {
            List<Articulo> resultados = articuloApi.buscarParaVenta(busqueda, 0, 100);

//            System.out.println("🕵️‍♂️ [DEBUG] Búsqueda finalizada. Resultados: " + (resultados != null ? resultados.size() : "null"));

            if (resultados == null || resultados.isEmpty()) {
                if (paginadorInventario != null) paginadorInventario.setPageCount(1);
                mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Sin Coincidencias",
                        "No se encontró ningún producto disponible que coincida con: " + texto
                );
                return;
            }

            masterInventario.setAll(resultados);

            // 🔥 ACTUALIZAMOS EL PAGINADOR TAMBIÉN AL BUSCAR CON LA LUPA/PISTOLA
            int paginasReales = articuloApi.getUltimasPaginasMostrador();
            if (paginadorInventario != null) {
                paginadorInventario.setPageCount(paginasReales);
            }

            filteredInventario = new FilteredList<>(masterInventario, p -> true);
            tablaInventario.setItems(filteredInventario);

            // Lo que escribe un lector de código de barras es el código exacto de un artículo:
            // ese va directo al ticket aunque la búsqueda haya traído otros parecidos.
            // (Antes "1003-001" también traía "1003-0010" y, como había varios, no agregaba ninguno.)
            for (Articulo candidato : resultados) {
                if (candidato.getCodigo() != null && candidato.getCodigo().trim().equalsIgnoreCase(busqueda)) {
                    agregarAlTicket(candidato);
                    txtCodigoBarras.clear();
                    txtCodigoBarras.requestFocus(); // listo para escanear el siguiente
                    return;
                }
            }

            if (resultados.size() == 1) {
                agregarAlTicket(resultados.get(0));
                txtCodigoBarras.clear();
                return;
            }

            tablaInventario.requestFocus();
            tablaInventario.getSelectionModel().selectFirst();

        } catch (Exception e) {
            System.out.println("❌ Error al buscar productos para venta: " + e.getMessage());

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error de búsqueda",
                    "No se pudo realizar la búsqueda del producto."
            );
        }
    }

    @FXML
    public void buscarProducto(ActionEvent event) {
        procesarBusquedaInventario();
    }
    // 🧹 Botón para limpiar la búsqueda del inventario
    @FXML
    public void limpiarBusquedaInventario(ActionEvent event) {
        txtCodigoBarras.clear();

        cargarInventarioParaVenta("", 0);

        txtCodigoBarras.requestFocus();
    }

    private void configurarTabla() {
        tablaTicket.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colCodigo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getArticulo().getCodigo()));

        colNombre.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getArticulo().getNombre()));

        colPrecio.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getArticulo().getPrecio()).asObject());
        // Precio y subtotal del ticket se muestran como plata: $ 10.000,00
        colPrecio.setCellFactory(com.nakel.frontend.util.FormatoMoneda.celdaPesos());
        colSubtotal.setCellFactory(com.nakel.frontend.util.FormatoMoneda.celdaPesos());

        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));

        colCantidad.setCellFactory(param -> new TableCell<LineaTicket, Integer>() {
            private final Button btnMenos = new Button("-");
            private final Label lblCantidad = new Label();
            private final Button btnMas = new Button("+");
            private final javafx.scene.layout.HBox panel =
                    new javafx.scene.layout.HBox(8, btnMenos, lblCantidad, btnMas);

            {
                panel.setAlignment(javafx.geometry.Pos.CENTER);

                btnMenos.getStyleClass().add("btn-cantidad-accion");
                btnMas.getStyleClass().add("btn-cantidad-accion");

                btnMas.setOnAction(e -> {
                    LineaTicket linea = getTableView().getItems().get(getIndex());

                    if (linea.getCantidad() < linea.getArticulo().getStockActual()) {
                        linea.setCantidad(linea.getCantidad() + 1);
                        getTableView().refresh();
                        actualizarTotal();
                    }
                });

                btnMenos.setOnAction(e -> {
                    LineaTicket linea = getTableView().getItems().get(getIndex());

                    if (linea.getCantidad() > 1) {
                        linea.setCantidad(linea.getCantidad() - 1);
                        getTableView().refresh();
                        actualizarTotal();
                    }
                });
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    lblCantidad.setText(String.valueOf(item));
                    setGraphic(panel);
                }
            }
        });

        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        colAccion.setCellFactory(param -> new TableCell<LineaTicket, Void>() {
            private final Button btnEliminar = new Button("");

            {
                btnEliminar.getStyleClass().add("btn-eliminar");
                // Cruz vectorial blanca (antes era un emoji)
                org.kordamp.ikonli.javafx.FontIcon cruz = new org.kordamp.ikonli.javafx.FontIcon("fas-times");
                cruz.setIconColor(javafx.scene.paint.Color.WHITE);
                btnEliminar.setGraphic(cruz);
                btnEliminar.setStyle("-fx-padding: 4px 10px;");
                com.nakel.frontend.util.Acciones.ayuda(btnEliminar, "Quitar del ticket");

                btnEliminar.setOnAction(e -> {
                    LineaTicket linea = getTableView().getItems().get(getIndex());

                    if (linea != null) {
                        tablaTicket.getItems().remove(linea);
                        actualizarTotal();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnEliminar);
                }
            }
        });
    }

    // 🧮 Método para sumar los precios de la tabla del ticket
    private double obtenerTotalNumerico() {
        // Recorre LineaTicket y suma los subtotales reales (precio * cantidad)
        return tablaTicket.getItems().stream()
                .mapToDouble(LineaTicket::getSubtotal)
                .sum();
    }

    private void configurarTablaInventario() {
        tablaInventario.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colInvCodigo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getCodigo()));

        colInvNombre.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getNombre()));

        colInvPrecio.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getPrecio()).asObject());
        colInvPrecio.setCellFactory(com.nakel.frontend.util.FormatoMoneda.celdaPesos());
    }

    private void cargarDatosIniciales() {
        // 1. SOLUCIÓN COMPROBANTES Y MEDIOS DE PAGO
        cmbTipoFactura.getItems().addAll("Ticket de Venta", "Factura A (Con IVA)", "Presupuesto");
        cmbTipoFactura.setValue("Ticket de Venta");

        cmbMedioPago.getItems().addAll("Efectivo", "Transferencia", "MercadoPago", "Tarjeta de Crédito", "Tarjeta de Débito", "Pago Mixto");
        cmbMedioPago.setValue("Efectivo");

        // Según el comprobante elegido cambia qué se puede tocar (Presupuesto no se cobra)
        cmbTipoFactura.valueProperty().addListener((obs, antes, ahora) -> actualizarModoComprobante());
        actualizarModoComprobante();

        // 2. Encendemos el buscador inteligente de Clientes
        configurarBuscadorCliente();
    }

    // Texto gris de fondo del combo de cliente cuando no hay nadie elegido
    private static final String TEXTO_CONSUMIDOR_FINAL = "Consumidor Final - 00000000";

    // Cómo se muestra un cliente en el combo: "Juan Pérez - 20334455"
    private String textoDeCliente(com.nakel.frontend.model.Cliente c) {
        if (c == null) return "";
        return c.getNombre() + (c.getCuit() != null && !c.getCuit().isBlank() ? " - " + c.getCuit() : "");
    }

    /**
     * Cliente realmente elegido para la venta.
     * Devuelve null cuando el combo está vacío (se ve "Consumidor Final" en gris):
     * en ese caso la venta sale a Consumidor Final.
     */
    private com.nakel.frontend.model.Cliente clienteElegido() {
        String texto = cmbCliente.getEditor().getText() == null ? "" : cmbCliente.getEditor().getText().trim();
        if (texto.isEmpty()) return null;

        com.nakel.frontend.model.Cliente seleccionado = cmbCliente.getValue();
        if (seleccionado == null) seleccionado = cmbCliente.getSelectionModel().getSelectedItem();

        boolean coincide = seleccionado != null && textoDeCliente(seleccionado).equals(texto);
        boolean esReal = coincide && seleccionado.getId() != null && seleccionado.getId() > 0;
        return esReal ? seleccionado : null;
    }

    // true si hay texto escrito en el combo que NO corresponde a un cliente elegido de la lista
    private boolean clienteEscritoSinElegir() {
        String texto = cmbCliente.getEditor().getText() == null ? "" : cmbCliente.getEditor().getText().trim();
        if (texto.isEmpty()) return false;

        com.nakel.frontend.model.Cliente seleccionado = cmbCliente.getValue();
        if (seleccionado == null) seleccionado = cmbCliente.getSelectionModel().getSelectedItem();
        return seleccionado == null || !textoDeCliente(seleccionado).equals(texto);
    }

    // Deja el combo vacío (se ve "Consumidor Final" en gris) y recarga la lista completa sin desplegarla
    private void volverAConsumidorFinal() {
        actualizandoCliente = true;
        try {
            cmbCliente.getSelectionModel().clearSelection();
            cmbCliente.setValue(null);
            cmbCliente.getEditor().clear();
            cmbCliente.getItems().clear();
        } finally {
            actualizandoCliente = false;
        }
        cmbCliente.hide();
        busquedaActualCliente = "";
        paginaActualCliente = 0;
        buscarYMostrarCliente("", 0, false);
    }

    private void configurarBuscadorCliente() {
        cmbCliente.setEditable(true);
        cmbCliente.setVisibleRowCount(10);

        // "Consumidor Final" ya no se escribe como texto real: queda de fondo en gris.
        // Si no se elige a nadie, la venta sale a Consumidor Final; para buscar un cliente
        // alcanza con hacer clic y escribir, sin tener que borrar nada antes.
        cmbCliente.setPromptText(TEXTO_CONSUMIDOR_FINAL);
        if (!cmbCliente.getStyleClass().contains("combo-cliente")) {
            cmbCliente.getStyleClass().add("combo-cliente");
        }

        // 1. Convertidor visual: Para que en pantalla diga "Juan - 20334455" (SIN FANTASMAS)
        cmbCliente.setConverter(new javafx.util.StringConverter<com.nakel.frontend.model.Cliente>() {
            @Override
            public String toString(com.nakel.frontend.model.Cliente c) {
                return textoDeCliente(c);
            }
            @Override
            public com.nakel.frontend.model.Cliente fromString(String string) {
                // Campo vacío = nadie elegido (Consumidor Final). Antes devolvía el último cliente
                // seleccionado aunque se hubiera borrado el texto.
                if (string == null || string.isBlank()) return null;
                return cmbCliente.getSelectionModel().getSelectedItem();
            }
        });

        debounceCliente = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));

        // 2. Escuchador de tipeo en vivo
        cmbCliente.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {

            if (actualizandoCliente) return;

            // 🔥 FIX: Si acabamos de seleccionar un cliente,
            // el StringConverter cambia el texto del editor.
            // NO debemos interpretar ese cambio como una nueva búsqueda.
            com.nakel.frontend.model.Cliente seleccionado =
                    cmbCliente.getSelectionModel().getSelectedItem();

            if (seleccionado != null) {

                String textoSeleccionado =
                        seleccionado.getNombre()
                                + (seleccionado.getCuit() != null
                                && !seleccionado.getCuit().isBlank()
                                ? " - " + seleccionado.getCuit()
                                : "");

                if (textoSeleccionado.equals(newVal)) {
                    return;
                }
            }

            // 🔥 FIX: Si el campo está vacío, traemos los primeros 20 por defecto

            // 🔥 FIX: Si el campo está vacío, traemos los primeros 20 por defecto en vez de ocultar
            if (newVal == null || newVal.trim().isEmpty()) {
                debounceCliente.stop();
                paginaActualCliente = 0;
                actualizandoCliente = true;
                // Borró el texto: ya no hay cliente elegido (vuelve a Consumidor Final)
                cmbCliente.getSelectionModel().clearSelection();
                cmbCliente.getItems().clear();
                actualizandoCliente = false;

                buscarYMostrarCliente("", 0);
                return;
            }

            debounceCliente.stop();
            debounceCliente.setOnFinished(e -> {
                busquedaActualCliente = newVal.trim();
                paginaActualCliente = 0; // Volvemos a la página 0 al buscar algo nuevo

                actualizandoCliente = true;
                cmbCliente.getItems().clear();
                actualizandoCliente = false;

                buscarYMostrarCliente(busquedaActualCliente, paginaActualCliente);
            });
            debounceCliente.playFromStart();
        });


        // 3. Escuchador de Selección (LIMPIO)
        cmbCliente.setOnAction(event -> {

            if (actualizandoCliente) return;

            com.nakel.frontend.model.Cliente seleccionado =
                    cmbCliente.getSelectionModel().getSelectedItem();

            if (seleccionado != null) {
                // Eligió "Consumidor Final" de la lista: en vez de escribirlo, vaciamos el combo
                // (queda el texto gris). Se hace con runLater para no pisar al combo mientras selecciona.
                if (seleccionado.getId() != null && seleccionado.getId() == 0L) {
                    Platform.runLater(this::volverAConsumidorFinal);
                    return;
                }

                actualizandoCliente = true;
                cmbCliente.setValue(seleccionado);
                actualizandoCliente = false;
            }
        });


        // 4. Disparo inicial: traemos la primera página apenas abre la pantalla, SIN elegir a nadie:
        //    el combo arranca vacío mostrando "Consumidor Final" en gris.
        buscarYMostrarCliente("", 0, false);
    }

    private void buscarYMostrarCliente(String texto, int pagina) {
        // Solo desplegamos la lista si la usuaria está parada en el combo (escribiendo o buscando)
        boolean enFoco = cmbCliente.isFocused() || cmbCliente.getEditor().isFocused();
        buscarYMostrarCliente(texto, pagina, enFoco);
    }

    private void buscarYMostrarCliente(String texto, int pagina, boolean desplegar) {

        // 📡 Viajamos al Backend
        com.nakel.frontend.service.ClienteApiService clienteApi =
                new com.nakel.frontend.service.ClienteApiService();

        java.util.List<com.nakel.frontend.model.Cliente> resultados =
                clienteApi.buscarClientesPaginados(texto, pagina, 20);


        if (resultados != null) {
            for (com.nakel.frontend.model.Cliente c : resultados) {
            }
        }

        actualizandoCliente = true; // 🔒 CERRAMOS

        try {

            // 🔥 LA MAGIA: Si la barra está vacía, clavamos a Consumidor Final
            if (texto == null || texto.trim().isEmpty()) {

                com.nakel.frontend.model.Cliente consumidorFinal =
                        new com.nakel.frontend.model.Cliente();

                consumidorFinal.setId(0L);
                consumidorFinal.setNombre("Consumidor Final");
                consumidorFinal.setCuit("00000000");

                cmbCliente.getItems().add(consumidorFinal);
            }

            if (resultados != null && !resultados.isEmpty()) {
                // Agregamos los clientes reales de la base de datos
                for (com.nakel.frontend.model.Cliente c : resultados) {

                    // Filtramos por las dudas para no duplicar Consumidor Final
                    if (c.getNombre() != null
                            && !c.getNombre().equalsIgnoreCase("Consumidor Final")) {

                        cmbCliente.getItems().add(c);

                    }
                }
                if (desplegar) cmbCliente.show();

            } else if (pagina == 0 && cmbCliente.getItems().isEmpty()) {
                cmbCliente.hide();
            }

            cmbCliente.getEditor().setText(texto);
            cmbCliente.getEditor().positionCaret(texto.length());

        } finally {

            actualizandoCliente = false; // 🔓 ABRIMOS
        }
    }


    private void cargarInventarioParaVenta(String busqueda, int pagina) {
        try {
            // Llamamos al método del API service enviando la búsqueda, la página actual y el tamaño por página (100)
            List<Articulo> inventario = articuloApi.buscarParaVenta(busqueda, pagina, 100);

            masterInventario.clear();

            if (inventario != null && !inventario.isEmpty()) {
                masterInventario.setAll(inventario);

                // 🔥 LA MAGIA VISUAL: Le avisamos al componente cuántas páginas existen
                int paginasReales = articuloApi.getUltimasPaginasMostrador();
                if (paginadorInventario != null) {
                    paginadorInventario.setPageCount(paginasReales);
                }
            } else {
                if (paginadorInventario != null) {
                    paginadorInventario.setPageCount(1);
                }
                System.out.println("⚠️ El inventario está vacío en esta página.");
            }

            filteredInventario = new FilteredList<>(masterInventario, p -> true);
            tablaInventario.setItems(filteredInventario);

        } catch (Exception e) {
            System.out.println("❌ Error al cargar inventario paginado en mostrador: " + e.getMessage());
        }
    }

    // 🔥 1. Agregamos el Vale como segundo parámetro
    private void ejecutarProcesoDeCierreDeVenta(List<Pago> listaPagos, com.nakel.frontend.model.Vale valeUsado) {
        ejecutarProcesoDeCierreDeVenta(listaPagos, valeUsado, null, 0.0, null);
    }

    // montoValeUsado: cuánto del vale se usó (null = todo). vueltoEnVale: vuelto que se entrega como vale
    // (se genera recién cuando la venta quedó guardada). idClienteVale: a nombre de quién va ese vale.
    private void ejecutarProcesoDeCierreDeVenta(List<Pago> listaPagos, com.nakel.frontend.model.Vale valeUsado,
                                                Double montoValeUsado, double vueltoEnVale, Long idClienteVale) {
        if (tablaTicket.getItems().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Ticket vacío", "No hay productos en el mostrador para cobrar.");
            return;
        }

        // 1. Convertimos los ítems de la tabla visual a DetalleVenta
        List<DetalleVenta> detalles = new ArrayList<>();
        for (LineaTicket linea : tablaTicket.getItems()) {
            detalles.add(new DetalleVenta(
                    linea.getCantidad(),
                    linea.getArticulo().getPrecio(),
                    linea.getSubtotal(),
                    linea.getArticulo()
            ));
        }
        // 2. Cliente elegido en el combo (null = combo vacío = Consumidor Final)
        com.nakel.frontend.model.Cliente clienteSeleccionado = clienteElegido();
        com.nakel.frontend.model.Cliente clienteParaBackend;

        // Si es un cliente real (ID mayor a 0)
        if (clienteSeleccionado != null && clienteSeleccionado.getId() != null && clienteSeleccionado.getId() > 0) {
            clienteParaBackend = clienteSeleccionado;
        } else {
            // 🔥 ESCUDO: Fallback para Consumidor Final genérico
            // Le mandamos el CUIT 00000000 para que tu Backend lo encuentre sin explotar
            clienteParaBackend = new com.nakel.frontend.model.Cliente();
            clienteParaBackend.setNombre("Consumidor Final");
            clienteParaBackend.setCuit("00000000");
        }

        // 3. Armar la entidad Venta
        com.nakel.frontend.model.Venta ventaFinal = new com.nakel.frontend.model.Venta(
                clienteParaBackend,
                obtenerTotalNumerico(),
                true,  // esFiscal
                false, // esTicketCambio (es venta original)
                detalles,
                listaPagos
        );

        // 4. Configurar banderas de regalo / comprobante

        boolean esRegalo = chkRegalo.isSelected();
        ventaFinal.setEsParaRegalo(esRegalo);
        // El combo "Tipo de factura" antes no se leía nunca: un Presupuesto se cobraba como venta real.
        // Ahora "Presupuesto" viaja al backend, que NO descuenta stock ni lo suma a la caja.
        boolean esPresupuesto = cmbTipoFactura != null && "Presupuesto".equals(cmbTipoFactura.getValue());
        if (esPresupuesto) {
            ventaFinal.setTipoComprobante("Presupuesto");
        } else {
            ventaFinal.setTipoComprobante(esRegalo ? "TICKET_REGALO" : "TICKET_NORMAL");
        }

        // 5. Registrar en Backend
        VentaApiService apiVentas = new VentaApiService();
        boolean exito = apiVentas.registrarVenta(ventaFinal);

        if (exito) {
            com.nakel.frontend.service.ValeApiService valeApi = new com.nakel.frontend.service.ValeApiService();
            StringBuilder avisos = new StringBuilder();

            // Si la venta se guardó bien y se usó un vale, descontamos SOLO lo que se usó
            if (valeUsado != null && !esPresupuesto) {
                boolean consumido = valeApi.consumirVale(valeUsado.getCodigo(), montoValeUsado);
                if (!consumido) {
                    avisos.append("\n\nATENCIÓN: no se pudo descontar el vale ").append(valeUsado.getCodigo())
                            .append(". Anotalo para que no se use dos veces.");
                }
            }

            // Vuelto entregado como vale: se crea recién ahora, con la venta ya guardada
            if (vueltoEnVale > 0.009 && !esPresupuesto) {
                com.nakel.frontend.model.Vale valeVuelto = valeApi.generarVale(vueltoEnVale, idClienteVale);
                if (valeVuelto != null) {
                    avisos.append("\n\nVALE POR EL VUELTO: ").append(com.nakel.frontend.util.FormatoMoneda.pesos(vueltoEnVale))
                            .append("\nCÓDIGO: ").append(valeVuelto.getCodigo());
                } else {
                    avisos.append("\n\nATENCIÓN: no se pudo generar el vale por el vuelto de ")
                            .append(com.nakel.frontend.util.FormatoMoneda.pesos(vueltoEnVale)).append(". Entregá el vuelto en mano.");
                }
            }

            mostrarAlerta(Alert.AlertType.INFORMATION,
                    esPresupuesto ? "Presupuesto guardado" : "¡Venta Exitosa!",
                    (esPresupuesto
                            ? "El presupuesto se guardó. No se descontó stock ni se sumó a la caja.\n\n"
                                + "Lo encontrás en el Historial de Ventas para imprimirlo o guardarlo en PDF."
                            : "La transacción se registró correctamente.") + avisos);

            // 6. Limpiar mostrador para la siguiente venta
            tablaTicket.getItems().clear();
            actualizarTotal();
            chkRegalo.setSelected(false);
            if (cmbTipoFactura != null) cmbTipoFactura.setValue("Ticket de Venta");

            // Recargamos el inventario del mostrador: sin esto quedaba el stock viejo en pantalla
            // y se podía volver a vender una unidad que ya no existía.
            txtCodigoBarras.clear();
            cargarInventarioParaVenta("", 0);

            // El combo vuelve a quedar vacío: se ve "Consumidor Final" en gris para el próximo cliente
            volverAConsumidorFinal();

            txtCodigoBarras.requestFocus();
        } else {

            // Mostramos el motivo real (ej: "Stock insuficiente de ...") y refrescamos el stock en pantalla
            String motivo = apiVentas.getUltimoError();
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo registrar la venta",
                    (motivo != null ? motivo : "No se pudo guardar la venta en la Base de Datos.")
                            + "\n\nEl ticket quedó cargado: no se perdió nada.");
            cargarInventarioParaVenta("", 0);
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    @FXML
    public void cobrarVenta(ActionEvent event) {
        String medioPago = cmbMedioPago.getValue();
        boolean presupuesto = esModoPresupuesto();

        if (!presupuesto && (medioPago == null || medioPago.isBlank())) {
            mostrarAlerta(Alert.AlertType.WARNING, "Falta el medio de pago", "Elegí con qué paga el cliente antes de cobrar.");
            return;
        }

        // Si escribió un nombre pero no eligió a nadie de la lista, frenamos: así la venta
        // no queda a nombre de un cliente equivocado ni de Consumidor Final por error.
        if (clienteEscritoSinElegir()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Falta elegir el cliente",
                    "Escribiste \"" + cmbCliente.getEditor().getText().trim() + "\" pero no elegiste ningún cliente de la lista.\n\n"
                            + "Elegí uno de la lista, o borrá el texto para vender a Consumidor Final.");
            cmbCliente.requestFocus();
            return;
        }

        if (presupuesto) {
            // Un presupuesto no lleva pagos ni abre la ventana de cobro: se guarda directo
            ejecutarProcesoDeCierreDeVenta(new ArrayList<>(), null);
        } else if ("Pago Mixto".equals(medioPago)) {
            abrirVentanaPagoMixto();
        } else {
            List<Pago> pagos = new ArrayList<>();
            pagos.add(new Pago(medioPago, obtenerTotalNumerico()));
            // 🔥 Pasamos "null" porque en un pago simple no se usan vales
            ejecutarProcesoDeCierreDeVenta(pagos, null);
        }
    }

    @FXML
    public void abrirClienteExpress(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/cliente-express-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            // 🔥 CAMBIO 1: El Dialog ahora maneja y espera un OBJETO Cliente, no un simple String
            javafx.scene.control.Dialog<com.nakel.frontend.model.Cliente> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Cliente nuevo");
            dialog.setHeaderText("Alta rápida de cliente");
            dialog.getDialogPane().setContent(root);

            javafx.scene.control.ButtonType btnGuardar = new javafx.scene.control.ButtonType("Guardar", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, javafx.scene.control.ButtonType.CANCEL);
            dialog.getDialogPane().lookupButton(btnGuardar).getStyleClass().add("btn-primario");

            ClienteExpressController controladorModal = loader.getController();

            dialog.setResultConverter(btn -> {
                // 🔥 CAMBIO 2: procesarGuardado() debe guardar en la BD y devolver el Cliente real
                if (btn == btnGuardar) {
                    return controladorModal.procesarGuardado();
                }
                return null;
            });

            // 🔥 CAMBIO 3: Atrapamos el cliente real (con su ID y CUIT) y lo metemos al ComboBox
            dialog.showAndWait().ifPresent(clienteReal -> {
                if (clienteReal != null) {
                    // Si el ComboBox no lo tiene en su lista, lo agregamos
                    if (!cmbCliente.getItems().contains(clienteReal)) {
                        cmbCliente.getItems().add(clienteReal);
                    }
                    // Lo dejamos seleccionado automáticamente para cobrar al instante
                    cmbCliente.setValue(clienteReal);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void abrirVentanaPagoMixto() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/pago-mixto-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Cobro Dividido / Mixto");

            javafx.scene.control.ButtonType btnFacturar = new javafx.scene.control.ButtonType("Emitir Factura", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnFacturar, javafx.scene.control.ButtonType.CANCEL);

            javafx.scene.Node botonFacturarNode = dialog.getDialogPane().lookupButton(btnFacturar);
            botonFacturarNode.getStyleClass().add("btn-primario");
            botonFacturarNode.setDisable(true);

            PagoMixtoController controladorModal = loader.getController();

            // 🔥 NUEVO: Atrapamos el ID directo desde el objeto Cliente (¡Chau split y chau For!)
            com.nakel.frontend.model.Cliente clienteSeleccionado = clienteElegido();
            Long idClienteParaVale = null;

            if (clienteSeleccionado != null && clienteSeleccionado.getId() != null && clienteSeleccionado.getId() > 0) {
                idClienteParaVale = clienteSeleccionado.getId(); // ¡Atrapamos el ID directo del objeto!
            }

            // 🔥 NUEVO: Le pasamos el idClienteParaVale al controlador del modal
            controladorModal.inicializarValores(obtenerTotalNumerico(), (javafx.scene.control.Button) botonFacturarNode, idClienteParaVale);

            dialog.getDialogPane().setContent(root);

            dialog.setResultConverter(btn -> {
                if (btn == btnFacturar && controladorModal.isPagoCompleto()) {
                    List<Pago> pagos = controladorModal.getPagosRegistrados();
                    com.nakel.frontend.model.Vale valeAplicado = controladorModal.getValeAplicado();
                    ejecutarProcesoDeCierreDeVenta(pagos, valeAplicado,
                            valeAplicado != null ? controladorModal.getMontoValeUsado() : null,
                            controladorModal.getVueltoPendiente(),
                            controladorModal.getIdCliente());
                }
                return null;
            });

            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void actualizarTotal() {
        double total = 0.0;
        for (LineaTicket item : tablaTicket.getItems()) {
            total += item.getSubtotal();
        }
        // Solo el monto: la palabra "TOTAL:" ya la pone la etiqueta de al lado (antes se leía "TOTAL: Total: $0.00")
        lblTotal.setText(com.nakel.frontend.util.FormatoMoneda.pesos(total));
    }

    private void agregarAlTicket(Articulo articulo) {
        if (articulo == null) return;

        for (LineaTicket linea : tablaTicket.getItems()) {
            if (linea.getArticulo().getCodigo().equals(articulo.getCodigo())) {
                if (linea.getCantidad() + 1 > articulo.getStockActual()) {
                    mostrarAlerta(Alert.AlertType.WARNING, "Stock Insuficiente", "Solo quedan " + articulo.getStockActual() + " unidades de " + articulo.getNombre());
                    return;
                }
                linea.sumarCantidad();
                tablaTicket.refresh();
                actualizarTotal();
                return;
            }
        }

        if (articulo.getStockActual() < 1) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin Stock", "El producto " + articulo.getNombre() + " no tiene stock disponible.");
            return;
        }

        tablaTicket.getItems().add(new LineaTicket(articulo));
        actualizarTotal();
    }
}