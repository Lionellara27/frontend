package com.nakel.frontend.controller;

import com.nakel.frontend.model.Articulo;
import com.nakel.frontend.model.DetalleCalculadora;
import com.nakel.frontend.model.Insumo;
import com.nakel.frontend.service.ArticuloApiService;
import com.nakel.frontend.service.InsumoApiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;

public class CalculadoraController {

    private BigDecimal precioVenta = BigDecimal.ZERO;
    private ArticuloApiService articuloService;

    @FXML private TextField txtNombreProducto;
    @FXML private ComboBox<Insumo> cmbInsumo;
    @FXML private TextField txtAncho;
    @FXML private TextField txtLargo;
    @FXML private TextField txtCantidad;

    @FXML private TableView<DetalleCalculadora> tablaReceta;
    @FXML private TableColumn<DetalleCalculadora, String> colInsumo;
    @FXML private TableColumn<DetalleCalculadora, String> colUso;
    @FXML private TableColumn<DetalleCalculadora, String> colSubtotal;

    @FXML private Label lblTotalCosto;
    @FXML private TextField txtMargen;
    @FXML private Label lblPrecioFinal;

    private ObservableList<DetalleCalculadora> listaReceta = FXCollections.observableArrayList();
    private double costoTotalReceta = 0.0;
    private final InsumoApiService insumoApi = new InsumoApiService();

    private final ObservableList<Insumo> insumosMaster = FXCollections.observableArrayList();
    private javafx.collections.transformation.FilteredList<Insumo> insumosFiltrados;
    private int paginaActualInsumos = 0;

    private boolean scrollListenerConfigurado = false;
    private int secuenciaBusquedaInsumo = 0;
    private boolean actualizandoInsumosProgramaticamente = false;

    private static final int TAMANIO_PAGINA_INSUMOS = 50;

