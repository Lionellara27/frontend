package com.nakel.frontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        // 1. Cargamos el FXML usando la ruta exacta y segura
        URL fxmlUrl = MainApp.class.getResource("/com/nakel/frontend/view/login-view.fxml");
        if (fxmlUrl == null) {
            throw new IOException("No se pudo encontrar el archivo login-view.fxml en las rutas de recursos.");
        }

        FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);

        // 2. Cargar el CSS global
        URL cssUrl = MainApp.class.getResource("/css/nakel.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.out.println("⚠️ OJO: No se encontró el CSS en /css/nakel.css");
        }

        // 3. CARGAR LOS ÍCONOS USANDO LA MISMA RUTA RELATIVA QUE EL FXML O ABSOLUTA DESDE LA RAÍZ
        try {
            // Intentamos cargar usando la ruta absoluta desde resources
            Image favicon = new Image(getClass().getResourceAsStream("/images/FlaviconSin.png"));
            Image taskbarIcon = new Image(getClass().getResourceAsStream("/images/FlaviconSin.png"));

            stage.getIcons().addAll(favicon, taskbarIcon);
        } catch (Exception e) {
            // Si por algo no los encuentra ahí, probamos con el logo genérico para que no explote
            try {
                Image fallback = new Image(getClass().getResourceAsStream("/images/TaskbarSin.png"));
                stage.getIcons().add(fallback);
            } catch (Exception ex) {
                System.out.println("⚠️ No se pudieron cargar los íconos de la app.");
            }
        }

        stage.setTitle("Nakel Software - Inicio de Sesión");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}