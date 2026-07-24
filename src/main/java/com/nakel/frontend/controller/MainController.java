package com.nakel.frontend.controller;

import com.nakel.frontend.service.ClienteApiService;
import com.nakel.frontend.service.CajaApiService; // 🔥 IMPORTANTE
import com.nakel.frontend.util.Icono;
import com.nakel.frontend.util.Navegador;
import com.nakel.frontend.util.SesionActual;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Optional;

public class MainController {

    @FXML private StackPane areaContenido;

    @FXML private Button btnMostrador;
    @FXML private Button btnCatalogo;
    @FXML private Button btnClientes;
    @FXML private Button btnProveedores;
    @FXML private Button btnHistorialVentas;
    @FXML private Button btnHistorialCajas;
    @FXML private Button btnInsumos;
    @FXML private Button btnCalculadora;
    @FXML private Button btnEstadisticas;
    @FXML private Button btnCerrarCaja;
    @FXML private Button btnConfiguracion;

    // 🔥 INSTANCIAMOS EL SERVICIO DE LA CAJA
    private final CajaApiService cajaApiService = new CajaApiService();

    @FXML
    public void initialize() {
        // 1. Le entregamos el panel central al Router
        Navegador.setPanelCentral(this.areaContenido);

        // 2. Mostramos el mensaje inicial
        mostrarBienvenida();

        // 🔥 3. MAGIA: ABRIR CAJA AUTOMÁTICAMENTE
        // Le pegamos al backend apenas entra. Si no había caja abierta hoy, el backend la crea en este exacto momento.
        System.out.println("Comprobando estado de la caja diaria...");
        cajaApiService.obtenerCajaActual(SesionActual.getUsuarioLogueado());

        // 4. Inyectamos los iconos
        if (btnMostrador != null) btnMostrador.setGraphic(Icono.MOSTRADOR.construir("#333333"));
        if (btnCatalogo != null) btnCatalogo.setGraphic(Icono.CATALOGO.construir());
        if (btnClientes != null) btnClientes.setGraphic(Icono.CLIENTES.construir());
        if (btnProveedores != null) btnProveedores.setGraphic(Icono.PROVEEDORES.construir());
        if (btnHistorialVentas != null) btnHistorialVentas.setGraphic(Icono.HISTORIAL.construir());
        if (btnHistorialCajas != null) btnHistorialCajas.setGraphic(Icono.CAJAS.construir());
        if (btnInsumos != null) btnInsumos.setGraphic(Icono.INSUMOS.construir());
        if (btnCalculadora != null) btnCalculadora.setGraphic(Icono.CALCULADORA.construir());
        if (btnEstadisticas != null) btnEstadisticas.setGraphic(Icono.ESTADISTICAS.construir());
        if (btnCerrarCaja != null) btnCerrarCaja.setGraphic(Icono.CERRAR_CAJA.construir());
        if (btnConfiguracion != null) btnConfiguracion.setGraphic(Icono.CONFIGURACION.construir());
    }

    // ==========================================================
    // NAVEGACIÓN LIMPIA (Usando el Router)
    // ==========================================================

