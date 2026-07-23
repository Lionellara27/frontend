package com.nakel.frontend.model;

import java.math.BigDecimal;
import java.util.List;

public class Receta {
    private String nombre;
    private String codigo;
    private Long idCategoria;
    private Long idMaterial;
    private String origen;
    private BigDecimal costo;
    private BigDecimal precioVenta;
    private int stock;
    private List<ItemReceta> insumosUsados;

    // Getters y Setters
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public Long getIdCategoria() { return idCategoria; }
    public void setIdCategoria(Long idCategoria) { this.idCategoria = idCategoria; }

    public Long getIdMaterial() { return idMaterial; }
    public void setIdMaterial(Long idMaterial) { this.idMaterial = idMaterial; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    public BigDecimal getCosto() { return costo; }
    public void setCosto(BigDecimal costo) { this.costo = costo; }

    public BigDecimal getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(BigDecimal precioVenta) { this.precioVenta = precioVenta; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public List<ItemReceta> getInsumosUsados() { return insumosUsados; }
    public void setInsumosUsados(List<ItemReceta> insumosUsados) { this.insumosUsados = insumosUsados; }
}