package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.nakel.frontend.model.Articulo;
import com.nakel.frontend.model.DetalleVenta;
import com.nakel.frontend.model.LineaTicket;
import com.nakel.frontend.model.Pago;
import com.nakel.frontend.service.ArticuloApiService;
import com.nakel.frontend.service.VentaApiService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;

public class VentaController {

    private final ArticuloApiService apiService = new ArticuloApiService();

    // Cabecera AFIP
    @FXML private ComboBox<String> cmbCliente;
    @FXML private ComboBox<String> cmbTipoFactura;

    // Buscador y Tabla
    @FXML private TextField txtCodigoBarras;
    @FXML private TableView<LineaTicket> tablaTicket;
    //
    @FXML
    private TableView<Articulo> tablaInventario;

    // Totales y Cobro
    @FXML private Label lblTotal;
    @FXML private ComboBox<String> cmbMedioPago;
    //check para el regalo
    @FXML private javafx.scene.control.CheckBox chkRegalo;

    @FXML
    private TableView<Articulo> tablaProductosPOS;

    //segunda tabla
    @FXML private TableColumn<Articulo, String> colInvCodigo;
    @FXML private TableColumn<Articulo, String> colInvNombre;
    @FXML private TableColumn<Articulo, Double> colInvPrecio;

    // Instanciamos el servicio y Gson para procesar
    private final ArticuloApiService articuloApi = new ArticuloApiService();
    private final Gson gson = new Gson();

    @FXML private TableColumn<LineaTicket, String> colCodigo;
    @FXML private TableColumn<LineaTicket, String> colNombre;
    @FXML private TableColumn<LineaTicket, Integer> colCantidad;
    @FXML private TableColumn<LineaTicket, Double> colPrecio; // <--- Cambió
    @FXML private TableColumn<LineaTicket, Double> colSubtotal;
    @FXML private TableColumn<LineaTicket, Void> colAccion;

    @FXML
    public void initialize() {

        configurarEventos(); //"enchufa" EJ: el "texfield" del codigo de barra que "ProcesarBUSQUEDA"
        configurarTabla(); // logica visual7
        configurarTablaInventario();
        cargarInventarioCompleto();

        cargarDatosIniciales(); //carga los datos
        cargarInventarioParaVenta();
        System.out.println("Terminal de Punto de Venta (POS) Iniciada.");
    }

    private void configurarEventos() {

        // Buscar por código o nombre cuando presionan Enter
        txtCodigoBarras.setOnAction(event ->
                procesarBusqueda(txtCodigoBarras.getText())
        );

        /* Doble click en un producto del inventario
        tablaInventario.setOnMouseClicked(event -> {

            if (event.getClickCount() == 2) {

                Articulo seleccionado =
                        tablaInventario.getSelectionModel().getSelectedItem();

                if (seleccionado != null) {

                    tablaTicket.getItems().add(seleccionado);

                    actualizarTotal();

                    System.out.println("✅ Agregado al ticket: "
                            + seleccionado.getNombre());
                }
            }
        });*/
        // DOBLE CLICK en inventario
        tablaInventario.setOnMouseClicked(event -> {

            if (event.getClickCount() == 2) {

                Articulo seleccionado =
                        tablaInventario.getSelectionModel().getSelectedItem();

                if (seleccionado != null) {
                    agregarAlTicket(seleccionado);
                }
            }
        });

        // ENTER en inventario
        tablaInventario.setOnKeyPressed(event -> {

            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {

                Articulo seleccionado =
                        tablaInventario.getSelectionModel().getSelectedItem();

                if (seleccionado != null) {
                    agregarAlTicket(seleccionado);
                }

                event.consume(); // evita comportamiento default de TableView
            }
        });
    }

