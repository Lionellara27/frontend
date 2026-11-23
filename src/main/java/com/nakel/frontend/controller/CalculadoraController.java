package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.DetalleCalculadora;
import com.nakel.frontend.model.Insumo;
import com.nakel.frontend.service.InsumoApiService;
import com.nakel.frontend.util.FormatoMoneda;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * Calculadora de Producción.
 *
 * Bloque 2 (insumos): una tabla con buscador y páginas. La columna "Disponible" muestra el stock
 * real MENOS lo que ya está cargado en la receta de esta pantalla. Ese descuento es solo visual
 * (vive en memoria): la base no se toca hasta que se confirma "Crear producto y fabricar".
 * Si se cierra la pantalla o el sistema sin fabricar, no se descontó nada.
 */
public class CalculadoraController {

    private static final int INSUMOS_POR_PAGINA = 20;

    private BigDecimal precioVenta = BigDecimal.ZERO;

    @FXML private TextField txtNombreProducto;

    // --- Lista de insumos (buscador + tabla + páginas) ---
    @FXML private TextField txtBuscarInsumo;
    @FXML private Button btnPaginaAnterior;
    @FXML private Button btnPaginaSiguiente;
    @FXML private Label lblPaginaInsumos;
    @FXML private TableView<Insumo> tablaInsumos;
    @FXML private TableColumn<Insumo, String> colInsNombre;
    @FXML private TableColumn<Insumo, String> colInsUnidad;
    @FXML private TableColumn<Insumo, String> colInsDisponible;
    @FXML private TableColumn<Insumo, String> colInsCosto;

    // --- Medida o cantidad del insumo elegido ---
    @FXML private Label lblAyudaMedida;
    @FXML private Label lblInsumoElegido;
    @FXML private Label lblStock;
    @FXML private javafx.scene.layout.HBox boxSuperficie;
    @FXML private javafx.scene.layout.VBox boxCantidad;
    @FXML private Label lblCantidad;
    @FXML private TextField txtAncho;
    @FXML private TextField txtLargo;
    @FXML private TextField txtCantidad;

    // --- Receta ---
    @FXML private TableView<DetalleCalculadora> tablaReceta;
    @FXML private TableColumn<DetalleCalculadora, String> colInsumo;
    @FXML private TableColumn<DetalleCalculadora, String> colUso;
    @FXML private TableColumn<DetalleCalculadora, String> colSubtotal;
    @FXML private TableColumn<DetalleCalculadora, Void> colQuitar;

    // --- Precio ---
    @FXML private Label lblTotalCosto;
    @FXML private TextField txtMargen;
    @FXML private Label lblGanancia;
    @FXML private Label lblPrecioFinal;

    private final ObservableList<Insumo> listaInsumos = FXCollections.observableArrayList();
    private final ObservableList<DetalleCalculadora> listaReceta = FXCollections.observableArrayList();
    private double costoTotalReceta = 0.0;

    private final InsumoApiService insumoApi = new InsumoApiService();
    private final Gson gson = new Gson();

    private int paginaInsumos = 0;
    private int totalPaginasInsumos = 1;

    private final DecimalFormat formatoCantidad =
            new DecimalFormat("#,##0.##", new DecimalFormatSymbols(new Locale("es", "AR")));

    @FXML
    public void initialize() {
        configurarTablaReceta();
        configurarTablaInsumos();

        // Buscador: espera un instante a que se termine de escribir y busca en TODOS los insumos
        javafx.animation.PauseTransition espera = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        espera.setOnFinished(e -> cargarInsumos(0));
        txtBuscarInsumo.textProperty().addListener((obs, viejo, nuevo) -> espera.playFromStart());
        com.nakel.frontend.util.Atajos.buscarConF3(txtBuscarInsumo, null);

        txtMargen.textProperty().addListener((obs, viejo, nuevo) -> calcularPrecioSugerido());

        cargarInsumos(0);
    }

    // =====================================================================
    //  LISTA DE INSUMOS
    // =====================================================================

