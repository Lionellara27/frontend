package com.nakel.frontend.model;

public class DetalleCalculadora {

    private Insumo insumo;
    private double cantidadUsada;
    private String tipoUso;
    private double subtotal;
    private Integer anchoUsado;
    private Integer largoUsado;

    public DetalleCalculadora(Insumo insumo, double cantidadUsada, String tipoUso, double subtotal, Integer anchoUsado, Integer largoUsado) {
        this.insumo = insumo;
        this.cantidadUsada = cantidadUsada;
        this.tipoUso = tipoUso;
        this.subtotal = subtotal;
        this.anchoUsado = anchoUsado;
        this.largoUsado = largoUsado;
    }

    public String getNombreInsumo() {
        return insumo != null ? insumo.getNombre() : "Desconocido";
    }

    public String getDescripcionUso() {
        if (anchoUsado != null && largoUsado != null) return "Corte: " + anchoUsado + "x" + largoUsado + " cm";
        return cantidadUsada + " Unid/Hs";
    }

    public String getSubtotalFormateado() {
        return String.format("$ %.2f", subtotal);
    }

    public double getSubtotal() { return subtotal; }
}