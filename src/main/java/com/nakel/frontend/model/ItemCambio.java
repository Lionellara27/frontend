package com.nakel.frontend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemCambio {

    private Long id;
    private Articulo articulo;
    private int cantidad;
    private double precioUnitario;
    private String tipo; // Acá viaja el "DEVUELTO" o "NUEVO"

    // Nota: No hace falta poner el atributo "Cambio cambio" en el front,
    // porque el backend se encarga de enlazarlo automáticamente cuando lo recibe.
}