    private void procesarBusqueda(String texto) {
        if (texto == null || texto.isBlank()) return;

        System.out.println("Buscando: " + texto);

        // Llamamos una sola vez a la API
        String json = articuloApi.buscarArticuloPorCodigo(texto);
        System.out.println("DEBUG - JSON recibido: " + json);

        if (json == null || json.isBlank() || json.equals("null")) {
            System.out.println("❌ Producto no encontrado en el Backend");
            return;
        }

        try {
            Articulo item = gson.fromJson(json, Articulo.class);

            if (item.getNombre() == null) {
                System.out.println("⚠️ Objeto creado, pero el nombre está vacío...");
            } else {
                // Ya no hacemos add directo, llamamos a la función inteligente
                agregarAlTicket(item);
                txtCodigoBarras.clear();
            }
        }catch (Exception e) {
            System.out.println("❌ ERROR FATAL en GSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void buscarProducto(ActionEvent event) {

        procesarBusqueda(txtCodigoBarras.getText());
    }

    private void configurarTabla() {
        tablaTicket.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 1. Datos que están adentro del Articulo, que está adentro de LineaTicket
        colCodigo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getArticulo().getCodigo()));
        colNombre.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getArticulo().getNombre()));
        colPrecio.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getArticulo().getPrecio()).asObject());

        // 2. Datos de Cantidad + 💥 ¡BOTONES MAS Y MENOS!
        colCantidad.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("cantidad"));
        colCantidad.setCellFactory(param -> new TableCell<LineaTicket, Integer>() {
            private final Button btnMenos = new Button("-");
            private final Label lblCantidad = new Label();
            private final Button btnMas = new Button("+");
            // Metemos los tres elementos en una cajita horizontal con 8px de separación
            private final javafx.scene.layout.HBox panel = new javafx.scene.layout.HBox(8, btnMenos, lblCantidad, btnMas);

            {
                panel.setAlignment(javafx.geometry.Pos.CENTER);
                btnMenos.getStyleClass().add("btn-cantidad-accion"); // Clases para darles estilo en el CSS
                btnMas.getStyleClass().add("btn-cantidad-accion");

                // LÓGICA DEL BOTÓN "+"
                btnMas.setOnAction(e -> {
                    LineaTicket linea = getTableView().getItems().get(getIndex());

                    // 🛑 CONTROL DE STOCK: Solo suma si no supera el stock actual del artículo
                    if (linea.getCantidad() < linea.getArticulo().getStockActual()) {
                        linea.setCantidad(linea.getCantidad() + 1);

                        // Recalculamos el subtotal de este renglón
                        //linea.setSubtotal(linea.getArticulo().getPrecio() * linea.getCantidad());

                        getTableView().refresh(); // Refresca la vista de la tabla
                        actualizarTotal();        // 🔥 Llama a tu método para actualizar el número gigante
                    } else {
                        System.out.println("❌ No podés agregar más, es el límite del stock.");
                    }
                });

                // LÓGICA DEL BOTÓN "-"
                btnMenos.setOnAction(e -> {
                    LineaTicket linea = getTableView().getItems().get(getIndex());

                    // Solo restamos si es mayor a 1 (si quieren dejarlo en 0 usan la ❌)
                    if (linea.getCantidad() > 1) {
                        linea.setCantidad(linea.getCantidad() - 1);
                        //linea.setSubtotal(linea.getArticulo().getPrecio() * linea.getCantidad());

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

        // 3. Subtotal
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        // 4. El Botón de Eliminar (Tu código original intacto)
        colAccion.setCellFactory(param -> new TableCell<LineaTicket, Void>() {
            private final Button btnEliminar = new Button("❌");
            {
                btnEliminar.getStyleClass().add("btn-eliminar");
                btnEliminar.setOnAction(e -> {
                    LineaTicket linea = getTableView().getItems().get(getIndex());
                    tablaTicket.getItems().remove(linea);
                    actualizarTotal();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnEliminar);
            }
        });
    }

    private void configurarTablaInventario() {

        tablaInventario.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colInvCodigo.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getCodigo())
        );

        colInvNombre.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getNombre())
        );

        colInvPrecio.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getPrecio()).asObject()
        );
    }

    private void cargarDatosIniciales() {
        cmbTipoFactura.getItems().addAll("Factura C", "Remito", "Presupuesto");
        cmbTipoFactura.setValue("Factura C");

        cmbMedioPago.getItems().addAll("Efectivo", "Transferencia", "MercadoPago", "Tarjeta de Crédito", "Pago Mixto");
        cmbMedioPago.setValue("Efectivo");
    }

    private void cargarInventarioCompleto() {
        System.out.println("Cargando inventario completo para el POS...");
        // Usamos el mismo servicio que el catálogo, porque es la misma API
        System.out.println("DEBUG - llamando backend...");
        List<Articulo> inventario = apiService.obtenerTodos();
        System.out.println("DEBUG - respuesta: " + inventario);

        if (inventario != null && !inventario.isEmpty()) {
            System.out.println("SIZE: " + inventario.size());

            // Acá podrías cargar una lista oculta o un ComboBox gigante
            // para que la clienta busque/seleccione los productos
            System.out.println("✅ Mostrador listo con " + inventario.size() + " productos.");
        } else {
            System.out.println("⚠️ El inventario está vacío.");
        }
    }

    private void cargarInventarioParaVenta() {

        List<Articulo> inventario = apiService.obtenerTodos();

        if (inventario != null && !inventario.isEmpty()) {
            System.out.println("SIZE: " + inventario.size());

            ObservableList<Articulo> listaObservable =
                    FXCollections.observableArrayList(inventario);

            tablaInventario.setItems(listaObservable);

            System.out.println("✅ Inventario cargado: "
                    + inventario.size() + " productos.");
        } else {
            System.out.println("⚠️ El inventario está vacío.");
        }
    }

    //---------------------------------------------------------------------------------------------------------------------------
    private double obtenerTotalNumerico() {
        // AHORA recorre LineaTicket y suma los subtotales reales (precio * cantidad)
        return tablaTicket.getItems().stream()
                .mapToDouble(LineaTicket::getSubtotal)
                .sum();
    }





    //
    private void ejecutarProcesoDeCierreDeVenta(List<com.nakel.frontend.model.Pago> listaPagos) {
        // 1. Validación de seguridad básica: que no cobren un ticket vacío
        if (tablaTicket.getItems().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Ticket vacío", "No hay productos en el mostrador para cobrar.");
            return;
        }

        // 2. Mapeamos las líneas del ticket visual a objetos puros de DetalleVenta
        List<com.nakel.frontend.model.DetalleVenta> detalles = new ArrayList<>();
        for (LineaTicket linea : tablaTicket.getItems()) {
            detalles.add(new com.nakel.frontend.model.DetalleVenta(
                    linea.getCantidad(),
                    linea.getArticulo().getPrecio(),
                    linea.getSubtotal(),
                    linea.getArticulo()
            ));
        }

        // 3. Extraemos el Cliente seleccionado (limpiando el String del ComboBox)
        String clienteSeleccionado = cmbCliente.getValue();
        com.nakel.frontend.model.Cliente clienteParaBackend = new com.nakel.frontend.model.Cliente();

        if (clienteSeleccionado != null && clienteSeleccionado.contains(" - ")) {
            String[] partes = clienteSeleccionado.split(" - ");
            // Guardamos solo el CUIT/DNI en el objeto (la posición 1 del corte)
            clienteParaBackend.setCuit(partes[1].trim());
        }

        // 4. Armamos el objeto Venta consolidado para mandar al Backend
        com.nakel.frontend.model.Venta ventaFinal = new com.nakel.frontend.model.Venta(
                clienteParaBackend, // 🔥 ACÁ PASAMOS EL OBJETO, YA NO ES UN STRING
                obtenerTotalNumerico(),
                true, // esFiscal (el VentaService del backend recalculará esto)
                chkRegalo.isSelected(),
                detalles,
                listaPagos
        );

        // 5. Impactamos en la API
        VentaApiService apiVentas = new VentaApiService();
        boolean exito = apiVentas.registrarVenta(ventaFinal);

        if (exito) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "¡Venta Exitosa!", "La transacción se registró correctamente en el sistema.");

            // 🧼 LIMPIEZA POST-VENTA: Dejamos la caja lista para la próxima clienta
            tablaTicket.getItems().clear();
            actualizarTotal();
            chkRegalo.setSelected(false);
            cmbCliente.setValue(null); // O el cliente por defecto que manejes
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de Red", "No se pudo guardar la venta. Revisá si el Backend está encendido.");
        }
    }

    // Método auxiliar rápido para los Pop-ups de aviso
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    //

    // 2. Así queda tu cobrarVenta modificado
    @FXML
    public void cobrarVenta(ActionEvent event) {
        String medioPago = cmbMedioPago.getValue();

        if ("Pago Mixto".equals(medioPago)) {
            abrirVentanaPagoMixto();
        } else {
            // Es un pago simple de un solo método. Armamos la lista con un único pago.
            List<com.nakel.frontend.model.Pago> pagos = new ArrayList<>();
            pagos.add(new com.nakel.frontend.model.Pago(medioPago, obtenerTotalNumerico()));

            // Disparamos el proceso central
            ejecutarProcesoDeCierreDeVenta(pagos);
        }
    }

    @FXML
    public void abrirClienteExpress(ActionEvent event) {
        try {
            // 1. Cargamos el diseño visual desde el archivo FXML
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/cliente-express-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            // 2. Preparamos el Pop-up (Dialog)
            javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Alta Exprés");
            dialog.setHeaderText("Cargar nuevo cliente rápido");
            dialog.getDialogPane().setContent(root);

            // 3. Agregamos los botones
            javafx.scene.control.ButtonType btnGuardar = new javafx.scene.control.ButtonType("💾 Guardar", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, javafx.scene.control.ButtonType.CANCEL);

            // Le damos tu estilo dorado Nakel al botón Guardar
            javafx.scene.Node botonGuardarNode = dialog.getDialogPane().lookupButton(btnGuardar);
            botonGuardarNode.getStyleClass().add("btn-primario");

            // 4. Conectamos con el nuevo Controlador del Modal
            ClienteExpressController controladorModal = loader.getController();

            // 5. ¿Qué pasa cuando el cajero presiona "Guardar"?
            dialog.setResultConverter(btn -> {
                if (btn == btnGuardar) {
                    // Delegamos toda la lógica de la API al otro controlador
                    return controladorModal.procesarGuardado(); // Devuelve "Nombre - CUIT"
                }
                return null;
            });

            // 6. Si el proceso fue exitoso, metemos al cliente en el ComboBox del mostrador
            dialog.showAndWait().ifPresent(resultado -> {
                if (resultado != null) {
                    if (!cmbCliente.getItems().contains(resultado)) {
                        cmbCliente.getItems().add(resultado);
                    }
                    cmbCliente.setValue(resultado);
                    System.out.println("✅ Cliente rápido seleccionado en el mostrador.");
                }
            });

        } catch (Exception e) {
            System.err.println("❌ Error al abrir la ventana de Alta Exprés.");
            e.printStackTrace();
        }
    }

