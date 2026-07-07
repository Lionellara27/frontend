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
    private BigDecimal saldoPendiente;

    // Constructores
    public Proveedor() {
        this.saldoPendiente = BigDecimal.ZERO;
    }

    // Getters y Setters (Podés generarlos con IntelliJ apretando Alt + Insert,
    // o usar @Data de Lombok si lo tenés configurado en el front)

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

    public BigDecimal getSaldoPendiente() { return saldoPendiente; }
    public void setSaldoPendiente(BigDecimal saldoPendiente) { this.saldoPendiente = saldoPendiente; }
}