    @FXML
    public void initialize() {
        this.articuloService = new ArticuloApiService();

        // 1. Configurar la tabla
        colInsumo.setCellValueFactory(new PropertyValueFactory<>("nombreInsumo"));
        colUso.setCellValueFactory(new PropertyValueFactory<>("descripcionUso"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotalFormateado"));
        tablaReceta.setItems(listaReceta);
        tablaReceta.setPlaceholder(new Label("Agregue telas, avíos o mano de obra a la receta."));

        // 2. Cargar insumos
        cargarInsumosDesdeBD();
        configurarBusquedaInsumo();
        //configurarScrollInfinito();

        // 3. Escuchar la selección del ComboBox
        cmbInsumo.getSelectionModel().selectedItemProperty().addListener((obs, viejo, nuevo) -> prepararCampos(nuevo));

        // 4. Escuchar el Margen de Ganancia
        txtMargen.textProperty().addListener((obs, viejo, nuevo) -> calcularPrecioSugerido());
    }

    private void cargarInsumosDesdeBD() {
        try {
            paginaActualInsumos = 0;
            actualizandoInsumosProgramaticamente = true; // 🔒 cerramos el candado

            insumosMaster.clear();
            insumosMaster.addAll(insumoApi.obtenerInsumosPaginadosComoLista(paginaActualInsumos, TAMANIO_PAGINA_INSUMOS));

            insumosFiltrados = new javafx.collections.transformation.FilteredList<>(insumosMaster, p -> true);
            cmbInsumo.setItems(insumosFiltrados);

            configurarConverterInsumo();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("No se pudieron cargar los insumos.");
        } finally {
            actualizandoInsumosProgramaticamente = false; // 🔓 abrimos el candado de nuevo
        }
    }

    // 🔥 Para que el ComboBox editable muestre "Telar Negro" en vez del objeto Java entero
    private void configurarConverterInsumo() {
        cmbInsumo.setConverter(new javafx.util.StringConverter<Insumo>() {
            @Override
            public String toString(Insumo insumo) {
                return insumo != null ? insumo.getNombre() : "";
            }

            @Override
            public Insumo fromString(String texto) {
                return cmbInsumo.getSelectionModel().getSelectedItem();
            }
        });
    }

    // 🔥 Botón "Cargar más": trae los siguientes 20 y los suma a los ya cargados
    // 🔥 Trae la siguiente tanda de insumos (llamado por el scroll, no por un botón)
    private void cargarMasInsumos() {
        try {
            paginaActualInsumos++;
            java.util.List<Insumo> siguientePagina = insumoApi.obtenerInsumosPaginadosComoLista(paginaActualInsumos, TAMANIO_PAGINA_INSUMOS);

            if (siguientePagina == null || siguientePagina.isEmpty()) {
                paginaActualInsumos--; // no había más, nos quedamos donde estábamos
                return;
            }

            insumosMaster.addAll(siguientePagina);
            System.out.println("✅ Scroll llegó al final: cargados " + siguientePagina.size() + " insumos más (página " + paginaActualInsumos + ")");

        } catch (Exception e) {
            e.printStackTrace();
            paginaActualInsumos--;
        }
    }

    // 🔥 Búsqueda: filtra en RAM al escribir, y busca en TODO el backend al presionar Enter
    private javafx.animation.PauseTransition debounceBusqueda;

    private void configurarBusquedaInsumo() {
        debounceBusqueda = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));

        cmbInsumo.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            // 🔒 Si el cambio lo hizo el sistema (al seleccionar un ítem), ignoramos para no reventar la RAM
            if (actualizandoInsumosProgramaticamente) return;

            if (newVal == null || newVal.trim().isEmpty()) {
                cmbInsumo.hide();
                return;
            }

            debounceBusqueda.stop();
            debounceBusqueda.setOnFinished(e -> buscarEnBackend(newVal.trim()));
            debounceBusqueda.playFromStart();
        });
    }

    private void buscarEnBackend(String texto) {
        final int miSecuencia = ++secuenciaBusquedaInsumo;

        // 📡 Viajamos al Backend: Buscamos en TODOS los insumos, trayendo los mejores 50
        java.util.List<Insumo> resultados = insumoApi.buscarInsumosComoLista(texto);

        if (miSecuencia != secuenciaBusquedaInsumo) return;

        actualizandoInsumosProgramaticamente = true; // 🔒 CERRAMOS EL CANDADO
        try {
            insumosMaster.setAll(resultados != null ? resultados : java.util.Collections.emptyList());
            insumosFiltrados.setPredicate(p -> true);

            if (resultados != null && !resultados.isEmpty()) {
                cmbInsumo.show(); // Despliega la lista si hay resultados
            } else {
                cmbInsumo.hide();
            }

            // Mantenemos el texto que tipeó la clienta y el cursor al final
            cmbInsumo.getEditor().setText(texto);
            cmbInsumo.getEditor().positionCaret(texto.length());

        } finally {
            actualizandoInsumosProgramaticamente = false; // 🔓 ABRIMOS EL CANDADO
        }
    }

    // 🔥 Búsqueda real contra el backend, entre TODOS los insumos (no solo los cargados)

    private void prepararCampos(Insumo insumo) {
        if (insumo == null || insumo.getCategoria() == null) return;

        txtAncho.clear(); txtLargo.clear(); txtCantidad.clear();

        if ("SUPERFICIE".equals(insumo.getCategoria().getTipoMedicion())) {
            txtAncho.setDisable(false);
            txtLargo.setDisable(false);
            txtCantidad.setDisable(true);
        } else {
            txtAncho.setDisable(true);
            txtLargo.setDisable(true);
            txtCantidad.setDisable(false);
        }
    }

    // 🔥 ACÁ ESTÁ EL MÉTODO QUE FALTABA CON EL PATOVICA DE STOCK
    @FXML
    public void agregarInsumoAReceta(ActionEvent event) {
        Insumo insumo = cmbInsumo.getValue();
        if (insumo == null) {
            mostrarError("Seleccione un insumo primero.");
            return;
        }

        try {
            double subtotal = 0.0;
            DetalleCalculadora detalle = null;

            if (insumo.getCategoria() == null || insumo.getCategoria().getTipoMedicion() == null) {
                mostrarError("El insumo tiene la categoría rota.");
                return;
            }

            String tipo = insumo.getCategoria().getTipoMedicion().trim().toUpperCase();

            if ("SUPERFICIE".equals(tipo)) {
                int ancho = Integer.parseInt(txtAncho.getText());
                int largo = Integer.parseInt(txtLargo.getText());
                int cm2Usados = ancho * largo;

                // 🛑 EL PATOVICA (Superficie)
                double areaActual = insumo.getAreaActualCm2() != null ? insumo.getAreaActualCm2() : 0;
                if (cm2Usados > areaActual) {
                    mostrarError("❌ Stock Insuficiente. Solo te quedan: " + areaActual + " cm² de " + insumo.getNombre());
                    return;
                }

                subtotal = insumo.getCostoCalculado().doubleValue() * cm2Usados;
                detalle = new DetalleCalculadora(insumo, cm2Usados, "Superficie", subtotal, ancho, largo);

            } else if ("UNIDAD".equals(tipo)) {
                double cantidad = Double.parseDouble(txtCantidad.getText());

                // 🛑 EL PATOVICA (Unidad)
                int cantidadActual = insumo.getCantidadActual() != null ? insumo.getCantidadActual() : 0;
                if (cantidad > cantidadActual) {
                    mostrarError("❌ Stock Insuficiente. Solo te quedan: " + cantidadActual + " unidades de " + insumo.getNombre());
                    return;
                }

                subtotal = insumo.getCostoCalculado().doubleValue() * cantidad;
                detalle = new DetalleCalculadora(insumo, cantidad, "Unidad", subtotal, null, null);

            } else if ("SERVICIO".equals(tipo) || "TIEMPO".equals(tipo)) {
                String horasTexto = txtCantidad.getText().replace(",", ".");
                double horas = Double.parseDouble(horasTexto);

                subtotal = insumo.getCostoCalculado().doubleValue() * horas;
                detalle = new DetalleCalculadora(insumo, horas, "Horas", subtotal, null, null);
            } else {
                mostrarError("Error: Tipo de medición desconocido (" + tipo + ")");
                return;
            }

            if (detalle != null) {
                listaReceta.add(detalle);
                recalcularCostoTotal();
                txtAncho.clear(); txtLargo.clear(); txtCantidad.clear();
            }

        } catch (NumberFormatException e) {
            mostrarError("Por favor, ingrese solamente números válidos.");
        }
    }

    @FXML
    public void eliminarFila(ActionEvent event) {
        DetalleCalculadora seleccionado = tablaReceta.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            listaReceta.remove(seleccionado);
            recalcularCostoTotal();
        }
    }

    private void recalcularCostoTotal() {
        costoTotalReceta = 0.0;
        for (DetalleCalculadora det : listaReceta) {
            costoTotalReceta += det.getSubtotal();
        }
        lblTotalCosto.setText(String.format("$ %.2f", costoTotalReceta));
        calcularPrecioSugerido();
    }

    private void calcularPrecioSugerido() {
        try {
            if (txtMargen.getText() != null && !txtMargen.getText().isBlank()) {
                double margen = Double.parseDouble(txtMargen.getText());
                double gananciaPlata = costoTotalReceta * (margen / 100);
                double precioSugerido = costoTotalReceta + gananciaPlata;
                this.precioVenta = BigDecimal.valueOf(precioSugerido);
                lblPrecioFinal.setText(String.format("$ %.2f", precioSugerido));
            } else {
                this.precioVenta = BigDecimal.valueOf(costoTotalReceta);
                lblPrecioFinal.setText(String.format("$ %.2f", costoTotalReceta));
            }
        } catch (NumberFormatException e) {
            this.precioVenta = BigDecimal.ZERO;
            lblPrecioFinal.setText("$ ---");
        }
    }

    @FXML
    public void guardarPresupuesto(ActionEvent event) {

        if (txtNombreProducto.getText() == null || txtNombreProducto.getText().isBlank()) {
            mostrarError("Debe ingresar el nombre del producto.");
            return;
        }

        if (tablaReceta.getItems().isEmpty()) {
            mostrarError("Debe agregar al menos un insumo.");
            return;
        }

        // =========================================================================
        // 🔥 NUEVA LÓGICA: Atajamos el Margen/Ganancia antes de que rompa nada
        // =========================================================================
        String textoMargen = txtMargen.getText();

        if (textoMargen == null || textoMargen.trim().isEmpty()) {

            javafx.scene.control.Alert confirmacion = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            confirmacion.setTitle("Atención - Margen Vacío");
            confirmacion.setHeaderText("No ingresaste ningún porcentaje de margen.");
            confirmacion.setContentText("¿Estás seguro de que querés guardar este producto con 0% de ganancia (al costo)?");

            // Si elige "Cancelar" o cierra con la X, abortamos el guardado
            if (confirmacion.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) != javafx.scene.control.ButtonType.OK) {
                return;
            }

            // Si eligió "Aceptar", clavamos el 0 en la cajita
            txtMargen.setText("0");

            // ⚠️ ATENCIÓN ACÁ: Como forzamos el 0%, tenés que asegurarte de que la variable
            // 'precioVenta' se actualice para que sea igual al 'costoTotalReceta'.
            // Si tenés un método que calcula el precio al tocar una tecla, llamalo acá.
            // Ejemplo: calcularPrecioFinal();
            // Si no, podés forzarlo directo si tu lógica lo permite:
            // precioVenta = costoTotalReceta;

        } else {
            // 🔥 Atajamos si puso comas en vez de puntos o si puso letras
            try {
                textoMargen = textoMargen.replace(",", ".");
                txtMargen.setText(textoMargen); // Actualizamos la cajita visualmente
                Double.parseDouble(textoMargen); // Solo para probar que no explote
            } catch (NumberFormatException e) {
                mostrarError("El margen debe ser un número válido (Ej: 50 o 50.5).");
                return;
            }
        }
        // =========================================================================

        // Validamos que el precio de venta sea válido (acordate de lo que te puse en el comentario ⚠️ de arriba)
        if (precioVenta == null || precioVenta.compareTo(BigDecimal.ZERO) <= 0) {
            mostrarError("El precio de venta es inválido. Verifique el margen o los costos.");
            return;
        }

        Insumo materialDestacado = determinarMaterialDestacado();

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/nakel/frontend/view/alta-producto-produccion-modal.fxml"));
            javafx.scene.Parent root = loader.load();

            AltaProductoProduccionController controller = loader.getController();
            controller.inicializarDatos(
                    txtNombreProducto.getText(),
                    costoTotalReceta,
                    precioVenta.doubleValue(),
                    materialDestacado,
                    listaReceta
            );

            javafx.stage.Stage modalStage = new javafx.stage.Stage();
            modalStage.setTitle("Finalizar Alta de Producto y Fabricar");
            modalStage.setScene(new javafx.scene.Scene(root));
            modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            modalStage.setResizable(false);
            modalStage.showAndWait();

            if (controller.isGuardadoExitoso()) {
                txtNombreProducto.clear();
                txtMargen.clear();
                listaReceta.clear();
                recalcularCostoTotal();
                mostrarExito("¡Producto creado y stock descontado!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al abrir la ventana de finalización de producto.");
        }
    }

    // 🧠 ALGORITMO: Busca el material de superficie más utilizado
    private Insumo determinarMaterialDestacado() {
        Insumo ganador = null;
        double maxSuperficie = 0.0;

        for (DetalleCalculadora det : listaReceta) {
            Insumo ins = det.getInsumo();
            if (ins != null && ins.getCategoria() != null) {
                String tipo = ins.getCategoria().getTipoMedicion().trim().toUpperCase();

                if ("SUPERFICIE".equals(tipo)) {
                    double cantidadUsada = det.getCantidadUsada();
                    if (cantidadUsada > maxSuperficie) {
                        maxSuperficie = cantidadUsada;
                        ganador = ins;
                    }
                }
            }
        }
        return ganador;
    }

    private void mostrarExito(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}