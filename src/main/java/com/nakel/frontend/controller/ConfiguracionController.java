package com.nakel.frontend.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TabPane;

public class ConfiguracionController {

    @FXML private TabPane tabPaneConfig;

    @FXML
    public void initialize() {
        System.out.println("Módulo de Configuración (Padre) Iniciado.");
    }
}