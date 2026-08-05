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
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.math.BigDecimal;

public class NuevoProveedorController {

    @FXML private Label lblTitulo;
    @FXML private TextField txtEmpresa;
    @FXML private TextField txtContacto;
    @FXML private ComboBox<String> cmbRubro;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;

    // 🔥 CAMBIO 1: Declaramos la variable del CUIT que agregamos al FXML
    @FXML private TextField txtCuit;

    @FXML private TextField txtSaldoFavor;
    @FXML private TextField txtSaldoContra;
    @FXML private TextArea txtComentarios;

    private final ProveedorApiService apiService = new ProveedorApiService();
    private Proveedor proveedorAEditar = null;

    @FXML
    public void initialize() {
        // Rubros ajustados a la realidad de su negocio
        cmbRubro.getItems().addAll(
                "Cueros y Telas",
                "Herrajes y Avíos",
                "Hilos y Cierres",
                "Insumos Marroquinería",
                "Productos Terminados (Mates, Billeteras)",
                "Packaging y Cajas",
                "Maquinaria y Taller",
                "Varios"
        );
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

        // 🔥 CAMBIO 2: Si el proveedor ya tiene un CUIT guardado, lo mostramos
        if (proveedor.getCuit() != null) {
            this.txtCuit.setText(proveedor.getCuit());
        } else {
            this.txtCuit.clear();
        }

        System.out.println("========== DEBUG EDITAR PROVEEDOR ==========");
        System.out.println("Proveedor seleccionado: " + proveedor.getRazonSocial());
        System.out.println("Saldo a Favor: " + proveedor.getSaldoFavor());
        System.out.println("Saldo en Contra: " + proveedor.getSaldoContra());
        System.out.println("============================================");

        // Llenamos los campos nuevos (si existen)
        if (proveedor.getSaldoFavor() != null) {
            this.txtSaldoFavor.setText(proveedor.getSaldoFavor().toString());
        } else {
            this.txtSaldoFavor.clear();
        }

        if (proveedor.getSaldoContra() != null) {
            this.txtSaldoContra.setText(proveedor.getSaldoContra().toString());
        } else {
            this.txtSaldoContra.clear();
        }

        if (proveedor.getComentarios() != null) {
            this.txtComentarios.setText(proveedor.getComentarios());
        } else {
            this.txtComentarios.clear();
        }
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

        // 🔥 CAMBIO 3: Ya no es un texto vacío, ahora lee lo que pusimos en el modal
        String cuit = txtCuit.getText() != null ? txtCuit.getText().trim() : "";

        String comentarios = txtComentarios.getText() != null ? txtComentarios.getText().trim() : "";

        // LÓGICA PARA PARSEAR EL SALDO A FAVOR
        BigDecimal saldoFavor = BigDecimal.ZERO;
        if (txtSaldoFavor.getText() != null && !txtSaldoFavor.getText().trim().isEmpty()) {
            try {
                String saldoLimpiado = txtSaldoFavor.getText().trim().replace(",", ".");
                saldoFavor = new BigDecimal(saldoLimpiado);
            } catch (NumberFormatException e) {
                mostrarAlerta("Error de formato", "El Saldo a Favor debe ser un número válido (Ej: 1500.00).", Alert.AlertType.WARNING);
                return;
            }
        }

        // LÓGICA PARA PARSEAR EL SALDO EN CONTRA
        BigDecimal saldoContra = BigDecimal.ZERO;
        if (txtSaldoContra.getText() != null && !txtSaldoContra.getText().trim().isEmpty()) {
            try {
                String saldoLimpiado = txtSaldoContra.getText().trim().replace(",", ".");
                saldoContra = new BigDecimal(saldoLimpiado);
            } catch (NumberFormatException e) {
                mostrarAlerta("Error de formato", "El Saldo en Contra debe ser un número válido (Ej: 500.50).", Alert.AlertType.WARNING);
                return;
            }
        }

        try {
            // Mandamos los datos fresquitos a la API
            if (proveedorAEditar == null) {
                apiService.guardarProveedoresEnBaseDeDatos(razonSocial, contacto, rubro, cuit, telefono, email, saldoFavor, saldoContra, comentarios);
            } else {
                apiService.actualizarProveedoresEnBaseDeDatos(proveedorAEditar.getId(), razonSocial, contacto, rubro, cuit, telefono, email, saldoFavor, saldoContra, comentarios);
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