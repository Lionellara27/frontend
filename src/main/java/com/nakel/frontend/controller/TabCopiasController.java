package com.nakel.frontend.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nakel.frontend.service.CopiasApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Optional;

/**
 * Pestaña "Copias de seguridad" de Configuración.
 *
 * El sistema guarda solo una copia por día de la semana (siempre hay como mucho 7, y se van pisando).
 * Acá se ve cuáles hay, se puede hacer una en el momento y se puede volver el sistema a como estaba un día.
 * La usuaria nunca ve archivos ni carpetas: solo días y fechas.
 */
public class TabCopiasController {

    @FXML private Label lblUltima;
    @FXML private Label lblAviso;
    @FXML private VBox cajaCopias;
    @FXML private Button btnAhora;

    private final CopiasApiService api = new CopiasApiService();

    @FXML
    public void initialize() {
        cargar();
    }

    /** Trae las copias del sistema y arma la lista. */
    private void cargar() {
        cajaCopias.getChildren().clear();
        mostrarAviso(null);

        CopiasApiService.Respuesta respuesta = api.listar();
        if (!respuesta.ok()) {
            lblUltima.setText("No se pudieron consultar las copias de seguridad.");
            cajaCopias.getChildren().add(textoGris(respuesta.cuerpo()));
            return;
        }

        JsonObject datos = JsonParser.parseString(respuesta.cuerpo()).getAsJsonObject();
        JsonArray copias = datos.getAsJsonArray("copias");
        if (datos.has("aviso") && !datos.get("aviso").isJsonNull()) mostrarAviso(datos.get("aviso").getAsString());

        JsonObject masNueva = null;
        for (JsonElement elemento : copias) {
            JsonObject copia = elemento.getAsJsonObject();
            if (masNueva == null && !copia.get("esRespaldo").getAsBoolean()) masNueva = copia;
            cajaCopias.getChildren().add(renglon(copia));
        }

        if (masNueva == null) {
            lblUltima.setText("Todavía no hay ninguna copia de seguridad.");
            cajaCopias.getChildren().add(textoGris("La primera se hace sola la próxima vez que se abra el sistema, o ahora con el botón de arriba."));
        } else {
            boolean deHoy = masNueva.get("deHoy").getAsBoolean();
            lblUltima.setText("Última copia: " + (deHoy ? "hoy" : masNueva.get("nombre").getAsString().toLowerCase()
                    + " " + masNueva.get("fecha").getAsString()) + " a las " + masNueva.get("hora").getAsString() + " hs");
        }
    }

    /** Un renglón de la lista: "Martes 24/11/2026 · 09:14 hs" y el botón para volver a ese día. */
    private HBox renglon(JsonObject copia) {
        boolean esRespaldo = copia.get("esRespaldo").getAsBoolean();
        String cuando = copia.get("fecha").getAsString() + " · " + copia.get("hora").getAsString() + " hs";

        Label titulo = new Label(esRespaldo ? copia.get("nombre").getAsString() : copia.get("nombre").getAsString() + " " + cuando
                + (copia.get("deHoy").getAsBoolean() ? "   (hoy)" : ""));
        titulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333333;");
        titulo.setWrapText(true);

        VBox textos = new VBox(2, titulo);
        textos.setAlignment(Pos.CENTER_LEFT);
        if (esRespaldo) textos.getChildren().add(textoGris("Guardada el " + cuando + ". Sirve para deshacer la última vez que se volvió atrás."));
        HBox.setHgrow(textos, Priority.ALWAYS);

        Button volver = new Button(esRespaldo ? "Deshacer la vuelta atrás" : "Volver a este día");
        volver.getStyleClass().add("btn-secundario");
        volver.setMinWidth(Region.USE_PREF_SIZE);
        volver.setOnAction(e -> restaurar(copia));

        HBox fila = new HBox(12, textos, volver);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setStyle("-fx-padding: 9 12 9 12; -fx-background-color: " + (esRespaldo ? "#FFF8EC" : "#F8F9FA")
                + "; -fx-background-radius: 8; -fx-border-color: #E5E7EB; -fx-border-radius: 8;");
        return fila;
    }

    @FXML
    private void hacerCopiaAhora() {
        btnAhora.setDisable(true);
        CopiasApiService.Respuesta respuesta = api.hacerAhora();
        btnAhora.setDisable(false);
        if (respuesta.ok()) {
            mostrar(Alert.AlertType.INFORMATION, "Copia de seguridad", "Listo: se guardó una copia de cómo está el sistema en este momento.");
        } else {
            mostrar(Alert.AlertType.ERROR, "No se pudo hacer la copia", respuesta.cuerpo());
        }
        cargar();
    }

    private void restaurar(JsonObject copia) {
        boolean esRespaldo = copia.get("esRespaldo").getAsBoolean();
        String cuando = (esRespaldo ? "" : copia.get("nombre").getAsString().toLowerCase() + " ")
                + copia.get("fecha").getAsString() + " a las " + copia.get("hora").getAsString() + " hs";

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Volver atrás");
        confirmacion.setHeaderText("¿Volver el sistema a como estaba el " + cuando + "?");
        confirmacion.setContentText("Todo lo que se cargó DESPUÉS de ese momento (ventas, artículos, cambios de precios, clientes) "
                + "se va a perder.\n\nAntes de hacerlo, el sistema guarda cómo está ahora, así que si te arrepentís "
                + "se puede deshacer desde esta misma pantalla.\n\nAl terminar, el sistema se cierra y hay que volver a abrirlo.");
        ButtonType aceptar = new ButtonType("Sí, volver a ese día", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmacion.getButtonTypes().setAll(cancelar, aceptar);
        Optional<ButtonType> elegido = confirmacion.showAndWait();
        if (elegido.isEmpty() || elegido.get() != aceptar) return;

        CopiasApiService.Respuesta respuesta = api.restaurar(copia.get("dia").getAsString());
        if (!respuesta.ok()) {
            mostrar(Alert.AlertType.ERROR, "No se pudo volver atrás", respuesta.cuerpo());
            cargar();
            return;
        }

        mostrar(Alert.AlertType.INFORMATION, "Listo", "El sistema volvió a como estaba el " + cuando
                + ".\n\nAhora se va a cerrar. Volvé a abrirlo para seguir trabajando.");
        Platform.exit(); // al reabrir, el sistema revisa la base y arranca limpio
    }

    private void mostrarAviso(String texto) {
        boolean hay = texto != null && !texto.isBlank();
        lblAviso.setText(hay ? "Atención: " + texto : "");
        lblAviso.setVisible(hay);
        lblAviso.setManaged(hay);
    }

    private static Label textoGris(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("dato-ayuda");
        etiqueta.setWrapText(true);
        etiqueta.setMinHeight(Region.USE_PREF_SIZE);
        return etiqueta;
    }

    private static void mostrar(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo, mensaje);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.showAndWait();
    }
}
