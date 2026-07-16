package com.nakel.frontend.controller;

import com.nakel.frontend.model.DetalleVenta;
import com.nakel.frontend.model.Venta;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.UUID;

public class CambioVentaController {

    // 🗃️ Listas para manejar el catálogo completo filtrable
    private javafx.collections.ObservableList<DetalleVenta> masterInventario = javafx.collections.FXCollections.observableArrayList();
    private javafx.collections.transformation.FilteredList<DetalleVenta> filteredInventario;

    @FXML private Label lblTituloVenta;
    @FXML private Label lblCliente;

    // 🔥 Tablas y Columnas (Izquierda: Devuelve)
    @FXML private TableView<DetalleVenta> tablaDevolucion;
    @FXML private TableColumn<DetalleVenta, String> colDevCant;
    @FXML private TableColumn<DetalleVenta, String> colDevDesc;
    @FXML private TableColumn<DetalleVenta, String> colDevPrecio;

    // 🔥 Tablas y Columnas (Derecha: Lleva)
    @FXML private TableView<DetalleVenta> tablaNuevos;
    @FXML private TableColumn<DetalleVenta, String> colNueCant;
    @FXML private TableColumn<DetalleVenta, String> colNueDesc;
    @FXML private TableColumn<DetalleVenta, String> colNuePrecio;

    @FXML private Label lblSaldoFavor;
    @FXML private Label lblCostoNuevos;
    @FXML private Label lblDiferenciaTexto;
    @FXML private Label lblDiferenciaMonto;
    @FXML private Label lblMensajeVoucher;

    // 🔍 Barra de búsqueda y Servicios
    @FXML private TextField txtBuscarNuevo;
    private final com.nakel.frontend.service.ArticuloApiService articuloApi = new com.nakel.frontend.service.ArticuloApiService();
    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    private Venta ventaOriginal;

    // Variables para guardar la matemática pura
    private double saldoAFavorCliente = 0.0;
    private double costoNuevosProductos = 0.0;