    @FXML public void mostrarPuntoDeVenta(ActionEvent event) { Navegador.cargarVista("/com/nakel/frontend/view/venta-view.fxml"); }
    @FXML public void mostrarClientes(ActionEvent event) { Navegador.cargarVista("/com/nakel/frontend/view/cliente-view.fxml"); }
    @FXML public void mostrarInsumos(ActionEvent event) { Navegador.cargarVista("/com/nakel/frontend/view/insumo-view.fxml"); }
    @FXML public void mostrarCalculadora(ActionEvent event) { Navegador.cargarVista("/com/nakel/frontend/view/calcular-produccion-view.fxml"); }
    @FXML public void mostrarProveedores(ActionEvent event) { Navegador.cargarVista("/com/nakel/frontend/view/proveedor-view.fxml"); }
    @FXML public void mostrarHistorialVentas(ActionEvent event) { Navegador.cargarVista("/com/nakel/frontend/view/historial-ventas-view.fxml"); }
    @FXML public void mostrarCatalogo(ActionEvent event) { Navegador.cargarVista("/com/nakel/frontend/view/articulo-view.fxml"); }
    @FXML public void mostrarConfiguracion(ActionEvent event) { Navegador.cargarVista("/com/nakel/frontend/view/configuracion.fxml"); }
    @FXML public void mostrarEstadisticas(ActionEvent event) { Navegador.cargarVista("/com/nakel/frontend/view/estadisticas.fxml"); }
    @FXML public void mostrarHistorialCajas(ActionEvent event) {Navegador.cargarVista("/com/nakel/frontend/view/historial-cajas-view.fxml");}
    // ==========================================================
    // 🔥 LÓGICA PARA CERRAR LA CAJA Y VOLVER AL LOGIN
    // ==========================================================
    @FXML
    public void cerrarCaja(ActionEvent event) {
        Alert opciones = new Alert(Alert.AlertType.CONFIRMATION);
        opciones.setTitle("Cerrar Caja");
        opciones.setHeaderText("¿Qué desea hacer?");
        opciones.setContentText("Seleccione cómo desea salir del sistema:");

        // 1. Creamos los 3 botones exactos que pediste
        ButtonType btnVolverLogin = new ButtonType("Volver al Login");
        ButtonType btnCerrarSalir = new ButtonType("Cerrar y Salir");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        opciones.getButtonTypes().setAll(btnVolverLogin, btnCerrarSalir, btnCancelar);

        Optional<ButtonType> resultado = opciones.showAndWait();

        // 2. Si eligió "Cancelar" o cerró la ventanita de la X, cortamos acá nomás
        if (!resultado.isPresent() || resultado.get() == btnCancelar) {
            return;
        }

        // 3. Si eligió alguna de las otras dos opciones, SÍ O SÍ CERRAMOS LA CAJA PRIMERO
        boolean exito = cajaApiService.cerrarCaja(SesionActual.getUsuarioLogueado());

        // Si ya estaba cerrada (Status 400), avisamos pero dejamos que salga igual
        if (!exito) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Aviso");
            info.setHeaderText(null);
            info.setContentText("Aviso: La caja ya se encontraba cerrada.");
            info.showAndWait();
        }

        // 4. Ahora sí, lo mandamos a donde pidió
        if (resultado.get() == btnVolverLogin) {
            irAlLogin(event); // Cierra sesión y va a la pantalla principal
        } else if (resultado.get() == btnCerrarSalir) {
            javafx.application.Platform.exit(); // Apaga todo el programa
            System.exit(0);
        }
    }

    // 🔥 Este método maneja el salto de pantalla sin errores
    private void irAlLogin(ActionEvent event) {
        try {
            // ⚠️ REVISÁ QUE ESTE NOMBRE SEA EXACTAMENTE EL DE TU ARCHIVO FXML DE LOGIN
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/nakel/frontend/view/login-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            java.net.URL cssUrl = getClass().getResource("/css/nakel.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Nakel Software - Iniciar Sesión");
            stage.centerOnScreen();

        } catch (Exception e) {
            mostrarAlertaError("Error de Pantalla", "No se encontró el archivo de Login. Revisá el nombre del archivo.");
            e.printStackTrace();
        }
    }

    // ==========================================================
    // UTILIDADES
    // ==========================================================
    private void mostrarBienvenida() {
        VBox bienvenida = new VBox(15);
        bienvenida.setAlignment(Pos.CENTER);

        Label titulo = new Label("Panel Principal");
        titulo.getStyleClass().add("welcome-title");

        Label subtitulo = new Label("Bienvenida a Nakel ERP");
        subtitulo.getStyleClass().add("welcome-subtitle");

        bienvenida.getChildren().addAll(titulo, subtitulo);

        if(areaContenido != null) {
            areaContenido.getChildren().clear();
            areaContenido.getChildren().add(bienvenida);
        }
    }

    private void mostrarAlertaError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}