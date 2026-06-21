package com.nakel.frontend.model;

public class Articulo {

    private Long id; // Fundamental para poder Editar o Eliminar después
    private String codigo;
    private String nombre;
    private Double precio;
    private Integer stockActual; // Agregado para el catálogo
    private Double alicuotaIva;  // Agregado
    private String origen;       // Agregado para filtrar (Reventa/Propia)

    // Clases anidadas para recibir los objetos del JSON
    private Categoria categoria;
    private Material material;

    public Articulo() {}

    // --- GETTERS ---
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public Double getPrecio() { return precio; }
    public Integer getStockActual() { return stockActual; }
    public Double getAlicuotaIva() { return alicuotaIva; }
    public String getOrigen() { return origen; }
    public Categoria getCategoria() { return categoria; }
    public Material getMaterial() { return material; }

    // --- SETTERS ---
    public void setId(Long id) { this.id = id; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setPrecio(Double precio) { this.precio = precio; }
    public void setStockActual(Integer stockActual) { this.stockActual = stockActual; }
    public void setAlicuotaIva(Double alicuotaIva) { this.alicuotaIva = alicuotaIva; }
    public void setOrigen(String origen) { this.origen = origen; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public void setMaterial(Material material) { this.material = material; }

    // =========================================================
    // SUB-CLASES para mapear los objetos que vienen del Backend
    // =========================================================

    public static class Categoria {
        private Long id;
        private String nombre; // Asumo que en tu back se llama "nombre"

        public Categoria() {}
        public Long getId() { return id; }
        public String getNombre() { return nombre; }
        // Este toString es clave para que el ComboBox de JavaFX muestre el texto lindo
        @Override public String toString() { return nombre; }
    }

    public static class Material {
        private Long id;
        private String nombre; // Asumo que en tu back se llama "nombre"

        public Material() {}
        public Long getId() { return id; }
        public String getNombre() { return nombre; }
        @Override public String toString() { return nombre; }
    }
}