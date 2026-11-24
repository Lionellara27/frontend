package com.nakel.frontend;

public class Launcher {
    public static void main(String[] args) {
        // Lo primero: poner a arrancar el motor del sistema. Mientras tanto se va preparando la ventana,
        // así las dos cosas avanzan a la vez en lugar de una después de la otra.
        com.nakel.frontend.util.ServidorLocal.lanzar();

        // Este puente llama a tu clase principal original sin que JavaFX explote
        MainApp.main(args);
    }
}