package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.nakel.frontend.model.Venta;
import com.nakel.frontend.service.VentaApiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.lang.reflect.Type;
import java.util.List;

public class HistorialVentasController {

    @FXML private ComboBox<String> cmbMes;
    @FXML private TextField txtBuscarVenta;
    @FXML private Label lblTotalFacturado;

    // La tabla y sus columnas (conectadas al FXML)
    @FXML private TableView<Venta> tablaVentas;
    @FXML private TableColumn<Venta, String> colFecha;
    @FXML private TableColumn<Venta, String> colNro;
    @FXML private TableColumn<Venta, String> colCliente;
    @FXML private TableColumn<Venta, Double> colTotal;
    @FXML private TableColumn<Venta, String> colEstado;
    @FXML private TableColumn<Venta, Void> colAcciones;

    private final VentaApiService apiService = new VentaApiService();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        System.out.println("Módulo de Historial de Ventas listo.");
        configurarColumnas();
        cargarVentas();
    }

    private void configurarColumnas() {
        // 1. Datos directos
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        // 2. Datos procesados (Magia de JavaFX para adaptar el modelo a la vista)

        // 🔥 ACÁ ESTÁ EL CAMBIO: Evaluamos si hay cliente y sacamos su nombre
        colCliente.setCellValueFactory(cell -> {
            if (cell.getValue().getCliente() != null) {
                return new SimpleStringProperty(cell.getValue().getCliente().getNombre());
            } else {
                return new SimpleStringProperty("Consumidor Final");
            }
        });

        // Simulamos un número de comprobante con el ID de la base de datos
        colNro.setCellValueFactory(cell -> new SimpleStringProperty("0001 - " + String.format("%08d", cell.getValue().getId())));

        // Lógica de AFIP (Si esFiscal es true, mostramos que hay que facturar)
        colEstado.setCellValueFactory(cell -> {
            boolean esFiscal = cell.getValue().getEsFiscal() != null && cell.getValue().getEsFiscal();
            return new SimpleStringProperty(esFiscal ? "A Facturar (AFIP)" : "No Declarado");
        });

        // Parseamos la fecha
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));

        // 3. (A futuro) Acá podés meter tu CellFactory con FontAwesome para los botones de Imprimir/Ver
    }

    private void cargarVentas() {
        String json = apiService.obtenerHistorialVentas();

        if (json != null && !json.equals("[]") && !json.isEmpty()) {
            try {
                // Leemos el objeto paginado del backend
                JsonObject respuestaServidor = JsonParser.parseString(json).getAsJsonObject();
                JsonArray arregloVentas = respuestaServidor.getAsJsonArray("content");

                // Convertimos a Java
                Type tipoLista = new TypeToken<List<Venta>>(){}.getType();
                List<Venta> listaVentas = gson.fromJson(arregloVentas, tipoLista);

                // Llenamos la tabla
                ObservableList<Venta> datosObservable = FXCollections.observableArrayList(listaVentas);
                tablaVentas.setItems(datosObservable);

                // Actualizamos la etiqueta del total facturado
                calcularTotalPantalla(listaVentas);

                System.out.println("✅ Historial cargado con " + listaVentas.size() + " ventas.");
            } catch (Exception e) {
                System.out.println("❌ Error al cargar historial: " + e.getMessage());
            }
        }
    }

    private void calcularTotalPantalla(List<Venta> ventas) {
        double suma = ventas.stream().mapToDouble(Venta::getTotal).sum();
        lblTotalFacturado.setText("$ " + String.format("%.2f", suma));
    }

    @FXML
    public void abrirPuntoDeVenta(ActionEvent event) {
        System.out.println("🚀 Saltando al Mostrador usando el Router...");
        // Acá llamás a tu clase Navegador/Router como tenías pensado
        com.nakel.frontend.util.Navegador.cargarVista("/com/nakel/frontend/view/venta-view.fxml");
    }
}