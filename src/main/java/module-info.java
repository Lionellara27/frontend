module com.nakel.frontend {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.google.gson;
    requires org.kordamp.ikonli.javafx;

    opens com.nakel.frontend to javafx.fxml;
    // Le damos permiso a JavaFX para leer tu carpeta de controladores
    opens com.nakel.frontend.controller to javafx.fxml;

    // 🔥 LA LÍNEA MÁGICA QUE FALTABA:
    // Le da permiso a GSON para armar los JSON y a JavaFX para leer los datos de la tabla
    opens com.nakel.frontend.model to com.google.gson, javafx.base;

    exports com.nakel.frontend;
    exports com.nakel.frontend.controller;
    exports com.nakel.frontend.model;
}