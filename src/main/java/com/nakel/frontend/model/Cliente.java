package com.nakel.frontend.model;

// ⚠️ Esta clase solo vive en el Frontend para recibir los datos de la API.
// No lleva anotaciones de base de datos (@Entity, @Table, etc.)
public class Cliente {

    private Long id;
    private String nombre;
    private String cuit;
    private String condicionIva;
    private String telefono;
    private String email;

    // --- GETTERS (Mínimos necesarios para el mostrador) ---
    public String getNombre() {
        return nombre;
    }

    public String getCuit() {
        return cuit;
    }

    public Long getId() {
        return id;
    }

    // Podés generarle los setters y el resto de los getters con tu IDE si querés
}