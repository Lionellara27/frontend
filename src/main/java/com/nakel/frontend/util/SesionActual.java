package com.nakel.frontend.util;

public class SesionActual {
    // Como por defecto arranca con el admin del backend, lo dejamos inicializado en "admin"
    private static String usuarioLogueado = "admin";

    public static String getUsuarioLogueado() {
        return usuarioLogueado;
    }

    public static void setUsuarioLogueado(String usuario) {
        SesionActual.usuarioLogueado = usuario;
    }
}