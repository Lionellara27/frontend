package com.nakel.frontend.model;

import java.math.BigDecimal;

public class MovimientoCaja {

    private Long id;
    private CajaDiaria caja;
    private Usuario usuario; // Asumo que ya tenés la clase Usuario en el front, si no, avisame
    private String fechaHora; // Usamos String para que Gson lo mapee fácil
    private TipoMovimientoCaja tipo; // Usamos tu Enum
    private String concepto;
    private String descripcion;
    private String medioPago;
    private BigDecimal monto;

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CajaDiaria getCaja() { return caja; }
    public void setCaja(CajaDiaria caja) { this.caja = caja; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }

    public TipoMovimientoCaja getTipo() { return tipo; }
    public void setTipo(TipoMovimientoCaja tipo) { this.tipo = tipo; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getMedioPago() { return medioPago; }
    public void setMedioPago(String medioPago) { this.medioPago = medioPago; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
}