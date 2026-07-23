package com.nakel.frontend.controller;

import com.nakel.frontend.model.Categoria;
import com.nakel.frontend.model.DetalleCalculadora;
import com.nakel.frontend.model.Insumo;
import com.nakel.frontend.model.Receta;
import com.nakel.frontend.service.ProduccionApiService;
import com.nakel.frontend.service.ParametrosApiService; // Importamos tu servicio de categorías
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AltaProductoProduccionController {

    private boolean guardadoExitoso = false;

    @FXML private TextField txtNombre;
    @FXML private TextField txtCodigoSKU;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private TextField txtMaterialDestacado;
    @FXML private TextField txtOrigen;
    @FXML private TextField txtCosto;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtStock;

    private double costoCalculado;
    private double precioCalculado;
    private Insumo insumoDestacado;
    private ObservableList<DetalleCalculadora> itemsReceta;

    // --- SERVICIOS ---
    private final ProduccionApiService produccionApiService = new ProduccionApiService();
    private final ParametrosApiService parametrosService = new ParametrosApiService();

    @FXML
    public void initialize() {
        txtOrigen.setText("PRODUCCION_PROPIA");
        txtOrigen.setDisable(true);
        txtStock.setText("1"); // Stock inicial por defecto

        // 1. Cargamos las categorías reutilizando tu servicio existente
        cargarCategorias();

        // 2. ESCUCHADOR: Cuando elije una categoría, pre-escribe el prefijo y busca el sugerido
        cmbCategoria.getSelectionModel().selectedItemProperty().addListener((obs, vieja, nuevaCat) -> {
            if (nuevaCat != null) {
                // 🔥 LA MAGIA DEL PREFIJO (Ej: 2222-)
                if (nuevaCat.getPrefijoSku() != null) {
                    txtCodigoSKU.setText(nuevaCat.getPrefijoSku() + "-0001");
                    txtCodigoSKU.positionCaret(txtCodigoSKU.getText().length()); // Cursor al final para borrar fácil
                }

                // Le pedimos al backend el número real que sigue
                buscarSkuSugerido(nuevaCat.getId());
            }
        });
    }

    // 📥 Método para traer las categorías usando el ParametrosApiService
    private void cargarCategorias() {
        try {
            cmbCategoria.getItems().clear();
            cmbCategoria.getItems().addAll(parametrosService.obtenerCategorias());
        } catch (Exception e) {
            System.out.println("❌ No se pudieron cargar las categorías: " + e.getMessage());
        }
    }

    // 🔍 Método que le pregunta al Backend cuál es el último código para esa categoría
    private void buscarSkuSugerido(Long idCategoria) {
        try {
            HttpClient cliente = HttpClient.newHttpClient();
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/produccion/siguiente-sku/" + idCategoria))
                    .GET()
                    .build();

            HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());

            // Si el backend nos da un código real (ej: 22220004), pisa al 2222-0001
            if (respuesta.statusCode() == 200 && respuesta.body() != null && !respuesta.body().isBlank()) {
                txtCodigoSKU.setText(respuesta.body());
                txtCodigoSKU.positionCaret(txtCodigoSKU.getText().length());
            }
        } catch (Exception e) {
            System.out.println("❌ Error al sugerir SKU: " + e.getMessage());
        }
    }

    public void inicializarDatos(String nombre, double costo, double precio, Insumo materialGanador, ObservableList<DetalleCalculadora> insumos) {
        this.costoCalculado = costo;
        this.precioCalculado = precio;
        this.insumoDestacado = materialGanador;
        this.itemsReceta = insumos;

        txtNombre.setText(nombre);
        txtCosto.setText(String.format("%.2f", costo));
        txtPrecio.setText(String.format("%.2f", precio));

        // 🔥 EXTRAE EL NOMBRE DEL MATERIAL PARA MOSTRAR EN PANTALLA
        if (materialGanador != null && materialGanador.getMaterial() != null) {
            txtMaterialDestacado.setText(materialGanador.getMaterial().getNombre());
        } else {
            txtMaterialDestacado.setText("Varios / Sin Material Principal");
        }
    }

    @FXML
    public void confirmarGuardado(ActionEvent event) {
        if (cmbCategoria.getValue() == null) {
            mostrarError("Debe seleccionar una Categoría.");
            return;
        }
        if (txtCodigoSKU.getText() == null || txtCodigoSKU.getText().isBlank()) {
            mostrarError("El código SKU es obligatorio.");
            return;
        }

        try {
            // 1. Preparamos la lista de insumos en formato simplificado (DTO del frontend)
            java.util.List<com.nakel.frontend.model.ItemReceta> listaDTO = new java.util.ArrayList<>();
            for (DetalleCalculadora det : itemsReceta) {
                listaDTO.add(new com.nakel.frontend.model.ItemReceta(
                        det.getInsumo().getId(),
                        BigDecimal.valueOf(det.getCantidadUsada())
                ));
            }

            // 2. Armamos el objeto Receta general
            Receta recetaFinal = new Receta();
            recetaFinal.setNombre(txtNombre.getText());
            recetaFinal.setCodigo(txtCodigoSKU.getText().trim());
            recetaFinal.setIdCategoria(cmbCategoria.getValue().getId());

            // 🔥 EXTRAE EL ID DEL MATERIAL PARA MANDAR AL BACKEND
            if (insumoDestacado != null && insumoDestacado.getMaterial() != null) {
                recetaFinal.setIdMaterial(insumoDestacado.getMaterial().getId());
            }

            recetaFinal.setOrigen("PRODUCCION_PROPIA");
            recetaFinal.setCosto(BigDecimal.valueOf(costoCalculado));
            recetaFinal.setPrecioVenta(BigDecimal.valueOf(precioCalculado));
            recetaFinal.setStock(Integer.parseInt(txtStock.getText().trim()));
            recetaFinal.setInsumosUsados(listaDTO);

            // 3. Mandamos el paquete al Backend a través del servicio
            produccionApiService.registrarFabricacion(recetaFinal);

            this.guardadoExitoso = true;
            cerrarModal(event);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("❌ Error de producción: " + e.getMessage());
        }
    }

    @FXML
    public void cerrarModal(ActionEvent event) {
        Stage stage = (Stage) txtNombre.getScene().getWindow();
        stage.close();
    }

    public boolean isGuardadoExitoso() {
        return guardadoExitoso;
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}