    private void configurarTablaInsumos() {
        colInsNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colInsUnidad.setCellValueFactory(c -> new SimpleStringProperty(unidadDe(c.getValue())));
        colInsDisponible.setCellValueFactory(c -> new SimpleStringProperty(textoDisponible(c.getValue())));
        colInsCosto.setCellValueFactory(c -> new SimpleStringProperty(textoCosto(c.getValue())));

        tablaInsumos.setItems(listaInsumos);
        tablaInsumos.setFixedCellSize(32); // renglones bajos: entran más insumos a la vista
        tablaInsumos.setPlaceholder(new Label("No hay insumos que coincidan con la búsqueda."));

        // Lo que ya no tiene disponible se ve apagado
        tablaInsumos.setRowFactory(tabla -> new TableRow<>() {
            @Override
            protected void updateItem(Insumo insumo, boolean empty) {
                super.updateItem(insumo, empty);
                boolean agotado = !empty && insumo != null && tieneStock(insumo) && disponible(insumo) <= 0;
                setOpacity(agotado ? 0.45 : 1);
            }
        });

        tablaInsumos.getSelectionModel().selectedItemProperty()
                .addListener((obs, viejo, nuevo) -> prepararCampos(nuevo));
    }

    /** Pide al backend una página de insumos (horas primero, después unidades, después metros). */
    private void cargarInsumos(int pagina) {
        String json = insumoApi.obtenerInsumosCalculadora(pagina, INSUMOS_POR_PAGINA, txtBuscarInsumo.getText());
        try {
            if (json == null) throw new IllegalStateException("el servidor no respondió");
            JsonObject respuesta = JsonParser.parseString(json).getAsJsonObject();
            List<Insumo> insumos = gson.fromJson(respuesta.getAsJsonArray("content"),
                    new TypeToken<List<Insumo>>() {}.getType());

            totalPaginasInsumos = Math.max(1, respuesta.get("totalPages").getAsInt());
            paginaInsumos = Math.min(pagina, totalPaginasInsumos - 1);
            listaInsumos.setAll(insumos != null ? insumos : List.of());
        } catch (Exception e) {
            listaInsumos.clear();
            totalPaginasInsumos = 1;
            paginaInsumos = 0;
            tablaInsumos.setPlaceholder(new Label("No se pudieron cargar los insumos. Revisá que el sistema esté iniciado."));
            System.err.println("Error al cargar insumos para la calculadora: " + e.getMessage());
        }

        lblPaginaInsumos.setText("Página " + (paginaInsumos + 1) + " de " + totalPaginasInsumos);
        btnPaginaAnterior.setDisable(paginaInsumos <= 0);
        btnPaginaSiguiente.setDisable(paginaInsumos >= totalPaginasInsumos - 1);
        prepararCampos(null); // al cambiar la lista no queda ningún insumo elegido
    }

    @FXML
    public void paginaAnterior(ActionEvent event) {
        if (paginaInsumos > 0) cargarInsumos(paginaInsumos - 1);
    }

    @FXML
    public void paginaSiguiente(ActionEvent event) {
        if (paginaInsumos < totalPaginasInsumos - 1) cargarInsumos(paginaInsumos + 1);
    }

    // =====================================================================
    //  STOCK DISPONIBLE (real menos lo cargado en la receta, solo en pantalla)
    // =====================================================================

    /** SUPERFICIE, UNIDAD o TIEMPO (los servicios se tratan como tiempo). */
    private String tipoDe(Insumo insumo) {
        if (insumo == null || insumo.getCategoria() == null || insumo.getCategoria().getTipoMedicion() == null) return "";
        String tipo = insumo.getCategoria().getTipoMedicion().trim().toUpperCase();
        return "SERVICIO".equals(tipo) ? "TIEMPO" : tipo;
    }

    /** La mano de obra no tiene stock: se puede cargar cualquier cantidad de horas. */
    private boolean tieneStock(Insumo insumo) {
        String tipo = tipoDe(insumo);
        return "SUPERFICIE".equals(tipo) || "UNIDAD".equals(tipo);
    }

