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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DetalleVentaController {

    @FXML private Label lblTitulo;
    @FXML private Label lblFecha;
    @FXML private Label lblCliente;
    @FXML private Label lblRegalo;
    @FXML private Label lblTotal;

    // AVISO: Cambiá "Object" por tu clase real (ej: DetalleVenta o Articulo)
    @FXML private TableView<DetalleVenta> tablaDetalles;
    @FXML private TableColumn<DetalleVenta, String> colCantidad;
    @FXML private TableColumn<DetalleVenta, String> colDescripcion;
    @FXML private TableColumn<DetalleVenta, String> colPrecioUni;
    @FXML private TableColumn<DetalleVenta, String> colSubtotal;

    @FXML
    public void initialize() {
        // Configuramos cómo se lee cada columna de la tabla de detalles
        colCantidad.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getCantidad())));

        // Asumimos que tu DetalleVenta tiene getArticulo().getNombre() o similar.
        // Si tu modelo tiene getNombre() directo, cambialo por cell.getValue().getNombre()
        colDescripcion.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getArticulo().getNombre()));

        colPrecioUni.setCellValueFactory(cell -> new SimpleStringProperty("$ " + cell.getValue().getPrecioUnitario()));
        colSubtotal.setCellValueFactory(cell -> new SimpleStringProperty("$ " + cell.getValue().getSubtotal()));
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
        // (Asegurate de tener este boolean en tu modelo Venta)
        if (venta.getEsTicketCambio() != null && venta.getEsTicketCambio()) {
            lblRegalo.setVisible(true);
            lblRegalo.setManaged(true);
        }

        // 3. Llenamos la tablita
        // 🔥 Reemplazá venta.getDetalles() por el método real que tengas en tu modelo Venta
        // tablaDetalles.setItems(FXCollections.observableArrayList(venta.getDetalles()));
        if (venta.getDetalles() != null) {
            tablaDetalles.setItems(FXCollections.observableArrayList(venta.getDetalles()));
        }
    }

    @FXML
    public void cerrarModal(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}