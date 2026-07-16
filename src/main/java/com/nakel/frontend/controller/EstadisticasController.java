package com.nakel.frontend.controller;

import com.google.gson.Gson;
import com.nakel.frontend.model.DashboardDTO;
import com.nakel.frontend.service.EstadisticasApiService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

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

    // 🔌 Servicios
    private final EstadisticasApiService estadisticasApi = new EstadisticasApiService();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        // Apenas se abre la pantalla, vamos a buscar los datos
        cargarDatosDashboard();
    }

    public void cargarDatosDashboard() {
        try {
            // 1. Le pegamos al backend
            String json = estadisticasApi.obtenerDatosDashboard();

            if (json != null && !json.isBlank()) {
                // 2. Abrimos la "cajita"
                DashboardDTO datos = gson.fromJson(json, DashboardDTO.class);

                // 3. Pintamos los números en la pantalla
                lblVentasHoy.setText(String.format("$ %.2f", datos.getVentasHoy()));
                lblCantidadHoy.setText(datos.getCantidadVentasHoy() + " ventas");

                lblVentasSemana.setText(String.format("$ %.2f", datos.getVentasSemana()));
                lblCantidadSemana.setText(datos.getCantidadVentasSemana() + " ventas");

                lblTotalProductos.setText(String.valueOf(datos.getProductosActivos()));
                lblStockCritico.setText(String.valueOf(datos.getStockCritico()));

                // 🔥 Toque de magia: Si hay stock crítico, el número se pone ROJO y grande
                if (datos.getStockCritico() > 0) {
                    lblStockCritico.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold; -fx-font-size: 24px;");
                } else {
                    lblStockCritico.setStyle("-fx-text-fill: #388e3c; -fx-font-weight: bold; -fx-font-size: 24px;"); // Verde si está todo ok
                }

            } else {
                System.out.println("⚠️ No se recibió JSON del servidor.");
            }
        } catch (Exception e) {
            System.out.println("❌ Error al pintar el dashboard: " + e.getMessage());
        }
    }
}