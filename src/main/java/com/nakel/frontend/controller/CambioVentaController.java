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

    private javafx.collections.ObservableList<DetalleVenta> masterInventario = javafx.collections.FXCollections.observableArrayList();
    private javafx.collections.transformation.FilteredList<DetalleVenta> filteredInventario;

    @FXML private Label lblTituloVenta;
    @FXML private Label lblCliente;

    @FXML private TableView<DetalleVenta> tablaDevolucion;
    @FXML private TableColumn<DetalleVenta, String> colDevCant;
    @FXML private TableColumn<DetalleVenta, String> colDevDesc;
    @FXML private TableColumn<DetalleVenta, String> colDevPrecio;

    @FXML private TableView<DetalleVenta> tablaNuevos;
    @FXML private TableColumn<DetalleVenta, String> colNueCant;
    @FXML private TableColumn<DetalleVenta, String> colNueDesc;
    @FXML private TableColumn<DetalleVenta, String> colNuePrecio;

    @FXML private Label lblSaldoFavor;
    @FXML private Label lblCostoNuevos;
    @FXML private Label lblDiferenciaTexto;
    @FXML private Label lblDiferenciaMonto;
    @FXML private Label lblMensajeVoucher;
    @FXML private TextField txtBuscarNuevo;

    private final com.nakel.frontend.service.ArticuloApiService articuloApi = new com.nakel.frontend.service.ArticuloApiService();
    private final com.nakel.frontend.service.ValeApiService valeApi = new com.nakel.frontend.service.ValeApiService();
    private final com.nakel.frontend.service.CambioApiService cambioApi = new com.nakel.frontend.service.CambioApiService();
    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    private Venta ventaOriginal;
    private double saldoAFavorCliente = 0.0;
    private double costoNuevosProductos = 0.0;

    @FXML
    public void initialize() {
        tablaDevolucion.setPlaceholder(new Label("No hay artículos para devolver."));
        tablaNuevos.setPlaceholder(new Label("Cargando inventario..."));
        configurarColumnaCantidadDevolucion();
        colDevDesc.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getArticulo().getNombre()));
        colDevPrecio.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty("$ " + cell.getValue().getPrecioUnitario()));

        configurarColumnaCantidadNuevos();
        colNueDesc.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getArticulo().getNombre()));
        colNuePrecio.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty("$ " + cell.getValue().getPrecioUnitario()));

        cargarCatalogoCompleto();

        // 🎧 LA MAGIA DE LA LUPITA: Este es el escuchador en vivo
        // 🎧 ESCUCHADOR EN VIVO PARA LA LUPITA DE NUEVOS ARTÍCULOS EN CAMBIOS
        if (txtBuscarNuevo != null) {
            txtBuscarNuevo.textProperty().addListener((observable, oldValue, newValue) -> {
                try {
                    if (newValue == null || newValue.trim().isEmpty()) {
                        cargarCatalogoCompleto();
                    } else {
                        // Llama a la API buscando en todo el backend por nombre o código
                        java.util.List<com.nakel.frontend.model.Articulo> resultados = articuloApi.buscarParaVenta(newValue.trim(), 0, 100);
                        if (resultados != null) {
                            masterInventario.clear();
                            for (com.nakel.frontend.model.Articulo art : resultados) {
                                if (art.getStockActual() > 0) {
                                    masterInventario.add(new DetalleVenta(0, art.getPrecio(), 0.0, art));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error en la búsqueda de cambios: " + e.getMessage());
                }
            });
        }
    }

    public void cargarVentaOriginal(Venta venta) {
        this.ventaOriginal = venta;
        lblTituloVenta.setText("Gestión de Cambio - Venta #" + String.format("%08d", venta.getId()));
        lblCliente.setText("Cliente: " + (venta.getCliente() != null ? venta.getCliente().getNombre() : "Consumidor Final"));

        // 🔥 OBTENEMOS EL "TICKET VIVO" DESDE EL BACKEND (Restando lo devuelto y sumando lo nuevo histórico)
        try {
            java.util.List<com.nakel.frontend.model.ArticuloInfoDTO> stockReal = cambioApi.obtenerArticulosActuales(venta.getId());

            if (stockReal != null && !stockReal.isEmpty()) {
                java.util.List<DetalleVenta> itemsParaDevolver = new java.util.ArrayList<>();

                for (com.nakel.frontend.model.ArticuloInfoDTO info : stockReal) {
                    // Transformamos el DTO del ticket vivo en un DetalleVenta para que la tabla lo dibuje
                    DetalleVenta det = new DetalleVenta();
                    det.setArticulo(info.getArticulo());
                    det.setCantidad(info.getCantidad());
                    det.setCantidadOriginal(info.getCantidad()); // Límite máximo que puede devolver ahora
                    det.setPrecioUnitario(info.getPrecioUnitario());
                    det.setSubtotal(info.getCantidad() * info.getPrecioUnitario());

                    itemsParaDevolver.add(det);
                }

                tablaDevolucion.setItems(javafx.collections.FXCollections.observableArrayList(itemsParaDevolver));
            } else {
                tablaDevolucion.setItems(javafx.collections.FXCollections.observableArrayList());
            }

        } catch (Exception e) {
            System.err.println("❌ Error al cargar stock real del cliente: " + e.getMessage());
            // Fallback por seguridad a los detalles originales si falla la red
            if (venta.getDetalles() != null) {
                for (DetalleVenta det : venta.getDetalles()) {
                    det.setCantidadOriginal(det.getCantidad());
                }
                tablaDevolucion.setItems(javafx.collections.FXCollections.observableArrayList(venta.getDetalles()));
            }
        }

        // Calculamos el saldo a favor inicial basado en lo que realmente tiene en la mano
        recalcularSaldoFavor();
    }

    @FXML
    public void buscarYAgregarNuevoProducto(ActionEvent event) {
        String busqueda = txtBuscarNuevo.getText();
        if (busqueda == null || busqueda.isBlank()) return;

        try {
            String jsonRespuesta = articuloApi.buscarProducto(busqueda.trim());
            if (jsonRespuesta != null && !jsonRespuesta.isBlank()) {
                com.nakel.frontend.model.Articulo articuloEncontrado = gson.fromJson(jsonRespuesta, com.nakel.frontend.model.Articulo.class);
                DetalleVenta nuevoItem = new DetalleVenta();
                nuevoItem.setArticulo(articuloEncontrado);
                nuevoItem.setCantidad(1);
                nuevoItem.setPrecioUnitario(articuloEncontrado.getPrecio());
                nuevoItem.setSubtotal(articuloEncontrado.getPrecio() * 1);

                tablaNuevos.getItems().add(nuevoItem);
                costoNuevosProductos += nuevoItem.getSubtotal();
                lblCostoNuevos.setText(String.format("$ %.2f", costoNuevosProductos));
                calcularDiferencias();
                txtBuscarNuevo.clear();
            } else {
                mostrarError("Producto no encontrado", "No se encontró ningún artículo: " + busqueda);
                txtBuscarNuevo.selectAll();
            }
        } catch (Exception e) {
            mostrarError("Error de Conexión", "Problema al buscar producto: " + e.getMessage());
        }
    }

    @FXML
    public void confirmarCambio(ActionEvent event) {
        // Validación: No podés confirmar un cambio vacío
        if (costoNuevosProductos == 0 && saldoAFavorCliente == ventaOriginal.getTotal()) {
            mostrarError("Cambio Inválido", "Debe seleccionar al menos un artículo para cambiar o devolver.");
            return;
        }

        double diferencia = costoNuevosProductos - saldoAFavorCliente;
        String resumen = generarResumenArticulos();

        // Obtenemos los datos del cliente real (o Consumidor Final)
        String nombreCliente = (ventaOriginal.getCliente() != null) ? ventaOriginal.getCliente().getNombre() : "Consumidor Final";
        Long idCliente = (ventaOriginal.getCliente() != null) ? ventaOriginal.getCliente().getId() : null;

        if (diferencia > 0) {
            // 🔥 AHORA ESPERAMOS A QUE EL MODAL NOS DIGA SI PAGÓ O NO
            boolean pagoExitoso = abrirModalDePago(diferencia, ventaOriginal);

            if (pagoExitoso) {
                actualizarStockEnBaseDeDatos();
                registrarCambioEnBackend(resumen, diferencia, null); // 📝 Guardamos el rastro histórico

                // 🟢 ÉXITO SIN VALE (El cliente pagó la diferencia)
                abrirModalExitoCambio(nombreCliente, 0.0, null);
                cerrarModal(event);
            } else {
                System.out.println("El usuario canceló el pago. No se guardan los cambios.");
            }

        } else if (diferencia < 0) {
            double montoVoucher = Math.abs(diferencia);
            com.nakel.frontend.model.Vale nuevoVale = valeApi.generarVale(montoVoucher, idCliente);

            if (nuevoVale != null) {
                actualizarStockEnBaseDeDatos();
                registrarCambioEnBackend(resumen, diferencia, nuevoVale.getCodigo()); // 📝 Guardamos el rastro histórico

                // 🟡 ÉXITO CON VALE O SALDO A FAVOR (Sobra plata)
                abrirModalExitoCambio(nombreCliente, montoVoucher, nuevoVale.getCodigo());
                cerrarModal(event);
            } else {
                mostrarError("Error de Conexión", "No se pudo generar el vale en el servidor.");
            }

        } else {
            // Cambio mano a mano
            actualizarStockEnBaseDeDatos();
            registrarCambioEnBackend(resumen, 0.0, null); // 📝 Guardamos el rastro histórico

            // 🟢 ÉXITO MANO A MANO (Saldo 0)
            abrirModalExitoCambio(nombreCliente, 0.0, null);
            cerrarModal(event);
        }
    }

    // 🔥 EL DISPARADOR DEL MODAL PREMIUM
    private void abrirModalExitoCambio(String nombreCliente, double saldoAFavor, String codigoVale) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/modal-exito-cambio.fxml"));
            javafx.scene.Parent root = loader.load();

            // Le pasamos los datos al nuevo controlador
            com.nakel.frontend.controller.ModalExitoCambioController controller = loader.getController();
            controller.inicializarDatos(nombreCliente, saldoAFavor, codigoVale);

            javafx.stage.Stage modalStage = new javafx.stage.Stage();
            modalStage.setTitle("¡Cambio Procesado!");
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.setResizable(false);

            // 🛑 FRENO DE MANO: El código frena acá hasta que la dueña toque "OK (Cerrar)"
            modalStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error al abrir el modal de éxito premium.");
        }
    }

    private void calcularDiferencias() {
        double diferencia = costoNuevosProductos - saldoAFavorCliente;
        if (diferencia > 0) {
            lblDiferenciaTexto.setText("EL CLIENTE DEBE ABONAR:");
            lblDiferenciaTexto.setStyle("-fx-text-fill: #d32f2f;");
            lblDiferenciaMonto.setText(String.format("$ %.2f", diferencia));
            lblMensajeVoucher.setVisible(false);
        } else if (diferencia < 0) {
            lblDiferenciaTexto.setText("SALDO A FAVOR DEL CLIENTE:");
            lblDiferenciaTexto.setStyle("-fx-text-fill: #388e3c;");
            lblDiferenciaMonto.setText(String.format("$ %.2f", Math.abs(diferencia)));
            lblMensajeVoucher.setVisible(true);
        } else {
            lblDiferenciaTexto.setText("CAMBIO DIRECTO (Sin costo):");
            lblDiferenciaTexto.setStyle("-fx-text-fill: #333333;");
            lblDiferenciaMonto.setText("$ 0.00");
            lblMensajeVoucher.setVisible(false);
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private String generarResumenArticulos() {
        StringBuilder sb = new StringBuilder();
        sb.append("Devuelve: ");
        for (DetalleVenta dev : tablaDevolucion.getItems()) {
            if (dev.getCantidad() > 0) {
                sb.append(dev.getArticulo().getNombre()).append(" (x").append(dev.getCantidad()).append("), ");
            }
        }
        sb.append(" | Se lleva: ");
        for (DetalleVenta nuev : masterInventario) {
            if (nuev.getCantidad() > 0) {
                sb.append(nuev.getArticulo().getNombre()).append(" (x").append(nuev.getCantidad()).append("), ");
            }
        }
        return sb.toString();
    }

    // 🔥 AHORA DEVUELVE BOOLEAN PARA SABER SI EL USUARIO PAGÓ DE VERDAD
    // 🔥 AHORA DEVUELVE BOOLEAN PARA SABER SI EL USUARIO PAGÓ DE VERDAD
// 🔥 AHORA DEVUELVE BOOLEAN PARA SABER SI EL USUARIO PAGÓ DE VERDAD
    private boolean abrirModalDePago(double montoAcobrar, Venta venta) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/pago-mixto-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            Long idCliente = (venta.getCliente() != null) ? venta.getCliente().getId() : null;

            PagoMixtoController controller = loader.getController();
            controller.inicializarValores(montoAcobrar, null, idCliente);

            Stage stage = new Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("Cobrar Diferencia de Cambio");
            stage.setScene(new javafx.scene.Scene(root));

            // 🛑 ACÁ SE CONGELA HASTA QUE EL MODAL SE CIERRE
            stage.showAndWait();

            // 🔥 LA MAGIA CON TUS MÉTODOS REALES: Si pagó, vemos si dejó un vale aplicado
            if (controller.isPagoCompleto()) {
                com.nakel.frontend.model.Vale valeUsado = controller.getValeAplicado();

                // Si el cliente usó un vale, lo mandamos a quemar a la base de datos
                if (valeUsado != null && valeUsado.getCodigo() != null && !valeUsado.getCodigo().isBlank()) {
                    valeApi.consumirVale(valeUsado.getCodigo());
                    System.out.println("🔥 Vale " + valeUsado.getCodigo() + " consumido exitosamente en el cambio.");
                }
            }

            // Retorna TRUE si pagó todo, FALSE si cerró de la X
            return controller.isPagoCompleto();

        } catch (Exception e) {
            System.err.println("Error al abrir el modal de pagos: " + e.getMessage());
            mostrarError("Error visual", "No se pudo abrir la ventana de pagos.");
            return false;
        }
    }

    // 📝 MAGIA HISTÓRICA: Le avisamos a la BD que esto pasó
// 📝 MAGIA HISTÓRICA: Empaquetamos y avisamos a la BD
    // 📝 MAGIA HISTÓRICA: Empaquetamos y avisamos a la BD
    private void registrarCambioEnBackend(String resumen, double diferencia, String codigoVale) {
        try {
            // 1. Creamos el objeto Cambio usando los SETTERS EXACTOS de tu clase
            com.nakel.frontend.model.Cambio nuevoCambio = new com.nakel.frontend.model.Cambio();

            nuevoCambio.setResumenArticulos(resumen);
            nuevoCambio.setDiferenciaCobrada(diferencia);
            nuevoCambio.setCodigoValeGenerado(codigoVale);

            // 🔥 2. ARMAMOS LA LISTA DE ÍTEMS PARA EL BACKEND
            java.util.List<com.nakel.frontend.model.ItemCambio> itemsDelCambio = new java.util.ArrayList<>();

            // A) Agarramos lo que el cliente DEVUELVE
            for (com.nakel.frontend.model.DetalleVenta dev : tablaDevolucion.getItems()) {
                if (dev.getCantidad() > 0) {
                    com.nakel.frontend.model.ItemCambio itemDevuelto = new com.nakel.frontend.model.ItemCambio();
                    itemDevuelto.setArticulo(dev.getArticulo());
                    itemDevuelto.setCantidad(dev.getCantidad());
                    itemDevuelto.setPrecioUnitario(dev.getPrecioUnitario());
                    itemDevuelto.setTipo("DEVUELTO"); // <-- Clave para restar en el backend
                    itemsDelCambio.add(itemDevuelto);
                }
            }
//
            // B) Agarramos lo que se lleva NUEVO
            for (com.nakel.frontend.model.DetalleVenta nuev : masterInventario) {
                if (nuev.getCantidad() > 0) {
                    com.nakel.frontend.model.ItemCambio itemNuevo = new com.nakel.frontend.model.ItemCambio();
                    itemNuevo.setArticulo(nuev.getArticulo());
                    itemNuevo.setCantidad(nuev.getCantidad());
                    itemNuevo.setPrecioUnitario(nuev.getPrecioUnitario());
                    itemNuevo.setTipo("NUEVO"); // <-- Clave para sumar en el backend
                    itemsDelCambio.add(itemNuevo);
                }
            }

            // 3. Le atamos la lista de artículos al cambio
            nuevoCambio.setItems(itemsDelCambio);

            // 4. Mandamos el paquete completo al servicio
            cambioApi.registrarCambio(ventaOriginal.getId(), nuevoCambio);

        } catch (Exception e) {
            System.out.println("No se pudo registrar la trazabilidad: " + e.getMessage());
        }
    }

    @FXML
    public void cerrarModal(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    private void actualizarStockEnBaseDeDatos() {
        for (DetalleVenta devuelto : tablaDevolucion.getItems()) {
            if (devuelto.getCantidad() > 0) {
                articuloApi.restaurarStock(devuelto.getArticulo().getId(), devuelto.getCantidad());
            }
        }
        for (DetalleVenta nuevo : masterInventario) {
            if (nuevo.getCantidad() > 0) {
                articuloApi.descontarStock(nuevo.getArticulo().getId(), nuevo.getCantidad());
            }
        }
    }

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

                btnMenos.setOnAction(e -> {
                    DetalleVenta item = getTableView().getItems().get(getIndex());
                    if (item.getCantidad() > 1) {
                        item.setCantidad(item.getCantidad() - 1);
                        item.setSubtotal(item.getCantidad() * item.getPrecioUnitario());
                    } else {
                        getTableView().getItems().remove(item);
                    }
                    getTableView().refresh();
                    recalcularSaldoFavor();
                });

                btnMas.setOnAction(e -> {
                    DetalleVenta item = getTableView().getItems().get(getIndex());
                    if (item.getCantidad() < item.getCantidadOriginal()) {
                        item.setCantidad(item.getCantidad() + 1);
                        item.setSubtotal(item.getCantidad() * item.getPrecioUnitario());
                        getTableView().refresh();
                        recalcularSaldoFavor();
                    } else {
                        mostrarError("Límite", "No puede devolver más de lo que compró.");
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView().getItems().get(getIndex()) == null) {
                    setGraphic(null);
                } else {
                    lblCant.setText(String.valueOf(getTableView().getItems().get(getIndex()).getCantidad()));
                    setGraphic(panel);
                }
            }
        });
    }

    private void recalcularSaldoFavor() {
        saldoAFavorCliente = 0.0;
        for (DetalleVenta det : tablaDevolucion.getItems()) {
            saldoAFavorCliente += det.getSubtotal();
        }
        lblSaldoFavor.setText(String.format("$ %.2f", saldoAFavorCliente));
        calcularDiferencias();
    }

    private void cargarCatalogoCompleto() {
        try {
            // 🔥 LA REGLA DE ORO: Vamos al Backend a pedir solo los primeros 100 con stock > 0
            java.util.List<com.nakel.frontend.model.Articulo> articulosBBDD = articuloApi.buscarParaVenta("", 0, 100);

            if (articulosBBDD != null) {
                masterInventario.clear();
                for (com.nakel.frontend.model.Articulo art : articulosBBDD) {
                    masterInventario.add(new DetalleVenta(0, art.getPrecio(), 0.0, art));
                }
                filteredInventario = new javafx.collections.transformation.FilteredList<>(masterInventario, p -> true);
                tablaNuevos.setItems(filteredInventario);
            }
        } catch (Exception e) {
            System.err.println("❌ Error al cargar catálogo en cambios: " + e.getMessage());
        }
    }

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

                btnMenos.setOnAction(e -> {
                    DetalleVenta item = getTableView().getItems().get(getIndex());
                    if (item.getCantidad() > 0) {
                        item.setCantidad(item.getCantidad() - 1);
                        item.setSubtotal(item.getCantidad() * item.getPrecioUnitario());
                        getTableView().refresh();
                        recalcularCostoNuevos();
                    }
                });

                btnMas.setOnAction(e -> {
                    DetalleVenta item = getTableView().getItems().get(getIndex());
                    if (item.getCantidad() < item.getArticulo().getStockActual()) {
                        item.setCantidad(item.getCantidad() + 1);
                        item.setSubtotal(item.getCantidad() * item.getPrecioUnitario());
                        getTableView().refresh();
                        recalcularCostoNuevos();
                    } else {
                        mostrarError("Sin stock", "No hay más unidades.");
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView().getItems().get(getIndex()) == null) {
                    setGraphic(null);
                } else {
                    lblCant.setText(String.valueOf(getTableView().getItems().get(getIndex()).getCantidad()));
                    setGraphic(panel);
                }
            }
        });
    }

    private void recalcularCostoNuevos() {
        costoNuevosProductos = 0.0;
        for (DetalleVenta det : masterInventario) {
            costoNuevosProductos += det.getSubtotal();
        }
        lblCostoNuevos.setText(String.format("$ %.2f", costoNuevosProductos));
        calcularDiferencias();
    }

    // 🔍 NUEVO MÉTODO: El cerebro de la búsqueda global para Cambios
    private void procesarBusquedaInventario(String busqueda) {
        try {
            java.util.List<com.nakel.frontend.model.Articulo> resultados = articuloApi.buscarParaVenta(busqueda, 0, 100);

            if (resultados != null) {
                masterInventario.clear();
                for (com.nakel.frontend.model.Articulo art : resultados) {
                    masterInventario.add(new DetalleVenta(0, art.getPrecio(), 0.0, art));
                }
                filteredInventario = new javafx.collections.transformation.FilteredList<>(masterInventario, p -> true);
                tablaNuevos.setItems(filteredInventario);
            }
        } catch (Exception e) {
            System.err.println("❌ Error al buscar productos para el cambio: " + e.getMessage());
        }
    }
}