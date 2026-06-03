package com.nakel.frontend.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.net.URL;

public class Navegador {

    // Ahora usa StackPane, igual que tu MainController
    private static StackPane panelCentral;

    public static void setPanelCentral(StackPane panel) {
        panelCentral = panel;
    }

    public static void cargarVista(String rutaFxml) {
        try {
            URL archivoUrl = Navegador.class.getResource(rutaFxml);
            if (archivoUrl == null) {
                System.err.println("🔴 Error: No se encontró la vista en: " + rutaFxml);
                return;
            }

            Pane nuevaVista = FXMLLoader.load(archivoUrl);

            if (panelCentral != null) {
                // Borra lo viejo y pone lo nuevo (La magia del StackPane)
                panelCentral.getChildren().clear();
                panelCentral.getChildren().add(nuevaVista);
            } else {
                System.err.println("🔴 Error: El panel central del Navegador no está configurado.");
            }
        } catch (Exception e) {
            System.err.println("🔴 Error al intentar navegar a la ruta: " + rutaFxml);
            e.printStackTrace();
        }
    }
}