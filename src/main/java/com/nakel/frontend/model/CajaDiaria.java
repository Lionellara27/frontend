package com.nakel.frontend.model;

import java.math.BigDecimal;

public class CajaDiaria {
    private Long id;
    private String fechaApertura;
    private String fechaCierre;
    private BigDecimal saldoInicial;
    private BigDecimal totalEfectivo;
    private BigDecimal totalMercadoPago;
    private BigDecimal totalTransferencias;
    private BigDecimal totalVentas;
    private BigDecimal saldoFinal;
    private Integer cantidadVentas;
    private String estado;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(String fechaApertura) { this.fechaApertura = fechaApertura; }

    public String getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(String fechaCierre) { this.fechaCierre = fechaCierre; }

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

    public BigDecimal getSaldoFinal() { return saldoFinal; }
    public void setSaldoFinal(BigDecimal saldoFinal) { this.saldoFinal = saldoFinal; }

    public Integer getCantidadVentas() { return cantidadVentas; }
    public void setCantidadVentas(Integer cantidadVentas) { this.cantidadVentas = cantidadVentas; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}