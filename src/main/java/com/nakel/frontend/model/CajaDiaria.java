package com.nakel.frontend.model;

import java.math.BigDecimal;

public class CajaDiaria {
    private Long id;
    private String fechaApertura;
    private String fechaCierre;

    // 🔥 Nuevos campos de Usuario (Asumiendo que tenés la clase Usuario en tu front)
    private Usuario usuarioApertura;
    private Usuario usuarioCierre;

    private BigDecimal saldoInicial;
    private BigDecimal totalEfectivo;
    private BigDecimal totalMercadoPago;
    private BigDecimal totalTransferencias;
    private BigDecimal totalVentas;

    // 🔥 Nuevo campo de Egresos
    private BigDecimal totalEgresos;

    private BigDecimal saldoFinal;
    private Integer cantidadVentas;
    private String estado;

    // 🔥 Nuevo campo de Observaciones
    private String observaciones;

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(String fechaApertura) { this.fechaApertura = fechaApertura; }

    public String getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(String fechaCierre) { this.fechaCierre = fechaCierre; }

    public Usuario getUsuarioApertura() { return usuarioApertura; }
    public void setUsuarioApertura(Usuario usuarioApertura) { this.usuarioApertura = usuarioApertura; }

    public Usuario getUsuarioCierre() { return usuarioCierre; }
    public void setUsuarioCierre(Usuario usuarioCierre) { this.usuarioCierre = usuarioCierre; }

    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(BigDecimal saldoInicial) { this.saldoInicial = saldoInicial; }

    public BigDecimal getTotalEfectivo() { return totalEfectivo; }
    public void setTotalEfectivo(BigDecimal totalEfectivo) { this.totalEfectivo = totalEfectivo; }

    public BigDecimal getTotalMercadoPago() { return totalMercadoPago; }
    public void setTotalMercadoPago(BigDecimal totalMercadoPago) { this.totalMercadoPago = totalMercadoPago; }

    public BigDecimal getTotalTransferencias() { return totalTransferencias; }
    public void setTotalTransferencias(BigDecimal totalTransferencias) { this.totalTransferencias = totalTransferencias; }

    public BigDecimal getTotalVentas() { return totalVentas; }
    public void setTotalVentas(BigDecimal totalVentas) { this.totalVentas = totalVentas; }

    public BigDecimal getTotalEgresos() { return totalEgresos; }
    public void setTotalEgresos(BigDecimal totalEgresos) { this.totalEgresos = totalEgresos; }

    public BigDecimal getSaldoFinal() { return saldoFinal; }
    public void setSaldoFinal(BigDecimal saldoFinal) { this.saldoFinal = saldoFinal; }

    public Integer getCantidadVentas() { return cantidadVentas; }
    public void setCantidadVentas(Integer cantidadVentas) { this.cantidadVentas = cantidadVentas; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}