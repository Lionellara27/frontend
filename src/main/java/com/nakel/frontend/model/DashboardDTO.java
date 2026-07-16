package com.nakel.frontend.model;

// Esta es la "Cajita" gemela a la del backend
public class DashboardDTO {
    private double ventasHoy;
    private int cantidadVentasHoy;
    private double ventasSemana;
    private int cantidadVentasSemana;
    private int productosActivos;
    private int stockCritico;

    // Getters para que el controlador pueda sacar los números
    public double getVentasHoy() { return ventasHoy; }
    public int getCantidadVentasHoy() { return cantidadVentasHoy; }
    public double getVentasSemana() { return ventasSemana; }
    public int getCantidadVentasSemana() { return cantidadVentasSemana; }
    public int getProductosActivos() { return productosActivos; }
    public int getStockCritico() { return stockCritico; }
}