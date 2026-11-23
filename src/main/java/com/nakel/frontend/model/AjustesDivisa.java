package com.nakel.frontend.model;

public class AjustesDivisa {
    // Cotización del dólar cargada hoy
    private Double cotizacionUsd;
    private boolean modoAutomatico;
    // oficial, blue, tarjeta o bolsa
    private String tipoDolar;
    // Con qué dólar están calculados los precios del catálogo (lo maneja el backend)
    private Double cotizacionBasePrecios;
    // De qué tipo y de qué día ("23/11/2026") es ese dólar
    private String tipoDolarBase;
    private String fechaBasePrecios;

    public AjustesDivisa() {}

    public String getTipoDolarBase() { return tipoDolarBase; }
    public void setTipoDolarBase(String tipoDolarBase) { this.tipoDolarBase = tipoDolarBase; }

    public String getFechaBasePrecios() { return fechaBasePrecios; }
    public void setFechaBasePrecios(String fechaBasePrecios) { this.fechaBasePrecios = fechaBasePrecios; }

    public AjustesDivisa(Double cotizacionUsd, boolean modoAutomatico) {
        this.cotizacionUsd = cotizacionUsd;
        this.modoAutomatico = modoAutomatico;
    }

    public Double getCotizacionUsd() { return cotizacionUsd; }
    public void setCotizacionUsd(Double cotizacionUsd) { this.cotizacionUsd = cotizacionUsd; }

    public boolean isModoAutomatico() { return modoAutomatico; }
    public void setModoAutomatico(boolean modoAutomatico) { this.modoAutomatico = modoAutomatico; }

    public String getTipoDolar() { return tipoDolar; }
    public void setTipoDolar(String tipoDolar) { this.tipoDolar = tipoDolar; }

    public Double getCotizacionBasePrecios() { return cotizacionBasePrecios; }
    public void setCotizacionBasePrecios(Double cotizacionBasePrecios) { this.cotizacionBasePrecios = cotizacionBasePrecios; }
}
