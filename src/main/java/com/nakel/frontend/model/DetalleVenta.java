package com.nakel.frontend.model;

public class DetalleVenta {
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;
    private Articulo articulo; // Usamos la clase Articulo que ya tenés en el front

    public DetalleVenta(Integer cantidad, Double precioUnitario, Double subtotal, Articulo articulo) {
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = subtotal;
        this.articulo = articulo;
    }

    // Getters para que GSON pueda leer los datos
    public Integer getCantidad() { return cantidad; }
    public Double getPrecioUnitario() { return precioUnitario; }
    public Double getSubtotal() { return subtotal; }
    public Articulo getArticulo() { return articulo; }
}