    /** Cuánto de este insumo ya está puesto en la receta (cm² o unidades). */
    private double reservado(Insumo insumo) {
        double total = 0;
        for (DetalleCalculadora det : listaReceta) {
            if (det.getInsumo() != null && det.getInsumo().getId() != null
                    && det.getInsumo().getId().equals(insumo.getId())) {
                total += det.getCantidadUsada();
            }
        }
        return total;
    }

    /** Stock real menos lo reservado en esta receta. No se guarda en ningún lado. */
    private double disponible(Insumo insumo) {
        double stock = 0;
        if ("SUPERFICIE".equals(tipoDe(insumo))) {
            stock = insumo.getAreaActualCm2() != null ? insumo.getAreaActualCm2() : 0;
        } else if ("UNIDAD".equals(tipoDe(insumo))) {
            stock = insumo.getCantidadActual() != null ? insumo.getCantidadActual() : 0;
        }
        return stock - reservado(insumo);
    }

    private String unidadDe(Insumo insumo) {
        return switch (tipoDe(insumo)) {
            case "SUPERFICIE" -> "Metros";
            case "TIEMPO" -> "Horas";
            default -> "Unidades";
        };
    }

    private String textoDisponible(Insumo insumo) {
        return switch (tipoDe(insumo)) {
            case "SUPERFICIE" -> formatoCantidad.format(disponible(insumo)) + " cm²";
            case "UNIDAD" -> formatoCantidad.format(disponible(insumo)) + " unidades";
            default -> "Sin límite";
        };
    }

    private String textoCosto(Insumo insumo) {
        String costo = FormatoMoneda.pesos(insumo.getCostoCalculado());
        return switch (tipoDe(insumo)) {
            case "SUPERFICIE" -> costo + " / cm²";
            case "TIEMPO" -> costo + " / hora";
            default -> costo + " c/u";
        };
    }

    // =====================================================================
    //  MEDIDA / CANTIDAD DEL INSUMO ELEGIDO
    // =====================================================================

    private void prepararCampos(Insumo insumo) {
        txtAncho.clear();
        txtLargo.clear();
        txtCantidad.clear();

        if (insumo == null) {
            mostrar(lblAyudaMedida, true);
            mostrar(lblInsumoElegido, false);
            mostrar(lblStock, false);
            mostrar(boxSuperficie, false);
            mostrar(boxCantidad, false);
            return;
        }

        boolean esSuperficie = "SUPERFICIE".equals(tipoDe(insumo));
        mostrar(lblAyudaMedida, false);
        mostrar(lblInsumoElegido, true);
        mostrar(lblStock, true);
        mostrar(boxSuperficie, esSuperficie);
        mostrar(boxCantidad, !esSuperficie);

        lblInsumoElegido.setText(insumo.getNombre());
        lblCantidad.setText("TIEMPO".equals(tipoDe(insumo)) ? "Horas" : "Cantidad (unidades)");
        actualizarTextoStock(insumo);

        // El cursor queda listo para escribir la medida
        javafx.application.Platform.runLater(() -> (esSuperficie ? txtAncho : txtCantidad).requestFocus());
    }

    private void actualizarTextoStock(Insumo insumo) {
        lblStock.setText(tieneStock(insumo) ? "Disponible: " + textoDisponible(insumo) : "Mano de obra: sin límite de horas");
    }

    private void mostrar(javafx.scene.Node nodo, boolean visible) {
        nodo.setVisible(visible);
        nodo.setManaged(visible);
    }

    // =====================================================================
    //  RECETA
    // =====================================================================

