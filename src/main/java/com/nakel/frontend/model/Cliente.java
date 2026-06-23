package com.nakel.frontend.model;

public class Cliente {

    private Long id;
    private String nombre;
    private String cuit;
    private String condicionIva;
    private String telefono;
    private String email;

    // Constructores vacíos para que Gson pueda trabajar tranquilo
    public Cliente() {
    }

    // --- GETTERS (Lo que JavaFX necesita para leer los datos y pintar la tabla) ---
    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCuit() {
        return cuit;
    }

    public String getCondicionIva() {
        return condicionIva;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    // --- SETTERS (Por si en el futuro necesitas editar al cliente en la tabla) ---
    public void setId(Long id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setCuit(String cuit) {
        this.cuit = cuit;
    }

    public void setCondicionIva(String condicionIva) {
        this.condicionIva = condicionIva;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}