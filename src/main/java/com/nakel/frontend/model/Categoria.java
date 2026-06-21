package com.nakel.frontend.model;

public class Categoria {
    private Long id;
    private String nombre;

    public Categoria() {}

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setId(Long id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    @Override
    public String toString() { return nombre; }
}