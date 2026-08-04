package com.nakel.frontend.controller;

import com.nakel.frontend.model.DetalleVenta;
import com.nakel.frontend.model.Venta;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import javafx.scene.control.TableCell;
import javafx.scene.text.Text;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DetalleVentaController {

    @FXML private Label lblTitulo;
    @FXML private Label lblFecha;
    @FXML private Label lblCliente;
    @FXML private Label lblRegalo;
    @FXML private Label lblTotal;

    @FXML private TableView<DetalleVenta> tablaDetalles;
    @FXML private TableColumn<DetalleVenta, String> colCantidad;
    @FXML private TableColumn<DetalleVenta, String> colDescripcion;
    @FXML private TableColumn<DetalleVenta, String> colPrecioUni;
    @FXML private TableColumn<DetalleVenta, String> colSubtotal;

    // 🔥 1. NUEVAS VARIABLES PARA EL HISTORIAL DE CAMBIOS
    @FXML private Label lblAvisoCambios;
    @FXML private TableView<com.nakel.frontend.model.Cambio> tablaCambios;
    @FXML private TableColumn<com.nakel.frontend.model.Cambio, String> colFechaCambio;
    @FXML private TableColumn<com.nakel.frontend.model.Cambio, String> colResumenCambio;
    @FXML private TableColumn<com.nakel.frontend.model.Cambio, String> colDiferenciaCambio;

    // 🔥 2. EL SERVICIO API
    private final com.nakel.frontend.service.CambioApiService cambioApi = new com.nakel.frontend.service.CambioApiService();

    @FXML
    public void initialize() {
        // Configuramos cómo se lee cada columna de la tabla de detalles original
        colCantidad.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getCantidad())));
        colDescripcion.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getArticulo().getNombre()));
        colPrecioUni.setCellValueFactory(cell -> new SimpleStringProperty("$ " + cell.getValue().getPrecioUnitario()));
        colSubtotal.setCellValueFactory(cell -> new SimpleStringProperty("$ " + cell.getValue().getSubtotal()));

        // 🔥 3. CONFIGURAMOS LA TABLA DE CAMBIOS (si existe en la vista)
        if (colFechaCambio != null && colResumenCambio != null) {
            // 3.A - Asignamos QUÉ dato va en cada columna (Esto ya lo tenías)
            colFechaCambio.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFechaCambio()));
            colResumenCambio.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getResumenArticulos()));
            colDiferenciaCambio.setCellValueFactory(cell -> new SimpleStringProperty("$ " + cell.getValue().getDiferenciaCobrada()));

            // 3.B - CÓMO SE VE LA FECHA: Formato lindo (dd/MM/yyyy - HH:mm)
            DateTimeFormatter formatoHumano = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");

            colFechaCambio.setCellFactory(tc -> new TableCell<com.nakel.frontend.model.Cambio, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        try {
                            // Convertimos el String feo de la base de datos a LocalDateTime y le damos formato
                            LocalDateTime fechaParsed = LocalDateTime.parse(item);
                            setText(formatoHumano.format(fechaParsed));
                        } catch (Exception e) {
                            // Si por algún motivo falla, mostramos el texto original
                            setText(item);
                        }
                    }
                }
            });

            // 3.C - CÓMO SE VE EL RESUMEN: Multilínea (Auto-wrap)
            colResumenCambio.setCellFactory(tc -> new TableCell<com.nakel.frontend.model.Cambio, String>() {
                private final Text text = new Text();

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        text.setText(item);
                        // Ajusta el texto al ancho de la celda menos un pequeño margen
                        text.wrappingWidthProperty().bind(tc.widthProperty().subtract(12));
                        setGraphic(text);
                    }
                }
            });
        }
    }

    public void cargarDatosVenta(Venta venta) {
        // 1. Textos principales
        lblTitulo.setText("Detalle de Venta #" + String.format("%08d", venta.getId()));

        String nombreCliente = (venta.getCliente() != null) ? venta.getCliente().getNombre() : "Consumidor Final";
        lblCliente.setText("Cliente: " + nombreCliente);

        lblTotal.setText(String.format("$ %.2f", venta.getTotal()));

        // Formateo de fecha
        try {
            LocalDateTime dateTime = LocalDateTime.parse(venta.getFechaHora());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");
            lblFecha.setText("Fecha: " + dateTime.format(formatter));
        } catch (Exception e) {
            lblFecha.setText("Fecha: " + venta.getFechaHora());
        }

        // 2. Lógica del Indicador Visual (El Regalo 🎁)
        if (venta.getEsTicketCambio() != null && venta.getEsTicketCambio()) {
            lblRegalo.setVisible(true);
            lblRegalo.setManaged(true);
        } else {
            lblRegalo.setVisible(false);
            lblRegalo.setManaged(false);
        }

        // 3. Llenamos la tablita original
        if (venta.getDetalles() != null) {
            tablaDetalles.setItems(FXCollections.observableArrayList(venta.getDetalles()));
        }

        // 🔥 4. BUSCAMOS Y MOSTRAMOS EL HISTORIAL DE CAMBIOS
        java.util.List<com.nakel.frontend.model.Cambio> historial = cambioApi.obtenerHistorialPorVenta(venta.getId());

        if (historial != null && !historial.isEmpty()) {
            // Hay cambios: mostramos el cartelito y llenamos la tabla extra
            if (lblAvisoCambios != null) {
                lblAvisoCambios.setText("⚠️ Esta venta tiene " + historial.size() + " cambio(s) registrado(s)");
                lblAvisoCambios.setVisible(true);
                lblAvisoCambios.setManaged(true);
            }
            if (tablaCambios != null) {
                tablaCambios.setItems(FXCollections.observableArrayList(historial));
                tablaCambios.setVisible(true);
                tablaCambios.setManaged(true);
            }
        } else {
            // No hay cambios: escondemos todo lo relacionado a cambios
            if (lblAvisoCambios != null) {
                lblAvisoCambios.setVisible(false);
                lblAvisoCambios.setManaged(false);
            }
            if (tablaCambios != null) {
                tablaCambios.setVisible(false);
                tablaCambios.setManaged(false);
            }
        }
    }

    @FXML
    public void cerrarModal(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}