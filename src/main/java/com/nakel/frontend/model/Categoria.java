package com.nakel.frontend.model;

public class Categoria {
    private Long id;
    private String nombre;
    private String tipoMedicion; // Para la Calculadora (Insumos)
    private String prefijoSku;   // NUEVO: Para el Catálogo (Artículos)

    public Categoria() {}

    public Categoria(Long id, String nombre, String tipoMedicion, String prefijoSku) {
        this.id = id;
        this.nombre = nombre;
        this.tipoMedicion = tipoMedicion;
        this.prefijoSku = prefijoSku;
    }

    // --- GETTERS ---
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTipoMedicion() { return tipoMedicion; }
    public String getPrefijoSku() { return prefijoSku; }

    // --- SETTERS ---
    public void setId(Long id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setTipoMedicion(String tipoMedicion) { this.tipoMedicion = tipoMedicion; }
    public void setPrefijoSku(String prefijoSku) { this.prefijoSku = prefijoSku; }

    @Override
    public String toString() { return nombre; }


}