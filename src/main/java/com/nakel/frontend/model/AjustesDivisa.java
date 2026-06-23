package com.nakel.frontend.model;

public class AjustesDivisa {
    private Double cotizacionUsd;
    private boolean modoAutomatico;

    public AjustesDivisa() {}

    public AjustesDivisa(Double cotizacionUsd, boolean modoAutomatico) {
        this.cotizacionUsd = cotizacionUsd;
        this.modoAutomatico = modoAutomatico;
    }

    public Double getCotizacionUsd() { return cotizacionUsd; }
    public void setCotizacionUsd(Double cotizacionUsd) { this.cotizacionUsd = cotizacionUsd; }

    public boolean isModoAutomatico() { return modoAutomatico; }
    public void setModoAutomatico(boolean modoAutomatico) { this.modoAutomatico = modoAutomatico; }
}