package com.nakel.frontend.model;

public class Vale {

    private Long id;
    private String codigo;
    private Double monto;
    private String fechaEmision;
    private String fechaVencimiento;
    private String estado;

    // 🔥 Agregamos el ID del cliente para que coincida con el backend
    private Long idCliente;

    // Constructor vacío para Gson
    public Vale() {
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public String getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(String fechaEmision) { this.fechaEmision = fechaEmision; }

    public String getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(String fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    // 🔥 Getters y Setters del nuevo campo
    public Long getIdCliente() { return idCliente; }
    public void setIdCliente(Long idCliente) { this.idCliente = idCliente; }
}