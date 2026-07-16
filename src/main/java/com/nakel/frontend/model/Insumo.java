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

    // 🔥 CALCULA CUÁNTO VALE 1 CM CUADRADO DE ESTA TELA
    public BigDecimal getCostoPorCm2() {
        if (anchoCm != null && largoCm != null && anchoCm > 0 && largoCm > 0 && costoTotal != null) {
            BigDecimal areaTotal = new BigDecimal(anchoCm * largoCm);
            // Usamos RoundingMode.HALF_UP para redondear correctamente los centavos
            return costoTotal.divide(areaTotal, 4, java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    // 🔥 CALCULA CUÁNTO VALE 1 SOLA UNIDAD (EJ: 1 CIERRE O 1 HORA)
    public BigDecimal getCostoPorUnidad() {
        if (cantidad != null && cantidad > 0 && costoTotal != null) {
            return costoTotal.divide(new BigDecimal(cantidad), 4, java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }


    @Override
    public String toString() {
        return getDescripcionCompleta();
    }
}