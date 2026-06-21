package com.nakel.frontend.model;

public class Pago {
    private String metodoPago;
    private Double monto;

    // Constructor para armar el pago en la pantalla
    public Pago(String metodoPago, Double monto) {
        this.metodoPago = metodoPago;
        this.monto = monto;
    }

    // GSON usa estos getters para crear el JSON que viaja al Back
    public String getMetodoPago() { return metodoPago; }
    public Double getMonto() { return monto; }
}