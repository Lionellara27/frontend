package com.nakel.frontend.model;

import java.math.BigDecimal;

public class ItemReceta {
    private Long idInsumo;
    private BigDecimal cantidadUsada;

    public ItemReceta(Long idInsumo, BigDecimal cantidadUsada) {
        this.idInsumo = idInsumo;
        this.cantidadUsada = cantidadUsada;
    }

    public Long getIdInsumo() {
        return idInsumo;
    }

    public void setIdInsumo(Long idInsumo) {
        this.idInsumo = idInsumo;
    }

    public BigDecimal getCantidadUsada() {
        return cantidadUsada;
    }

    public void setCantidadUsada(BigDecimal cantidadUsada) {
        this.cantidadUsada = cantidadUsada;
    }
}