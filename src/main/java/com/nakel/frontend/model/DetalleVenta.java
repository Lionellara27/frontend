package com.nakel.frontend.model;

public class DetalleVenta {
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;
    private Articulo articulo;
    //parte new
    private int cantidadOriginal;

    // 🔥 AGREGADO: Constructor vacío (Gson lo necesita para no tirar error)
    public DetalleVenta() {
    }

    // ✅ MANTENIDO: Tu constructor original (Nada de lo que ya hiciste se rompe)
    public DetalleVenta(Integer cantidad, Double precioUnitario, Double subtotal, Articulo articulo) {
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = subtotal;
        this.articulo = articulo;
    }

    // ✅ MANTENIDO: Tus Getters originales
    public Integer getCantidad() { return cantidad; }
    public Double getPrecioUnitario() { return precioUnitario; }
    public Double getSubtotal() { return subtotal; }
    public Articulo getArticulo() { return articulo; }

    // 🔥 AGREGADO: Setters para poder inyectar datos paso a paso en el controlador
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public void setPrecioUnitario(Double precioUnitario) { this.precioUnitario = precioUnitario; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    public void setArticulo(Articulo articulo) { this.articulo = articulo; }


    //parte new
    public int getCantidadOriginal() {
        return cantidadOriginal;
    }

    public void setCantidadOriginal(int cantidadOriginal) {
        this.cantidadOriginal = cantidadOriginal;
    }
}