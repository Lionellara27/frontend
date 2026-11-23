package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.CajaDiaria;
import com.nakel.frontend.model.MovimientoCaja;
import com.nakel.frontend.model.TipoMovimientoCaja;
import com.nakel.frontend.service.MovimientoCajaApiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ModalDetalleCajaController {

    @FXML private Label lblTitulo;
    @FXML private Label lblFechaApertura;
    @FXML private Label lblFechaCierre;
    @FXML private Label lblEstado;
    @FXML private Label lblTotalVentas;
    @FXML private Label lblEgresos;
    @FXML private Label lblSaldoFinal;

    // Con qué se cobró en el día
    @FXML private Label lblEfectivo;
    @FXML private Label lblTransferencias;
    @FXML private Label lblMercadoPago;
    @FXML private Label lblDebito;
    @FXML private Label lblCredito;
    @FXML private Label lblVales;

    @FXML private TableView<MovimientoCaja> tablaMovimientos;
    @FXML private TableColumn<MovimientoCaja, String> colHora;
    @FXML private TableColumn<MovimientoCaja, String> colTipo;
    @FXML private TableColumn<MovimientoCaja, String> colConcepto;
    @FXML private TableColumn<MovimientoCaja, String> colDetalle;
    @FXML private TableColumn<MovimientoCaja, String> colMedio;
    @FXML private TableColumn<MovimientoCaja, String> colMonto;

    private final MovimientoCajaApiService movimientoApiService = new MovimientoCajaApiService();
    private final Gson gson = new Gson();
    private final ObservableList<MovimientoCaja> listaMovimientos = FXCollections.observableArrayList();

    private static String pesos(BigDecimal monto) {
        return com.nakel.frontend.util.FormatoMoneda.pesos(monto); // mismo formato en todo el sistema
    }

    public void inicializarDatos(CajaDiaria caja) {
        if (lblTitulo != null) lblTitulo.setText("Detalle de Caja N° " + caja.getId());

        lblFechaApertura.setText(HistorialCajasController.formatearFecha(caja.getFechaApertura()));
        lblFechaCierre.setText(caja.getFechaCierre() != null
                ? HistorialCajasController.formatearFecha(caja.getFechaCierre())
                : "Abierta actualmente");
        lblEstado.setText("ABIERTA".equalsIgnoreCase(caja.getEstado()) ? "Abierta" : "Cerrada");
        lblTotalVentas.setText(pesos(caja.getTotalVentas()));
        lblEgresos.setText(pesos(caja.getTotalEgresos()));
        lblSaldoFinal.setText(pesos(caja.getSaldoFinal()));

        if (lblEfectivo != null) lblEfectivo.setText(pesos(caja.getTotalEfectivo()));
        if (lblTransferencias != null) lblTransferencias.setText(pesos(caja.getTotalTransferencias()));
        if (lblMercadoPago != null) lblMercadoPago.setText(pesos(caja.getTotalMercadoPago()));
        if (lblDebito != null) lblDebito.setText(pesos(caja.getTotalTarjetaDebito()));
        if (lblCredito != null) lblCredito.setText(pesos(caja.getTotalTarjetaCredito()));

        configurarTabla();
        cargarMovimientos(caja.getId());

        // Lo pagado con vales no es plata nueva del día: se calcula sumando los "Vale usado"
        if (lblVales != null) {
            BigDecimal vales = BigDecimal.ZERO;
            for (MovimientoCaja mov : listaMovimientos) {
                if (mov.getTipo() == TipoMovimientoCaja.AJUSTE && mov.getConcepto() != null
                        && mov.getConcepto().startsWith("Vale usado") && mov.getMonto() != null) {
                    vales = vales.add(mov.getMonto());
                }
            }
            lblVales.setText(pesos(vales));
        }
    }

    // Nombre claro de cada tipo de movimiento (en la base se guarda en mayúsculas y con guion bajo)
    private static String nombreTipo(MovimientoCaja mov) {
        TipoMovimientoCaja tipo = mov.getTipo();
        if (tipo == null) return "-";
        switch (tipo) {
            case APERTURA_CAJA: return "Apertura";
            case CIERRE_CAJA: return "Cierre";
            case VENTA: return "Venta";
            case DEVOLUCION: return "Cambio";
            case INGRESO: return "Ingreso";
            case EGRESO: return "Egreso";
            case PAGO_PROVEEDOR: return "Pago a proveedor";
            case RETIRO_CAJA: return "Retiro";
            case AJUSTE:
                return (mov.getConcepto() != null && mov.getConcepto().startsWith("Vale")) ? "Vale" : "Ajuste";
            default: return tipo.name();
        }
    }

    private void configurarTabla() {
        colTipo.setCellValueFactory(cell -> new SimpleStringProperty(nombreTipo(cell.getValue())));
        colConcepto.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getConcepto()));

        if (colDetalle != null) {
            colDetalle.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getDescripcion() != null ? cell.getValue().getDescripcion() : ""));
            // El detalle puede ser largo: al dejar el mouse encima se lee completo
            colDetalle.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String texto, boolean vacio) {
                    super.updateItem(texto, vacio);
                    if (vacio || texto == null || texto.isBlank()) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        setText(texto);
                        Tooltip completo = new Tooltip(texto);
                        completo.setWrapText(true);
                        completo.setMaxWidth(520);
                        setTooltip(completo);
                    }
                }
            });
        }

        if (colMedio != null) {
            colMedio.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getMedioPago() != null ? cell.getValue().getMedioPago() : "-"));
        }

        // Los movimientos que solo informan (cambios y vales) no son plata que entra: se muestran entre paréntesis
        colMonto.setCellValueFactory(cell -> {
            MovimientoCaja mov = cell.getValue();
            BigDecimal monto = mov.getMonto() != null ? mov.getMonto() : BigDecimal.ZERO;
            TipoMovimientoCaja tipo = mov.getTipo();

            if (tipo == TipoMovimientoCaja.APERTURA_CAJA && monto.signum() == 0) return new SimpleStringProperty("-");
            if (tipo == TipoMovimientoCaja.DEVOLUCION || tipo == TipoMovimientoCaja.AJUSTE) {
                return new SimpleStringProperty("(" + pesos(monto) + ")");
            }
            return new SimpleStringProperty(pesos(monto));
        });

        colHora.setCellValueFactory(cellData -> {
            String fechaCruda = cellData.getValue().getFechaHora();
            if (fechaCruda != null && fechaCruda.contains("T")) {
                return new SimpleStringProperty(fechaCruda.split("T")[1].substring(0, 5));
            }
            return new SimpleStringProperty(fechaCruda);
        });

        tablaMovimientos.setPlaceholder(new Label("Esta caja no tiene movimientos."));
        tablaMovimientos.setItems(listaMovimientos);
    }

    private void cargarMovimientos(Long cajaId) {
        String json = movimientoApiService.obtenerMovimientosPorCaja(cajaId);
        listaMovimientos.clear();
        if (json != null && !json.isEmpty()) {
            try {
                Type listType = new TypeToken<List<MovimientoCaja>>(){}.getType();
                List<MovimientoCaja> movimientos = gson.fromJson(json, listType);
                if (movimientos != null) {
                    listaMovimientos.addAll(movimientos);
                }
            } catch (Exception e) {
                System.out.println("Error al leer los movimientos de la caja: " + e.getMessage());
            }
        }
    }

    @FXML
    public void cerrarModal() {
        Stage stage = (Stage) lblEstado.getScene().getWindow();
        stage.close();
    }
}
