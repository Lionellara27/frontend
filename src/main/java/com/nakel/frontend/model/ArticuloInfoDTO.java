package com.nakel.frontend.model;

public class ArticuloInfoDTO {
    private Articulo articulo;
    private int cantidad;
    private double precioUnitario;

    public ArticuloInfoDTO() {
    }

    public ArticuloInfoDTO(Articulo articulo, int cantidad, double precioUnitario) {
        this.articulo = articulo;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public Articulo getArticulo() {
        return articulo;
    }

    public void setArticulo(Articulo articulo) {
        this.articulo = articulo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }
}