package com.nakel.frontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        // Cargar el FXML del Login
        FXMLLoader fxmlLoader = new FXMLLoader(
                MainApp.class.getResource("/com/nakel/frontend/view/login-view.fxml")
        );

        Scene scene = new Scene(fxmlLoader.load(), 600, 400);

        // Cargar el CSS global (¡Con la ruta corta correcta!)
        java.net.URL cssUrl = MainApp.class.getResource("/css/nakel.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.out.println("⚠️ OJO: No se encontró el CSS en /css/nakel.css");
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