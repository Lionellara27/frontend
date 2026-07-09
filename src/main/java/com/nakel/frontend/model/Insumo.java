package com.nakel.frontend.model;

import java.math.BigDecimal;

public class Insumo {
    private Long id;
    private String nombre;

    // 🔥 CAMBIO CLAVE: Ahora es el objeto Categoria (ya no es un String)
    private Categoria categoria;

    private BigDecimal costoTotal;
    private Integer anchoCm;
    private Integer largoCm;
    private Integer cantidad;

    public Insumo() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    // 🔥 ACÁ ESTÁN LOS MÉTODOS QUE TE FALTABAN
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public BigDecimal getCostoTotal() { return costoTotal; }
    public void setCostoTotal(BigDecimal costoTotal) { this.costoTotal = costoTotal; }

    public Integer getAnchoCm() { return anchoCm; }
    public void setAnchoCm(Integer anchoCm) { this.anchoCm = anchoCm; }

    public Integer getLargoCm() { return largoCm; }
    public void setLargoCm(Integer largoCm) { this.largoCm = largoCm; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    // Método calculado auxiliar para mostrar la descripción formateada
    public String getDescripcionCompleta() {
        if (categoria != null && "SUPERFICIE".equals(categoria.getTipoMedicion())) {
            return nombre + " (" + anchoCm + "x" + largoCm + " cm)";
        }
        return nombre;
    }
}