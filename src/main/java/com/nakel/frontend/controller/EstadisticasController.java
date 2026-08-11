package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.nakel.frontend.model.Articulo;
import com.nakel.frontend.model.DashboardDTO;
import com.nakel.frontend.service.ArticuloApiService;
import com.nakel.frontend.service.EstadisticasApiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class EstadisticasController {

    // 🏷️ Tarjeta 1: Ventas Hoy
    @FXML private Label lblVentasHoy;
    @FXML private Label lblCantidadHoy;

    // 🏷️ Tarjeta 2: Ventas Semana
    @FXML private Label lblVentasSemana;
    @FXML private Label lblCantidadSemana;

    // 🏷️ Tarjeta 3: Inventario
    @FXML private Label lblTotalProductos;

    // 🏷️ Tarjeta 4: Alertas
    @FXML private Label lblStockCritico;

    // ⚠️ Tabla de Stock Crítico
    @FXML private TableView<Articulo> tablaStockCritico;
    @FXML private TableColumn<Articulo, String> colCriticoCodigo;
    @FXML private TableColumn<Articulo, String> colCriticoNombre;
    @FXML private TableColumn<Articulo, Integer> colCriticoStock;

    // 🔌 Servicios
    private final EstadisticasApiService estadisticasApi = new EstadisticasApiService();
    private final ArticuloApiService articuloApi = new ArticuloApiService(); // Sumamos el servicio de artículos
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        // 1. Configuramos las columnas de la tabla primero
        colCriticoCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colCriticoNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCriticoStock.setCellValueFactory(new PropertyValueFactory<>("stockActual"));

        // 2. Apenas se abre la pantalla, vamos a buscar los datos
        cargarDatosDashboard();
    }

    public void cargarDatosDashboard() {
        try {
            // --- PARTE 1: LAS TARJETAS PRINCIPALES ---
            String json = estadisticasApi.obtenerDatosDashboard();

            if (json != null && !json.isBlank()) {
                DashboardDTO datos = gson.fromJson(json, DashboardDTO.class);

                lblVentasHoy.setText(String.format("$ %.2f", datos.getVentasHoy()));
                lblCantidadHoy.setText(datos.getCantidadVentasHoy() + " ventas");

                lblVentasSemana.setText(String.format("$ %.2f", datos.getVentasSemana()));
                lblCantidadSemana.setText(datos.getCantidadVentasSemana() + " ventas");

                lblTotalProductos.setText(String.valueOf(datos.getProductosActivos()));
            } else {
                System.out.println("⚠️ No se recibió JSON del servidor para el dashboard.");
            }

            // --- PARTE 2: LA TABLA Y EL NÚMERO CRÍTICO SINCRONIZADOS ---
            cargarTablaYNumeroCritico();

        } catch (Exception e) {
            System.out.println("❌ Error al pintar el dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cargarTablaYNumeroCritico() {
        try {
            // Traemos el catálogo completo (Asegurate de que el backend mande un lote grande, ej: size=1000)
            List<Articulo> catalogoCompleto = articuloApi.obtenerTodos();
            ObservableList<Articulo> itemsCriticos = FXCollections.observableArrayList();

            if (catalogoCompleto != null) {
                for (Articulo art : catalogoCompleto) {
                    if (art.getStockActual() < 10) {
                        itemsCriticos.add(art);
                    }
                }
            }

            // 1. Inyectamos los resultados en la grilla visual
            tablaStockCritico.setItems(itemsCriticos);

            // 2. Sincronizamos el número de la tarjeta EXACTAMENTE con la cantidad que muestra la tabla
            int totalCritico = itemsCriticos.size();
            lblStockCritico.setText(String.valueOf(totalCritico));

            // 3. Estilos dinámicos: Rojo si hay urgencias, Verde si todo está ok
            if (totalCritico > 0) {
                lblStockCritico.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold; -fx-font-size: 24px;");
            } else {
                lblStockCritico.setStyle("-fx-text-fill: #388e3c; -fx-font-weight: bold; -fx-font-size: 24px;");
            }

        } catch (Exception e) {
            System.out.println("❌ Error al cargar la tabla de stock crítico: " + e.getMessage());
            e.printStackTrace();
        }
    }
}