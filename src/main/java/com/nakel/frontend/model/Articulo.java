package com.nakel.frontend.model;

public class Articulo {

    private Long id;
    private String codigo;
    private String nombre;
    private Double precio;
    private Integer stockActual;
    private Double alicuotaIva;
    private String origen;

    // Para hacer calculos
    private transient Integer cantidad = 1;

    // 🔥 Ahora usa las clases independientes que creaste (Categoria.java y Material.java)
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

    // --- GETTERS Y SETTERS PARA LA CANTIDAD Y HACER CALCULOS ---
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    // 🗑️ ¡ACÁ ESTABAN LAS CLASES ANIDADAS QUE BORRAMOS!
}