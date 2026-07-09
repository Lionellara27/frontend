package com.nakel.frontend.model;

public class Categoria {
    private Long id;
    private String nombre;

    // 🔥 El campo clave que le avisa al formulario cómo comportarse
    private String tipoMedicion;

    // Constructor vacío (Siempre útil para que Gson arme los objetos desde el JSON)
    public Categoria() {}

    // Constructor completo (Para cargar los datos de la base de datos)
    public Categoria(Long id, String nombre, String tipoMedicion) {
        this.id = id;
        this.nombre = nombre;
        this.tipoMedicion = tipoMedicion;
    }

    // --- GETTERS ---
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTipoMedicion() { return tipoMedicion; }

    // --- SETTERS ---
    public void setId(Long id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setTipoMedicion(String tipoMedicion) { this.tipoMedicion = tipoMedicion; }

    @Override
    public String toString() { return nombre; }
}