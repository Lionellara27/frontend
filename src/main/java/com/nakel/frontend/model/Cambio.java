package com.nakel.frontend.model;

import java.util.List; // 🔥 1. AGREGÁ ESTE IMPORT

public class Cambio {

    private Long id;
    private String fechaCambio;
    private Double diferenciaCobrada;
    private String metodoPago;
    private String codigoValeGenerado;
    private String resumenArticulos;

    // 🔥 2. AGREGÁ EL ATRIBUTO DE LA LISTA
    private List<ItemCambio> items;

    public Cambio() {
    }

    // Getters y Setters que ya tenías...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFechaCambio() { return fechaCambio; }
    public void setFechaCambio(String fechaCambio) { this.fechaCambio = fechaCambio; }

    public Double getDiferenciaCobrada() { return diferenciaCobrada; }
    public void setDiferenciaCobrada(Double diferenciaCobrada) { this.diferenciaCobrada = diferenciaCobrada; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getCodigoValeGenerado() { return codigoValeGenerado; }
    public void setCodigoValeGenerado(String codigoValeGenerado) { this.codigoValeGenerado = codigoValeGenerado; }

    public String getResumenArticulos() { return resumenArticulos; }
    public void setResumenArticulos(String resumenArticulos) { this.resumenArticulos = resumenArticulos; }

    // 🔥 3. AGREGÁ ESTOS DOS MÉTODOS AL FINAL PARA QUE DESAPAREZCA EL ERROR
    public List<ItemCambio> getItems() {
        return items;
    }

    public void setItems(List<ItemCambio> items) {
        this.items = items;
    }
}