    @FXML
    public void initialize() {
        tablaDevolucion.setPlaceholder(new Label("No hay artículos para devolver."));
        tablaNuevos.setPlaceholder(new Label("Cargando inventario..."));
        // 1. Configuramos la Tabla Izquierda (Lo que devuelve)
        configurarColumnaCantidadDevolucion();
        colDevDesc.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getArticulo().getNombre()));
        colDevPrecio.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty("$ " + cell.getValue().getPrecioUnitario()));

        // 2. Configuramos la Tabla Derecha (🔥 AHORA CON BOTONES Y CATÁLOGO REAL)
        configurarColumnaCantidadNuevos(); // Tu nuevo método privado
        colNueDesc.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getArticulo().getNombre()));
        colNuePrecio.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty("$ " + cell.getValue().getPrecioUnitario()));

        // 🔥 Cargamos la góndola entera de entrada
        cargarCatalogoCompleto();
    }

    public void cargarVentaOriginal(Venta venta) {
        this.ventaOriginal = venta;

        lblTituloVenta.setText("Gestión de Cambio - Venta #" + String.format("%08d", venta.getId()));
        lblCliente.setText("Cliente: " + (venta.getCliente() != null ? venta.getCliente().getNombre() : "Consumidor Final"));

        // 🔥 MAGIA APLICADA: Llenamos la tabla izquierda con los productos reales
        // Guardamos la cantidad original vendida para limitar la devolución
        if (venta.getDetalles() != null) {
            for (DetalleVenta det : venta.getDetalles()) {
                det.setCantidadOriginal(det.getCantidad());
            }

            tablaDevolucion.setItems(javafx.collections.FXCollections.observableArrayList(venta.getDetalles()));
        }

        saldoAFavorCliente = venta.getTotal();
        lblSaldoFavor.setText(String.format("$ %.2f", saldoAFavorCliente));

        calcularDiferencias();
    }

    // 🔥 EL NÚCLEO MATEMÁTICO DEL VOUCHER
    private void calcularDiferencias() {
        // Diferencia = Lo que se lleva (costo) - Lo que devuelve (favor)
        double diferencia = costoNuevosProductos - saldoAFavorCliente;

        if (diferencia > 0) {
            // El cliente eligió cosas más caras. Debe pagar la diferencia.
            lblDiferenciaTexto.setText("EL CLIENTE DEBE ABONAR:");
            lblDiferenciaTexto.setStyle("-fx-text-fill: #d32f2f;"); // Rojo
            lblDiferenciaMonto.setText(String.format("$ %.2f", diferencia));
            lblMensajeVoucher.setVisible(false);

        } else if (diferencia < 0) {
            // El cliente eligió cosas más baratas. Le sobra plata (Saldo a favor / Voucher)
            lblDiferenciaTexto.setText("SALDO A FAVOR DEL CLIENTE:");
            lblDiferenciaTexto.setStyle("-fx-text-fill: #388e3c;"); // Verde
            lblDiferenciaMonto.setText(String.format("$ %.2f", Math.abs(diferencia)));

            lblMensajeVoucher.setVisible(true); // Prendemos el aviso naranja del voucher

        } else {
            // Cambio mano a mano
            lblDiferenciaTexto.setText("CAMBIO DIRECTO (Sin costo):");
            lblDiferenciaTexto.setStyle("-fx-text-fill: #333333;");
            lblDiferenciaMonto.setText("$ 0.00");
            lblMensajeVoucher.setVisible(false);
        }
    }

    @FXML
    public void buscarYAgregarNuevoProducto(ActionEvent event) {
        String busqueda = txtBuscarNuevo.getText();

        if (busqueda == null || busqueda.isBlank()) {
            return;
        }

        try {
            // 1. Buscamos el artículo en el backend usando tu API
            // (Ajustá "buscarProducto" al nombre exacto que tengas en ArticuloApiService)
            String jsonRespuesta = articuloApi.buscarProducto(busqueda.trim());

            if (jsonRespuesta != null && !jsonRespuesta.isBlank()) {
                // 2. Convertimos el JSON al objeto Articulo
                com.nakel.frontend.model.Articulo articuloEncontrado = gson.fromJson(jsonRespuesta, com.nakel.frontend.model.Articulo.class);

                // 3. Armamos el nuevo renglón para la tabla (DetalleVenta)
                DetalleVenta nuevoItem = new DetalleVenta();
                nuevoItem.setArticulo(articuloEncontrado);
                nuevoItem.setCantidad(1); // Por defecto agregamos 1 unidad
                // (Ajustá getPrecio() al nombre real de tu getter en Articulo)
                nuevoItem.setPrecioUnitario(articuloEncontrado.getPrecio());
                nuevoItem.setSubtotal(articuloEncontrado.getPrecio() * 1);

                // 4. Lo metemos en la tabla de Nuevos Artículos
                tablaNuevos.getItems().add(nuevoItem);

                // 5. Actualizamos los totales
                costoNuevosProductos += nuevoItem.getSubtotal();
                lblCostoNuevos.setText(String.format("$ %.2f", costoNuevosProductos));

                // 6. Recalculamos la matemática del voucher al instante
                calcularDiferencias();

                // 7. Limpiamos el campito para que la dueña pueda escanear otro
                txtBuscarNuevo.clear();

            } else {
                mostrarError("Producto no encontrado", "No se encontró ningún artículo con el código o nombre: " + busqueda);
                txtBuscarNuevo.selectAll();
            }

        } catch (Exception e) {
            mostrarError("Error de Conexión", "Hubo un problema al buscar el producto en el servidor: " + e.getMessage());
        }
    }

    // Método de ayuda para mostrar los carteles rojos de error rápido
    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    @FXML
    public void confirmarCambio(ActionEvent event) {
        double diferencia = costoNuevosProductos - saldoAFavorCliente;

        // 🛡️ PREPARACIÓN FISCAL (ARCA/AFIP)
        boolean requiereNotaCredito = (ventaOriginal.getEsFiscal() != null && ventaOriginal.getEsFiscal());

        if (requiereNotaCredito) {
            System.out.println("⚠️ ALERTA FISCAL: Ticket original con CAE. Preparando conexión con ARCA para emitir NOTA DE CRÉDITO.");
            // TODO: Etapa 4 - Llamar al Web Service de ARCA acá antes de seguir
        } else {
            System.out.println("✅ Movimiento de inventario interno. No requiere aviso a ARCA.");
        }

        // 💰 LÓGICA DE SALDOS
        if (diferencia < 0) {
            String codigoVoucher = "VOU-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            double montoVoucher = Math.abs(diferencia);

            String msjFiscal = requiereNotaCredito ? "\n(Se emitirá Nota de Crédito en ARCA)" : "";

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Cambio Procesado");
            alert.setHeaderText("¡Cambio Exitoso!");
            alert.setContentText("Stock actualizado." + msjFiscal + "\n\nSe ha generado el comprobante:\nCÓDIGO: " + codigoVoucher + "\nMONTO A FAVOR: $" + montoVoucher);
            alert.showAndWait();

        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Cambio Procesado");
            alert.setHeaderText("¡Cambio Exitoso!");
            alert.setContentText("Stock actualizado correctamente.\nSe debe cobrar al cliente: $" + diferencia);
            alert.showAndWait();
        }

        cerrarModal(event);
    }

    @FXML
    public void cerrarModal(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }


    // Método privado para mantener limpio el código
    private void configurarColumnaCantidadDevolucion() {
        colDevCant.setCellFactory(col -> new TableCell<DetalleVenta, String>() {
            private final Button btnMenos = new Button("-");
            private final Label lblCant = new Label();
            private final Button btnMas = new Button("+");
            private final javafx.scene.layout.HBox panel = new javafx.scene.layout.HBox(5, btnMenos, lblCant, btnMas);

            {
                panel.setAlignment(javafx.geometry.Pos.CENTER);
                btnMenos.setStyle("-fx-background-color: #ffcdd2; -fx-cursor: hand;");
                btnMas.setStyle("-fx-background-color: #c8e6c9; -fx-cursor: hand;");

                // Restar cantidad a devolver
                btnMenos.setOnAction(e -> {
                    DetalleVenta item = getTableView().getItems().get(getIndex());
                    if (item.getCantidad() > 1) {
                        item.setCantidad(item.getCantidad() - 1);
                        item.setSubtotal(item.getCantidad() * item.getPrecioUnitario());
                    } else {
                        // Si llega a 0, lo sacamos de la tabla porque significa que NO lo devuelve
                        getTableView().getItems().remove(item);
                    }
                    getTableView().refresh();
                    recalcularSaldoFavor(); // Actualizamos los números
                });

                // Sumar cantidad (Opcional, por si restó de más sin querer)
                // Sumar cantidad (sin superar la cantidad vendida originalmente)
                btnMas.setOnAction(e -> {
                    DetalleVenta item = getTableView().getItems().get(getIndex());

                    if (item.getCantidad() < item.getCantidadOriginal()) {
                        item.setCantidad(item.getCantidad() + 1);
                        item.setSubtotal(item.getCantidad() * item.getPrecioUnitario());

                        getTableView().refresh();
                        recalcularSaldoFavor();
                    } else {
                        mostrarError(
                                "Cantidad máxima alcanzada",
                                "No se pueden devolver más unidades de las que fueron vendidas."
                        );
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView().getItems().get(getIndex()) == null) {
                    setGraphic(null);
                } else {
                    DetalleVenta detalle = getTableView().getItems().get(getIndex());
                    lblCant.setText(String.valueOf(detalle.getCantidad()));
                    setGraphic(panel);
                }
            }
        });
    }

    // El método que actualiza la plata cuando tocás los botones
    private void recalcularSaldoFavor() {
        saldoAFavorCliente = 0.0;
        for (DetalleVenta det : tablaDevolucion.getItems()) {
            saldoAFavorCliente += det.getSubtotal();
        }
        lblSaldoFavor.setText(String.format("$ %.2f", saldoAFavorCliente));
        calcularDiferencias(); // Recalcula también la diferencia general
    }
    //--------------------------------
    // 🔥 Carga todo el inventario de tu local con cantidad inicial 0
    private void cargarCatalogoCompleto() {
        try {
            // Le pegamos a tu ApiService (obtenerTodos) igual que en el mostrador
            java.util.List<com.nakel.frontend.model.Articulo> articulosBBDD = articuloApi.obtenerTodos();

            if (articulosBBDD != null) {
                masterInventario.clear();
                for (com.nakel.frontend.model.Articulo art : articulosBBDD) {
                    // Creamos renglones en 0 pesos y 0 unidades usando tu constructor
                    DetalleVenta renglonCatalogo = new DetalleVenta(0, art.getPrecio(), 0.0, art);
                    masterInventario.add(renglonCatalogo);
                }

                // Enchufamos la lista al filtro de JavaFX
                filteredInventario = new javafx.collections.transformation.FilteredList<>(masterInventario, p -> true);
                tablaNuevos.setItems(filteredInventario);
                System.out.println("✅ Catálogo de cambios inicializado con " + articulosBBDD.size() + " productos.");
            }
        } catch (Exception e) {
            System.err.println("Error al precargar el inventario en cambios: " + e.getMessage());
        }
    }

    // 🔥 Botones de más y menos para la Góndola de la derecha
    private void configurarColumnaCantidadNuevos() {
        colNueCant.setCellFactory(col -> new TableCell<DetalleVenta, String>() {
            private final Button btnMenos = new Button("-");
            private final Label lblCant = new Label();
            private final Button btnMas = new Button("+");
            private final javafx.scene.layout.HBox panel = new javafx.scene.layout.HBox(5, btnMenos, lblCant, btnMas);

            {
                panel.setAlignment(javafx.geometry.Pos.CENTER);
                btnMenos.setStyle("-fx-background-color: #ffcdd2; -fx-cursor: hand;");
                btnMas.setStyle("-fx-background-color: #c8e6c9; -fx-cursor: hand;");

                // Restar unidades que se lleva
                btnMenos.setOnAction(e -> {
                    DetalleVenta item = getTableView().getItems().get(getIndex());
                    if (item.getCantidad() > 0) { // No permitimos menos de 0
                        item.setCantidad(item.getCantidad() - 1);
                        item.setSubtotal(item.getCantidad() * item.getPrecioUnitario());
                        getTableView().refresh();
                        recalcularCostoNuevos();
                    }
                });

                // Sumar unidades que se lleva
                // Sumar unidades que se lleva
                btnMas.setOnAction(e -> {
                    DetalleVenta item = getTableView().getItems().get(getIndex());

                    int stockDisponible = item.getArticulo().getStockActual();

                    if (item.getCantidad() < stockDisponible) {

                        item.setCantidad(item.getCantidad() + 1);
                        item.setSubtotal(item.getCantidad() * item.getPrecioUnitario());

                        getTableView().refresh();
                        recalcularCostoNuevos();

                    } else {
                        mostrarError(
                                "Stock insuficiente",
                                "Solo hay " + stockDisponible + " unidades disponibles."
                        );
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView().getItems().get(getIndex()) == null) {
                    setGraphic(null);
                } else {
                    DetalleVenta detalle = getTableView().getItems().get(getIndex());
                    lblCant.setText(String.valueOf(detalle.getCantidad()));
                    setGraphic(panel);
                }
            }
        });
    }

    // Recorre toda la lista maestra sumando solo lo que tenga cantidad > 0
    private void recalcularCostoNuevos() {
        costoNuevosProductos = 0.0;
        for (DetalleVenta det : masterInventario) {
            costoNuevosProductos += det.getSubtotal();
        }
        lblCostoNuevos.setText(String.format("$ %.2f", costoNuevosProductos));
        calcularDiferencias(); // Actualiza el vuelto/deuda al instante
    }
}