//
private void abrirVentanaPagoMixto() {
    try {
        // 1. Cargamos el diseño visual desde el archivo FXML (¡Adiós a dibujar con código!)
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/pago-mixto-modal.fxml"));
        javafx.scene.Parent root = loader.load();

        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Cobro Dividido / Mixto");
        dialog.setHeaderText("Agregá los pagos hasta completar el total");

        javafx.scene.control.ButtonType btnFacturar = new javafx.scene.control.ButtonType("✅ Emitir Factura", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnFacturar, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.Node botonFacturarNode = dialog.getDialogPane().lookupButton(btnFacturar);
        botonFacturarNode.getStyleClass().add("btn-primario");
        botonFacturarNode.setDisable(true); // Arranca deshabilitado hasta que complete el pago

        // 2. 🔥 MAGIA PURA: Le pasamos el total REAL de la venta al nuevo controlador
        PagoMixtoController controladorModal = loader.getController();
        controladorModal.inicializarValores(obtenerTotalNumerico(), (javafx.scene.control.Button) botonFacturarNode);

        // 3. Metemos el diseño FXML adentro del Pop-up
        dialog.getDialogPane().setContent(root);

        // 4. ¿Qué pasa cuando hacen clic en "Emitir Factura"?
        dialog.setResultConverter(btn -> {
            if (btn == btnFacturar && controladorModal.isPagoCompleto()) {
                System.out.println("=====================================");
                System.out.println("💸 FACTURANDO PAGO MIXTO CON ÉXITO");
                System.out.println("=====================================");

                // 🚀 Próximo paso: Acá llamaremos al backend con la lista de pagos
                List<Pago> pagos = controladorModal.getPagosRegistrados();
                ejecutarProcesoDeCierreDeVenta(pagos);
            }
            return null;
        });

        dialog.showAndWait();

    } catch (Exception e) {
        System.err.println("Error al abrir la ventana de pago mixto.");
        e.printStackTrace();
    }
}

    // 🧮 Método para sumar los precios de la tabla
    private void actualizarTotal() {
        double total = 0.0;
        for (LineaTicket item : tablaTicket.getItems()) { // AHORA RECORRE LineaTicket
            total += item.getSubtotal();
        }
        lblTotal.setText("Total: $" + String.format("%.2f", total));
    }

    private void agregarAlTicket(Articulo articulo) {
        if (articulo == null) return;

        // Buscamos si el producto ya está en el ticket
        for (LineaTicket linea : tablaTicket.getItems()) {
            if (linea.getArticulo().getCodigo().equals(articulo.getCodigo())) {
                // 🔥 VALIDACIÓN DE STOCK (Deuda del PDF saldada)
                if (linea.getCantidad() + 1 > articulo.getStockActual()) {
                    System.out.println("❌ ¡NO HAY STOCK SUFICIENTE! Quedan: " + articulo.getStockActual());
                    // Acá le podrías meter un Alert de JavaFX a futuro
                    return;
                }

                linea.sumarCantidad();
                tablaTicket.refresh();
                actualizarTotal();
                return;
            }
        }

        // Si es la primera vez que lo escanea, validamos stock igual
        if (articulo.getStockActual() < 1) {
            System.out.println("❌ ESTE PRODUCTO NO TIENE STOCK (0).");
            return;
        }

        // Si no existe, lo metemos adentro de una LineaTicket nueva
        tablaTicket.getItems().add(new LineaTicket(articulo));
        actualizarTotal();
    }
}