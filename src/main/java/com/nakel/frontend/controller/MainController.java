package com.nakel.frontend.controller;

import com.nakel.frontend.service.ClienteApiService;
import com.nakel.frontend.util.Icono;
import com.nakel.frontend.util.Navegador; // ¡Importamos tu Router!
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainController {
    // 1. ENLAZAMOS LOS BOTONES Y EL PANEL DEL FXML
    // ==========================================================
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

    @FXML
    public void initialize() {
        ClienteApiService api = new ClienteApiService();
        //api.probarConexion();

        // 1. Le entregamos el panel central al Router para que tome el control
        Navegador.setPanelCentral(this.areaContenido);

        // 2. Mostramos el mensaje inicial
        mostrarBienvenida();

        // ¡INYECTAMOS LOS VECTORES DORADOS A CADA BOTÓN!
        //ufamos un if noNull pro si alguna vez se borra un boton para que nada explote
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

    @FXML
    public void mostrarPuntoDeVenta(ActionEvent event) {
        Navegador.cargarVista("/com/nakel/frontend/view/venta-view.fxml");
    }

    @FXML
    public void mostrarClientes(ActionEvent event) {
        Navegador.cargarVista("/com/nakel/frontend/view/cliente-view.fxml");
    }

    @FXML
    public void mostrarInsumos(ActionEvent event) {
        Navegador.cargarVista("/com/nakel/frontend/view/insumo-view.fxml");
    }

    @FXML
    public void mostrarCalculadora(ActionEvent event) {
        Navegador.cargarVista("/com/nakel/frontend/view/calcular-produccion-view.fxml");
    }

    @FXML
    public void mostrarProveedores(ActionEvent event) {
        Navegador.cargarVista("/com/nakel/frontend/view/proveedor-view.fxml");
    }

    @FXML
    public void mostrarHistorialVentas(ActionEvent event) {
        Navegador.cargarVista("/com/nakel/frontend/view/historial-ventas-view.fxml");
    }

    @FXML
    public void mostrarCatalogo(ActionEvent event) {
        System.out.println("Abriendo Catálogo de Artículos...");
        Navegador.cargarVista("/com/nakel/frontend/view/articulo-view.fxml");
    }

    @FXML
    public void mostrarConfiguracion(ActionEvent event) {
        System.out.println("Abriendo Configuración...");
        Navegador.cargarVista("/com/nakel/frontend/view/configuracion.fxml");
    }

    // ==========================================================
    // MENSAJE DE INICIO (Por defecto)
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
}