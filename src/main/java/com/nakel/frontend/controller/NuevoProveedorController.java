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

    @FXML private Label lblTitulo;
    @FXML private TextField txtEmpresa;
    @FXML private TextField txtContacto;
    @FXML private ComboBox<String> cmbRubro;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;

    // 🔥 NUEVO CAMPO DE SALDO
    @FXML private TextField txtSaldo;

    private final ProveedorApiService apiService = new ProveedorApiService();
    private Proveedor proveedorAEditar = null;

    @FXML
    public void initialize() {
        // 🔥 Rubros ajustados a la realidad de su negocio
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

        // 🕵️‍♂️ MICRÓFONO PARA CAZAR EL BUG DE LOS 100000
        System.out.println("========== DEBUG EDITAR PROVEEDOR ==========");
        System.out.println("Proveedor seleccionado: " + proveedor.getRazonSocial());
        System.out.println("Saldo que trae el objeto: " + proveedor.getSaldo());
        System.out.println("============================================");

        // 🔥 Solución al problema visual (El texto gris)
        if (proveedor.getSaldo() != null) {
            this.txtSaldo.setText(proveedor.getSaldo().toString());
        } else {
            this.txtSaldo.clear(); // ✨ Esto deja el campo vacío y muestra el promptText gris
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
        String cuit = "";

        // 🔥 LÓGICA DEL SALDO
        BigDecimal saldo = BigDecimal.ZERO;
        if (txtSaldo.getText() != null && !txtSaldo.getText().trim().isEmpty()) {
            try {
                // Reemplazamos coma por punto por si la clienta tipea "1500,50"
                String saldoLimpiado = txtSaldo.getText().trim().replace(",", ".");
                saldo = new BigDecimal(saldoLimpiado);
            } catch (NumberFormatException e) {
                mostrarAlerta("Error de formato", "El saldo debe ser un número válido (Ej: 1500.00 o -500).", Alert.AlertType.WARNING);
                return;
            }
        }

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