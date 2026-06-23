package com.nakel.frontend.model;

public class LineaTicket {

    private Articulo articulo;
    private int cantidad;

    // Constructor: cuando agregamos algo al ticket, por defecto es 1 unidad
    public LineaTicket(Articulo articulo) {
        this.articulo = articulo;
        this.cantidad = 1;
    }

    // --- GETTERS Y SETTERS ---
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

    // 🔥 EL TRUCO DE MAGIA: El subtotal se calcula solo
    public double getSubtotal() {
        if (articulo != null && articulo.getPrecio() != null) {
            return articulo.getPrecio() * cantidad;
        }
        return 0.0;
    }

    // Método útil para cuando hacen doble clic repetido
    public void sumarCantidad() {
        this.cantidad++;
    }
}