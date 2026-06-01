package com.nakel.frontend.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

public class Navegador {

    // Acá guardamos la referencia al centro de tu pantalla principal
    private static BorderPane panelCentral;

    // Se llama una sola vez al arrancar el programa
    public static void setPanelCentral(BorderPane panel) {
        panelCentral = panel;
    }

    // El método que van a usar todos los botones del sistema
    public static void cargarVista(String rutaFxml) {
        try {
            Parent vista = FXMLLoader.load(Navegador.class.getResource(rutaFxml));

            if (panelCentral != null) {
                panelCentral.setCenter(vista);
            } else {
                System.err.println("🔴 Error: El panel central del Navegador no está configurado.");
            }
        } catch (Exception e) {
            System.err.println("🔴 Error al intentar navegar a la ruta: " + rutaFxml);
            e.printStackTrace();
        }
    }
}