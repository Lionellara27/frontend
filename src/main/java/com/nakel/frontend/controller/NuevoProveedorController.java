package com.nakel.frontend.controller;

import com.nakel.frontend.model.Proveedor;
import com.nakel.frontend.service.ProveedorApiService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;

public class NuevoProveedorController {

    @FXML private Label lblTitulo; // Acordate de agregarle el fx:id="lblTitulo" a este Label en tu FXML
    @FXML private TextField txtEmpresa;
    @FXML private TextField txtContacto;
    @FXML private ComboBox<String> cmbRubro;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;

    private final ProveedorApiService apiService = new ProveedorApiService();
    private Proveedor proveedorAEditar = null;

    @FXML
    public void initialize() {
        // Recuperamos tus rubros originales que estaban geniales
        cmbRubro.getItems().addAll("Cueros y Telas", "Herrajes", "Hilos y Cierres", "Maquinaria", "Varios");
    }

    public void cargarDatosParaEditar(Proveedor proveedor) {
        this.proveedorAEditar = proveedor;

        if (this.lblTitulo != null) {
            this.lblTitulo.setText("✏️ Editar Proveedor");
        }

        this.txtEmpresa.setText(proveedor.getRazonSocial());
        this.txtContacto.setText(proveedor.getNombreContacto());
        this.cmbRubro.setValue(proveedor.getRubro());
        this.txtTelefono.setText(proveedor.getTelefono());
        this.txtEmail.setText(proveedor.getEmail());
    }

    @FXML
    public void guardarProveedor(ActionEvent event) {
        if (txtEmpresa.getText() == null || txtEmpresa.getText().trim().isEmpty()) {
            mostrarAlerta("Error", "La Empresa / Marca es obligatoria.", Alert.AlertType.WARNING);
            return;
        }

        String razonSocial = txtEmpresa.getText().trim();
        String contacto = txtContacto.getText().trim();
        String rubro = cmbRubro.getValue() != null ? cmbRubro.getValue() : "";
        String telefono = txtTelefono.getText().trim();
        String email = txtEmail.getText().trim();

        String cuit = "";
        BigDecimal saldo = BigDecimal.ZERO;

        try {
            if (proveedorAEditar == null) {
                apiService.guardarProveedoresEnBaseDeDatos(razonSocial, contacto, rubro, cuit, telefono, email, saldo);
            } else {
                apiService.actualizarProveedoresEnBaseDeDatos(proveedorAEditar.getId(), razonSocial, contacto, rubro, cuit, telefono, email, saldo);
            }
            cerrarModal(event);
        } catch (Exception e) {
            mostrarAlerta("Error al guardar", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void cerrarModal(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}