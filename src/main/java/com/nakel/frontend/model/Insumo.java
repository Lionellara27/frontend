package com.nakel.frontend.model;

import java.math.BigDecimal;

public class Insumo {
    private Long id;
    private String nombre;

    private Categoria categoria;
    private BigDecimal costoTotal;

    // ==========================================
    // 📦 CAMPOS NUEVOS: UNIDAD
    // ==========================================
    private Integer cantidadLote;
    private Integer cantidadActual;

    // ==========================================
    // 📏 CAMPOS NUEVOS: SUPERFICIE
    // ==========================================
    private Integer anchoLoteCm;
    private Integer largoLoteCm;
    private Integer areaActualCm2;

    public Insumo() {}

    // ==========================================
    // GETTERS Y SETTERS BÁSICOS
    // ==========================================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public BigDecimal getCostoTotal() { return costoTotal; }
    public void setCostoTotal(BigDecimal costoTotal) { this.costoTotal = costoTotal; }

    // ==========================================
    // GETTERS Y SETTERS NUEVOS
    // ==========================================
    public Integer getCantidadLote() { return cantidadLote; }
    public void setCantidadLote(Integer cantidadLote) { this.cantidadLote = cantidadLote; }

    public Integer getCantidadActual() { return cantidadActual; }
    public void setCantidadActual(Integer cantidadActual) { this.cantidadActual = cantidadActual; }

    public Integer getAnchoLoteCm() { return anchoLoteCm; }
    public void setAnchoLoteCm(Integer anchoLoteCm) { this.anchoLoteCm = anchoLoteCm; }

    public Integer getLargoLoteCm() { return largoLoteCm; }
    public void setLargoLoteCm(Integer largoLoteCm) { this.largoLoteCm = largoLoteCm; }

    public Integer getAreaActualCm2() { return areaActualCm2; }
    public void setAreaActualCm2(Integer areaActualCm2) { this.areaActualCm2 = areaActualCm2; }

    // ==========================================
    // 🔥 MOTORES DE CÁLCULO (Usan el LOTE comprado)
    // ==========================================
    public BigDecimal getCostoPorCm2() {
        if (anchoLoteCm != null && largoLoteCm != null && anchoLoteCm > 0 && largoLoteCm > 0 && costoTotal != null) {
            BigDecimal areaTotal = new BigDecimal(anchoLoteCm * largoLoteCm);
            return costoTotal.divide(areaTotal, 4, java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getCostoPorUnidad() {
        if (cantidadLote != null && cantidadLote > 0 && costoTotal != null) {
            return costoTotal.divide(new BigDecimal(cantidadLote), 4, java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    // ==========================================
    // 🏷️ MAGIA VISUAL (Usa lo ACTUAL para mostrar qué queda)
    // ==========================================
   // public String getDescripcionCompleta() {
     //   if (categoria != null && "SUPERFICIE".equals(categoria.getTipoMedicion())) {
       //     return nombre + " (Quedan: " + (areaActualCm2 != null ? areaActualCm2 : 0) + " cm²)";
        //} else if (categoria != null && "UNIDAD".equals(categoria.getTipoMedicion())) {
          //  return nombre + " (Quedan: " + (cantidadActual != null ? cantidadActual : 0) + " u.)";
        //}
        //return nombre;
    //}

    public String getDescripcionCompleta() {
        if (categoria != null) {
            if ("SUPERFICIE".equals(categoria.getTipoMedicion())) {
                return nombre + " (Quedan: " + (areaActualCm2 != null ? areaActualCm2 : 0) + " cm²)";
            } else if ("UNIDAD".equals(categoria.getTipoMedicion())) {
                return nombre + " (Quedan: " + (cantidadActual != null ? cantidadActual : 0) + " u.)";
            } else if ("SERVICIO".equals(categoria.getTipoMedicion())) {
                // 🔥 MAGIA: Si es servicio/tiempo, no mostramos stock, mostramos el precio x hora.
                return nombre + " (Valor: $" + getCostoTotal() + "/hr)";
            }
        }
        return nombre;
    }


    // ==========================================
    // 🔥 EL CEREBRO DE LA TABLA (NUEVO)
    // ==========================================
    public BigDecimal getCostoCalculado() {
        if (categoria != null) {
            String tipo = categoria.getTipoMedicion();
            if ("SUPERFICIE".equals(tipo)) {
                return getCostoPorCm2();
            } else if ("UNIDAD".equals(tipo)) {
                return getCostoPorUnidad();
            }
        }
        return costoTotal; // Si es Servicio/Tiempo, el costo es el total (la hora)
    }

    @Override
    public String toString() {
        return getDescripcionCompleta();
    }
}