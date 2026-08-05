package com.nakel.frontend.model;

import java.math.BigDecimal;

public class Proveedor {

    private Long id;
    private String razonSocial;
    private String nombreContacto;
    private String rubro;
    private String cuit;
    private String telefono;
    private String email;

    // 🔥 NUEVOS CAMPOS
    private BigDecimal saldoFavor;
    private BigDecimal saldoContra;
    private String comentarios;

    // Constructor
    public Proveedor() {
        this.saldoFavor = BigDecimal.ZERO;
        this.saldoContra = BigDecimal.ZERO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getNombreContacto() { return nombreContacto; }
    public void setNombreContacto(String nombreContacto) { this.nombreContacto = nombreContacto; }

    public String getRubro() { return rubro; }
    public void setRubro(String rubro) { this.rubro = rubro; }

    public String getCuit() { return cuit; }
    public void setCuit(String cuit) { this.cuit = cuit; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // 🔥 GETTERS Y SETTERS NUEVOS
    public BigDecimal getSaldoFavor() { return saldoFavor; }
    public void setSaldoFavor(BigDecimal saldoFavor) { this.saldoFavor = saldoFavor; }

    public BigDecimal getSaldoContra() { return saldoContra; }
    public void setSaldoContra(BigDecimal saldoContra) { this.saldoContra = saldoContra; }

    public String getComentarios() { return comentarios; }
    public void setComentarios(String comentarios) { this.comentarios = comentarios; }
}