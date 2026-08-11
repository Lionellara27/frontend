package com.nakel.frontend.model;

public class ItemCambio {

    private Long id;
    private Articulo articulo;
    private int cantidad;
    private double precioUnitario;
    private String tipo; // Acá viaja el "DEVUELTO" o "NUEVO"

    // Nota: No hace falta poner el atributo "Cambio cambio" en el front,
    // porque el backend se encarga de enlazarlo automáticamente cuando lo recibe.

    public ItemCambio() {
    }

    public ItemCambio(Long id, Articulo articulo, int cantidad, double precioUnitario, String tipo) {
        this.id = id;
        this.articulo = articulo;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.tipo = tipo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}