    private void configurarTablaReceta() {
        colInsumo.setCellValueFactory(new PropertyValueFactory<>("nombreInsumo"));
        colUso.setCellValueFactory(new PropertyValueFactory<>("descripcionUso"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotalFormateado"));
        tablaReceta.setItems(listaReceta);
        tablaReceta.setFixedCellSize(32);
        tablaReceta.setPlaceholder(new Label("Elegí insumos de la lista de arriba y agregalos a la receta."));

        // Tachito rojo en cada fila: al quitar, lo reservado vuelve a estar disponible
        colQuitar.setCellFactory(col -> new TableCell<>() {
            private final Button btnQuitar = new Button();
            {
                com.nakel.frontend.util.Acciones.eliminar(btnQuitar, "Quitar de la receta");
                btnQuitar.setOnAction(e -> {
                    listaReceta.remove(getTableView().getItems().get(getIndex()));
                    recalcularCostoTotal();
                    refrescarDisponibles();
                });
                setAlignment(javafx.geometry.Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnQuitar);
            }
        });
    }

    /** Vuelve a dibujar la columna "Disponible" y el texto del insumo elegido. */
    private void refrescarDisponibles() {
        tablaInsumos.refresh();
        Insumo elegido = tablaInsumos.getSelectionModel().getSelectedItem();
        if (elegido != null) actualizarTextoStock(elegido);
    }

    @FXML
    public void agregarInsumoAReceta(ActionEvent event) {
        Insumo insumo = tablaInsumos.getSelectionModel().getSelectedItem();
        if (insumo == null) {
            mostrarError("Elegí primero un insumo de la lista.");
            return;
        }

        String tipo = tipoDe(insumo);
        try {
            DetalleCalculadora detalle;

            if ("SUPERFICIE".equals(tipo)) {
                int ancho = Integer.parseInt(txtAncho.getText().trim());
                int largo = Integer.parseInt(txtLargo.getText().trim());
                if (ancho <= 0 || largo <= 0) {
                    mostrarError("El ancho y el largo tienen que ser mayores a 0.");
                    return;
                }

                // Un corte más largo que la pieza original no puede salir de ella, aunque los cm² alcancen
                int ladoMayorPieza = Math.max(valor(insumo.getAnchoLoteCm()), valor(insumo.getLargoLoteCm()));
                int ladoMenorPieza = Math.min(valor(insumo.getAnchoLoteCm()), valor(insumo.getLargoLoteCm()));
                if (ladoMayorPieza > 0 && (Math.max(ancho, largo) > ladoMayorPieza || Math.min(ancho, largo) > ladoMenorPieza)) {
                    mostrarError("Ese corte no entra en la pieza.\n\nLa pieza de " + insumo.getNombre() + " mide "
                            + insumo.getAnchoLoteCm() + " x " + insumo.getLargoLoteCm() + " cm y el corte pedido es de "
                            + ancho + " x " + largo + " cm.");
                    return;
                }

                int cm2Usados = ancho * largo;
                if (cm2Usados > disponible(insumo)) {
                    mostrarError("No alcanza el material.\n\nEl corte de " + ancho + " x " + largo + " cm usa "
                            + formatoCantidad.format(cm2Usados) + " cm² y de " + insumo.getNombre()
                            + " quedan disponibles " + textoDisponible(insumo) + ".");
                    return;
                }

                double subtotal = insumo.getCostoCalculado().doubleValue() * cm2Usados;
                detalle = new DetalleCalculadora(insumo, cm2Usados, "Superficie", subtotal, ancho, largo);

            } else if ("UNIDAD".equals(tipo)) {
                double cantidad = leerNumero(txtCantidad);
                if (cantidad <= 0) {
                    mostrarError("La cantidad tiene que ser mayor a 0.");
                    return;
                }
                if (cantidad > disponible(insumo)) {
                    mostrarError("No alcanza el stock.\n\nDe " + insumo.getNombre() + " quedan disponibles "
                            + textoDisponible(insumo) + ".");
                    return;
                }

                double subtotal = insumo.getCostoCalculado().doubleValue() * cantidad;
                detalle = new DetalleCalculadora(insumo, cantidad, "Unidad", subtotal, null, null);

            } else if ("TIEMPO".equals(tipo)) {
                double horas = leerNumero(txtCantidad);
                if (horas <= 0) {
                    mostrarError("Las horas tienen que ser mayores a 0.");
                    return;
                }

                double subtotal = insumo.getCostoCalculado().doubleValue() * horas;
                detalle = new DetalleCalculadora(insumo, horas, "Horas", subtotal, null, null);

            } else {
                mostrarError("Este insumo no tiene bien cargada la categoría (no se sabe cómo se mide).");
                return;
            }

            listaReceta.add(detalle);
            recalcularCostoTotal();
            txtAncho.clear();
            txtLargo.clear();
            txtCantidad.clear();
            refrescarDisponibles();

        } catch (NumberFormatException e) {
            mostrarError("Por favor, ingrese solamente números válidos.");
        }
    }

    private int valor(Integer numero) {
        return numero != null ? numero : 0;
    }

    /** Acepta coma o punto como decimal ("1,5" o "1.5"). */
    private double leerNumero(TextField campo) {
        return Double.parseDouble(campo.getText().trim().replace(",", "."));
    }

    // =====================================================================
    //  PRECIO
    // =====================================================================

    private void recalcularCostoTotal() {
        costoTotalReceta = 0.0;
        for (DetalleCalculadora det : listaReceta) {
            costoTotalReceta += det.getSubtotal();
        }
        lblTotalCosto.setText(FormatoMoneda.pesos(costoTotalReceta));
        calcularPrecioSugerido();
    }

    private void calcularPrecioSugerido() {
        try {
            if (txtMargen.getText() != null && !txtMargen.getText().isBlank()) {
                double margen = Double.parseDouble(txtMargen.getText());
                double gananciaPlata = costoTotalReceta * (margen / 100);
                double precioSugerido = costoTotalReceta + gananciaPlata;
                this.precioVenta = BigDecimal.valueOf(precioSugerido);
                lblPrecioFinal.setText(FormatoMoneda.pesos(precioSugerido));
                lblGanancia.setText(FormatoMoneda.pesos(gananciaPlata));
            } else {
                this.precioVenta = BigDecimal.valueOf(costoTotalReceta);
                lblPrecioFinal.setText(FormatoMoneda.pesos(costoTotalReceta));
                lblGanancia.setText(FormatoMoneda.pesos(0));
            }
        } catch (NumberFormatException e) {
            this.precioVenta = BigDecimal.ZERO;
            lblPrecioFinal.setText("$ ---");
            lblGanancia.setText("$ ---");
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

        // Atajamos el margen antes de que rompa nada
        String textoMargen = txtMargen.getText();

        if (textoMargen == null || textoMargen.trim().isEmpty()) {

            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacion.setTitle("Atención - Margen Vacío");
            confirmacion.setHeaderText("No ingresaste ningún porcentaje de margen.");
            confirmacion.setContentText("¿Estás seguro de que querés guardar este producto con 0% de ganancia (al costo)?");

            // Si elige "Cancelar" o cierra con la X, abortamos el guardado
            if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }

            // Si eligió "Aceptar", clavamos el 0 en la cajita (el listener recalcula el precio)
            txtMargen.setText("0");

        } else {
            // Atajamos si puso comas en vez de puntos o si puso letras
            try {
                textoMargen = textoMargen.replace(",", ".");
                txtMargen.setText(textoMargen); // Actualizamos la cajita visualmente
                Double.parseDouble(textoMargen); // Solo para probar que no explote
            } catch (NumberFormatException e) {
                mostrarError("El margen debe ser un número válido (Ej: 50 o 50.5).");
                return;
            }
        }

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
                // Recién ahora el stock se descontó de verdad: volvemos a pedirlo al backend
                cargarInsumos(paginaInsumos);
                mostrarExito("¡Producto creado y stock descontado!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al abrir la ventana de finalización de producto.");
        }
    }

    // Busca el material de superficie más utilizado
    private Insumo determinarMaterialDestacado() {
        Insumo ganador = null;
        double maxSuperficie = 0.0;

        for (DetalleCalculadora det : listaReceta) {
            Insumo ins = det.getInsumo();
            if ("SUPERFICIE".equals(tipoDe(ins))) {
                double cantidadUsada = det.getCantidadUsada();
                if (cantidadUsada > maxSuperficie) {
                    maxSuperficie = cantidadUsada;
                    ganador = ins;
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
