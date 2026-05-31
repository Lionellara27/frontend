module com.nakel.frontend {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.nakel.frontend to javafx.fxml;
    // Le damos permiso a JavaFX para leer tu carpeta de controladores
    opens com.nakel.frontend.controller to javafx.fxml;

    exports com.nakel.frontend;
    exports com.nakel.frontend.controller;
}