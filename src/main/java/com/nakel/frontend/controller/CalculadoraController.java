package com.nakel.frontend.controller;

import com.nakel.frontend.model.Articulo;
import com.nakel.frontend.model.DetalleCalculadora;
import com.nakel.frontend.model.Insumo;
import com.nakel.frontend.model.Categoria;
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

    @FXML
    private TextField txtNombreProducto;


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
//
    private final InsumoApiService insumoApi = new InsumoApiService();

    @FXML
    public void initialize() {
        this.articuloService = new ArticuloApiService();

        // 1. Configurar la tabla
        colInsumo.setCellValueFactory(new PropertyValueFactory<>("nombreInsumo"));
        colUso.setCellValueFactory(new PropertyValueFactory<>("descripcionUso"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotalFormateado"));
        tablaReceta.setItems(listaReceta);
        tablaReceta.setPlaceholder(new Label("Agregue telas, avíos o mano de obra a la receta."));

        // 2. Simular carga de insumos (A futuro, acá llamás a InsumoApiService)
        //simularCargaInsumos();
        cargarInsumosDesdeBD();


        // 3. Escuchar la selección del ComboBox para bloquear/desbloquear cajitas
        cmbInsumo.getSelectionModel().selectedItemProperty().addListener((obs, viejo, nuevo) -> prepararCampos(nuevo));

        // 4. Escuchar cuando escriben el Margen de Ganancia para recalcular el precio final
        txtMargen.textProperty().addListener((obs, viejo, nuevo) -> calcularPrecioSugerido());
    }

    private void cargarInsumosDesdeBD() {

        try {

            cmbInsumo.getItems().clear();

            cmbInsumo.getItems().addAll(
                    insumoApi.obtenerListaInsumos()
            );

        } catch (Exception e) {

            e.printStackTrace();
            mostrarError("No se pudieron cargar los insumos.");

        }
    }

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

            // 1. Verificamos que tenga categoría para que no explote
            if (insumo.getCategoria() == null || insumo.getCategoria().getTipoMedicion() == null) {
                mostrarError("El insumo tiene la categoría rota.");
                return;
            }

            // 2. Normalizamos la palabra (todo mayúscula y sin espacios extra) para evitar errores
            String tipo = insumo.getCategoria().getTipoMedicion().trim().toUpperCase();

            if ("SUPERFICIE".equals(tipo)) {
                int ancho = Integer.parseInt(txtAncho.getText());
                int largo = Integer.parseInt(txtLargo.getText());
                int cm2Usados = ancho * largo;

                subtotal = insumo.getCostoCalculado().doubleValue() * cm2Usados;
                detalle = new DetalleCalculadora(insumo, cm2Usados, "Superficie", subtotal, ancho, largo);

            } else if ("UNIDAD".equals(tipo)) {
                double cantidad = Double.parseDouble(txtCantidad.getText());

                subtotal = insumo.getCostoCalculado().doubleValue() * cantidad;
                detalle = new DetalleCalculadora(insumo, cantidad, "Unidad", subtotal, null, null);

                // Agregamos TIEMPO por las dudas de cómo lo hayas escrito en tu base de datos
            } else if ("SERVICIO".equals(tipo) || "TIEMPO".equals(tipo)) {
                // Reemplazamos la coma por punto por si escriben "1,5" horas en vez de "1.5"
                String horasTexto = txtCantidad.getText().replace(",", ".");
                double horas = Double.parseDouble(horasTexto);

                subtotal = insumo.getCostoCalculado().doubleValue() * horas;
                detalle = new DetalleCalculadora(insumo, horas, "Horas", subtotal, null, null);

            } else {
                // Si el tipo es una palabra que no conocemos, avisamos y FRENAMOS todo
                mostrarError("Error: Tipo de medición desconocido (" + tipo + ")");
                return;
            }

            // 3. LA CLAVE: Solo agregamos a la tabla si el detalle se creó bien (sin fantasmas)
            if (detalle != null) {
                listaReceta.add(detalle);
                recalcularCostoTotal();

                // Limpiamos la pantalla para el próximo insumo
                txtAncho.clear();
                txtLargo.clear();
                txtCantidad.clear();
            }

        } catch (NumberFormatException e) {
            // Si en cantidad escriben "jadkjsa" o dejan vacío, caemos acá suavemente
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
        calcularPrecioSugerido(); // Actualiza el precio final también
    }

    private void calcularPrecioSugerido() {
        try {
            if (txtMargen.getText() != null && !txtMargen.getText().isBlank()) {
                double margen = Double.parseDouble(txtMargen.getText());
                double gananciaPlata = costoTotalReceta * (margen / 100);
                double precioSugerido = costoTotalReceta + gananciaPlata;

                // Asignamos el valor a la variable global (la convertimos a BigDecimal)
                this.precioVenta = BigDecimal.valueOf(precioSugerido);

                lblPrecioFinal.setText(String.format("$ %.2f", precioSugerido));
            } else {
                // Si no hay margen, el precio de venta es el mismo que el costo
                this.precioVenta = BigDecimal.valueOf(costoTotalReceta);
                lblPrecioFinal.setText(String.format("$ %.2f", costoTotalReceta));
            }
        } catch (NumberFormatException e) {
            this.precioVenta = BigDecimal.ZERO;
            lblPrecioFinal.setText("$ ---");
        }
    }

    /*viejo metodo
    private void calcularPrecioSugerido() {

        try {
            if (txtMargen.getText() != null && !txtMargen.getText().isBlank()) {
                double margen = Double.parseDouble(txtMargen.getText());
                double gananciaPlata = costoTotalReceta * (margen / 100);
                double precioSugerido = costoTotalReceta + gananciaPlata;
                lblPrecioFinal.setText(String.format("$ %.2f", precioSugerido));
            } else {
                lblPrecioFinal.setText(String.format("$ %.2f", costoTotalReceta));
            }
        } catch (NumberFormatException e) {
            lblPrecioFinal.setText("$ ---"); // Si pone letras, mostramos rayitas
        }
    }*/

    @FXML
    public void guardarPresupuesto(ActionEvent event) {
        // 1. VALIDACIONES
        if (txtNombreProducto.getText() == null || txtNombreProducto.getText().isBlank()) {
            mostrarError("Debe ingresar el nombre del producto.");
            return;
        }
        if (tablaReceta.getItems().isEmpty()) {
            mostrarError("Debe agregar al menos un insumo.");
            return;
        }
        // Verificamos el precioVenta (que ahora es BigDecimal y está en la clase)
        if (precioVenta == null || precioVenta.compareTo(BigDecimal.ZERO) <= 0) {
            mostrarError("El precio de venta es inválido.");
            return;
        }

        // 2. CREACIÓN DEL OBJETO
        Articulo nuevoProducto = new Articulo();
        nuevoProducto.setNombre(txtNombreProducto.getText());
        nuevoProducto.setPrecio(precioVenta.doubleValue()); // Convertimos BigDecimal a Double
        nuevoProducto.setOrigen("PRODUCCION_PROPIA");

        try {
            // 3. LLAMADA AL SERVICIO
            // Usamos guardarArticulo que devuelve boolean (true si fue 200 o 201)
            boolean exito = articuloService.guardarArticulo(nuevoProducto);

            if (exito) {
                // 4. DESCUENTO DE STOCK
                // Recorremos la tabla para descontar los insumos
                for (DetalleCalculadora det : tablaReceta.getItems()) {
                    // Acá deberías llamar a tu servicio de insumos para descontar
                    // insumoApi.descontarStock(det.getInsumo().getId(), det.getCantidadUsada());
                    System.out.println("Descontando stock de: " + det.getInsumo().getNombre());
                }

                mostrarExito("¡Producto '" + nuevoProducto.getNombre() + "' guardado correctamente!");

                // Limpiamos la pantalla
                txtNombreProducto.clear();
                listaReceta.clear();
                // Reseteamos valores
                costoTotalReceta = 0.0;
                lblTotalCosto.setText("$ 0.00");
                lblPrecioFinal.setText("$ 0.00");

            } else {
                mostrarError("Error al guardar en el servidor. Verifique la conexión.");
            }
        } catch (Exception e) {
            mostrarError("Error inesperado: " + e.getMessage());
        }
    }

   /*private void mostrarError(String msj) {
        Alert a = new Alert(Alert.AlertType.WARNING, msj);
        a.setHeaderText(null);
        a.showAndWait();
    }*/

    private void mostrarExito(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // Aprovechá y agregá este también que seguro lo vas a necesitar
    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}