package com.nakel.frontend;

import com.nakel.frontend.util.ServidorLocal;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {

        // Ícono y estética de Nakel para TODAS las ventanas emergentes (alertas, diálogos, modales)
        com.nakel.frontend.util.Ventanas.instalar();

        // Si el motor del sistema ya está andando (o estamos en el editor de código), se va directo al login
        if (!ServidorLocal.arrancando() && (ServidorLocal.responde() || ServidorLocal.archivoDelBackend() == null)) {
            mostrarLogin(stage);
            return;
        }

        // Si no, se muestra un cartel de "Iniciando..." mientras arranca, y recién después el login.
        // Así nunca aparece el login antes de tiempo diciendo que no puede conectar.
        Label estado = new Label("Iniciando el sistema…");
        Stage cartel = crearCartelDeInicio(estado);
        cartel.show();

        Thread arranque = new Thread(() -> {
            String problema = ServidorLocal.asegurar(texto -> Platform.runLater(() -> estado.setText(texto)));
            Platform.runLater(() -> {
                cartel.close();
                if (problema != null) {
                    Alert aviso = new Alert(Alert.AlertType.ERROR);
                    aviso.setTitle("Nakel ERP no pudo iniciar");
                    aviso.setHeaderText("El sistema no terminó de iniciar");
                    aviso.setContentText(problema + "\n\nCerrá esta ventana y volvé a abrir Nakel. Si sigue pasando, avisale a quien te instaló el sistema.");
                    aviso.showAndWait();
                }
                mostrarLogin(stage);
            });
        }, "arranque-del-sistema");
        arranque.setDaemon(true);
        arranque.start();
    }

    /** Ventanita sin bordes con el logo y una barra que se mueve, mientras arranca el motor del sistema. */
    private Stage crearCartelDeInicio(Label estado) {
        VBox caja = new VBox(14);
        caja.setAlignment(Pos.CENTER);
        caja.setStyle("-fx-background-color: white; -fx-padding: 28 40 28 40; -fx-border-color: #155643; -fx-border-width: 2;");

        try {
            ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/images/Logoo.png")));
            logo.setFitHeight(96);
            logo.setPreserveRatio(true);
            caja.getChildren().add(logo);
        } catch (Exception sinLogo) {
            // el cartel sirve igual sin la imagen
        }

        Label titulo = new Label("Nakel ERP");
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #155643;");
        estado.setStyle("-fx-font-size: 14px; -fx-text-fill: #5F6B76;");
        ProgressBar barra = new ProgressBar(); // sin valor: se mueve sola
        barra.setPrefWidth(260);
        barra.setStyle("-fx-accent: #D49A54;");
        caja.getChildren().addAll(titulo, barra, estado);

        Stage cartel = new Stage(StageStyle.UNDECORATED);
        cartel.setScene(new Scene(caja, 400, 290));
        cartel.setTitle("Nakel ERP");
        ponerIconos(cartel);
        cartel.centerOnScreen();
        return cartel;
    }

    private void mostrarLogin(Stage stage) {
        try {
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
            }

            // 3. Íconos de la ventana
            ponerIconos(stage);

            stage.setTitle("Nakel Software - Inicio de Sesión");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir la pantalla de ingreso: " + e.getMessage()).showAndWait();
            Platform.exit();
        }
    }

    private void ponerIconos(Stage stage) {
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/FlaviconSin.png")));
        } catch (Exception e) {
            try {
                stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/TaskbarSin.png")));
            } catch (Exception ex) {
                System.out.println("No se pudieron cargar los íconos de la app.");
            }
        }
    }

    /** Al cerrar el programa se apaga también el motor del sistema (si lo arrancó esta ventana). */
    @Override
    public void stop() {
        ServidorLocal.apagar();
    }

    public static void main(String[] args) {
        launch();
    }
}
