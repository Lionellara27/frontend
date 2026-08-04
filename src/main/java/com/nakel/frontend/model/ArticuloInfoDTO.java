package com.nakel.frontend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloInfoDTO {
    private Articulo articulo;
    private int cantidad;
    private double precioUnitario;
}