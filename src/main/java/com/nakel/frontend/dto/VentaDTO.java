package com.nakel.frontend.dto;

import com.nakel.frontend.model.Articulo;
import java.util.List;

public class VentaDTO {
    private String cliente;
    private String medioPago;
    private Double total;
    private List<Articulo> items; // Los artículos que están en la tabla

    // Constructores, Getters y Setters
    public VentaDTO(String cliente, String medioPago, Double total, List<Articulo> items) {
        this.cliente = cliente;
        this.medioPago = medioPago;
        this.total = total;
        this.items = items;
    }
}