package com.nakel.frontend.controller;

import com.nakel.frontend.model.AjustesDivisa;
import com.nakel.frontend.model.Categoria;
import com.nakel.frontend.service.ArticuloApiService;
import com.nakel.frontend.service.ConfiguracionApiService;
import com.nakel.frontend.service.ParametrosApiService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class TabDivisasController {

    @FXML
    private TextField txtCotizacionDolar;

    @FXML
    private CheckBox chkActualizacionAutomatica;

    @FXML
    private ComboBox<Categoria> cmbCategoriaAumento;

    @FXML
    private TextField txtPorcentajeAumento;

    @FXML
    private Button btnGuardarCotizacion;

    @FXML
    private Button btnAplicarAumento;

    @FXML
    private Button btnSincronizarDolar;

    private final ConfiguracionApiService apiService =
            new ConfiguracionApiService();

    private final ParametrosApiService parametrosApiService =
            new ParametrosApiService();

    private final ArticuloApiService articuloApiService =
            new ArticuloApiService();

    @FXML
    public void initialize() {
        System.out.println("========== PESTAÑA DIVISAS ==========");
        System.out.println("💵 Inicializando configuración de divisas...");

        cargarCategorias();
        cargarAjustesDivisa();
        actualizarEstadoCampoCotizacion();
    }

// ============================================================
// CARGAR CATEGORÍAS REALES DESDE EL BACKEND
// ============================================================

    private void cargarCategorias() {
        System.out.println("📦 Cargando categorías desde el backend...");

        cmbCategoriaAumento.getItems().clear();

        try {
            List<Categoria> categorias =
                    parametrosApiService.obtenerCategorias();

            if (categorias == null || categorias.isEmpty()) {
                System.out.println("⚠️ No se encontraron categorías en el backend.");
                return;
            }

            for (Categoria categoria : categorias) {
                if (categoria == null ||
                        categoria.getNombre() == null ||
                        categoria.getNombre().isBlank()) {
                    continue;
                }

                cmbCategoriaAumento.getItems().add(categoria);

                System.out.println(
                        "✅ Categoría cargada: "
                                + categoria.getId()
                                + " - "
                                + categoria.getNombre()
                );
            }

            if (!cmbCategoriaAumento.getItems().isEmpty()) {
                cmbCategoriaAumento.getSelectionModel().select(0);
            }

            System.out.println(
                    "✅ Total de categorías cargadas: "
                            + cmbCategoriaAumento.getItems().size()
            );

        } catch (Exception e) {
            System.err.println("❌ Error al cargar categorías.");
            e.printStackTrace();
        }
    }

// ============================================================
// CARGAR CONFIGURACIÓN DE DIVISA
// ============================================================

    private void cargarAjustesDivisa() {
        System.out.println("💵 Consultando configuración de divisa...");

        try {
            AjustesDivisa ajustes =
                    apiService.obtenerAjustesDivisa();

            if (ajustes == null) {
                System.out.println(
                        "⚠️ El backend no devolvió configuración."
                );
                return;
            }

            txtCotizacionDolar.setText(
                    String.valueOf(ajustes.getCotizacionUsd())
            );

            chkActualizacionAutomatica.setSelected(
                    ajustes.isModoAutomatico()
            );

            System.out.println(
                    "✅ Cotización cargada: "
                            + ajustes.getCotizacionUsd()
            );

            System.out.println(
                    "✅ Modo automático: "
                            + ajustes.isModoAutomatico()
            );

        } catch (Exception e) {
            System.err.println(
                    "❌ Error al cargar configuración de divisa."
            );
            e.printStackTrace();
        }
    }

// ============================================================
// CAMBIO DE MODO AUTOMÁTICO
// ============================================================

    @FXML
    public void toggleModoAutomatico(ActionEvent event) {
        actualizarEstadoCampoCotizacion();
    }

    private void actualizarEstadoCampoCotizacion() {
        boolean automatico =
                chkActualizacionAutomatica.isSelected();

        txtCotizacionDolar.setDisable(automatico);

        System.out.println(
                "⚙️ Modo automático: " + automatico
        );
    }

// ============================================================
// GUARDAR COTIZACIÓN
// ============================================================

    @FXML
    public void guardarCotizacion(ActionEvent event) {
        String texto = txtCotizacionDolar.getText();

        if (texto == null || texto.isBlank()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Cotización inválida",
                    "Ingresá una cotización para el dólar."
            );
            return;
        }

        try {
            Double valor =
                    Double.parseDouble(
                            texto.replace(",", ".")
                    );

            if (valor <= 0) {
                mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Cotización inválida",
                        "La cotización debe ser mayor que cero."
                );
                return;
            }

            AjustesDivisa ajustes =
                    new AjustesDivisa(
                            valor,
                            chkActualizacionAutomatica.isSelected()
                    );

            System.out.println(
                    "💾 Guardando configuración de divisa..."
            );

            boolean exito =
                    apiService.guardarAjustesDivisa(ajustes);

            if (exito) {
                mostrarAlerta(
                        Alert.AlertType.INFORMATION,
                        "Configuración guardada",
                        "Los ajustes de divisa se guardaron correctamente."
                );
            } else {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error",
                        "No se pudo guardar la configuración en el servidor."
                );
            }

        } catch (NumberFormatException e) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Cotización inválida",
                    "Ingresá un número válido.\nEjemplo: 1420"
            );
        }
    }

// ============================================================
// AUMENTO MASIVO
// ============================================================

    @FXML
    public void aplicarAumentoMasivo(ActionEvent event) {
        Categoria categoria =
                cmbCategoriaAumento.getValue();

        String textoPorcentaje =
                txtPorcentajeAumento.getText();

        if (categoria == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Categoría requerida",
                    "Seleccioná una categoría."
            );
            return;
        }

        if (textoPorcentaje == null ||
                textoPorcentaje.isBlank()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Porcentaje requerido",
                    "Ingresá el porcentaje de aumento."
            );
            return;
        }

        double porcentaje;

        try {
            porcentaje =
                    Double.parseDouble(
                            textoPorcentaje.replace(",", ".")
                    );
        } catch (NumberFormatException e) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Porcentaje inválido",
                    "Ingresá un porcentaje numérico.\nEjemplo: 10"
            );
            return;
        }

        if (porcentaje <= 0) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Porcentaje inválido",
                    "El porcentaje debe ser mayor que cero."
            );
            return;
        }

        if (porcentaje > 1000) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Porcentaje demasiado alto",
                    "El aumento no puede superar el 1000%."
            );
            return;
        }

        Alert confirmacion =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle(
                "Confirmar aumento masivo"
        );

        confirmacion.setHeaderText(
                "⚠️ Actualización de precios"
        );

        confirmacion.setContentText(
                "Categoría: "
                        + categoria.getNombre()
                        + "\n\n"
                        + "Aumento: "
                        + porcentaje
                        + "%\n\n"
                        + "¿Querés continuar?"
        );

        Optional<ButtonType> resultado =
                confirmacion.showAndWait();

        if (resultado.isEmpty() ||
                resultado.get() != ButtonType.OK) {
            System.out.println(
                    "ℹ️ Aumento cancelado por el usuario."
            );
            return;
        }

        System.out.println("🚀 Ejecutando aumento masivo...");
        System.out.println(
                "📦 Categoría: "
                        + categoria.getNombre()
        );
        System.out.println(
                "🆔 ID categoría: "
                        + categoria.getId()
        );
        System.out.println(
                "📈 Porcentaje: "
                        + porcentaje
                        + "%"
        );

        int cantidadActualizada =
                articuloApiService.aumentarPrecios(
                        categoria.getId(),
                        BigDecimal.valueOf(porcentaje)
                );

        if (cantidadActualizada >= 0) {
            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Precios actualizados",
                    "✅ Se actualizaron "
                            + cantidadActualizada
                            + " artículos."
            );

            txtPorcentajeAumento.clear();

        } else {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error al actualizar",
                    "No se pudieron actualizar los precios."
            );
        }
    }

// ============================================================
// SINCRONIZAR PRECIOS CON DÓLAR
// ============================================================

    @FXML
    public void sincronizarPreciosConDolar(ActionEvent event) {
        String cotizacion =
                txtCotizacionDolar.getText();

        if (cotizacion == null ||
                cotizacion.isBlank()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Cotización no disponible",
                    "No hay una cotización válida para sincronizar."
            );
            return;
        }

        Alert confirmacion =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle(
                "Sincronizar precios con dólar"
        );

        confirmacion.setHeaderText(
                "Actualización por cotización"
        );

        confirmacion.setContentText(
                "Cotización utilizada: $"
                        + cotizacion
                        + "\n\n"
                        + "¿Querés recalcular los precios?"
        );

        Optional<ButtonType> resultado =
                confirmacion.showAndWait();

        if (resultado.isEmpty() ||
                resultado.get() != ButtonType.OK) {
            System.out.println(
                    "ℹ️ Sincronización cancelada."
            );
            return;
        }

        System.out.println(
                "🔄 Sincronización solicitada."
        );

        mostrarAlerta(
                Alert.AlertType.INFORMATION,
                "Sincronización pendiente",
                "La cotización está disponible.\n\n"
                        + "La sincronización automática de precios todavía "
                        + "requiere conectar el servicio de cotización."
        );
    }

// ============================================================
// ALERTAS
// ============================================================

    private void mostrarAlerta(
            Alert.AlertType tipo,
            String titulo,
            String mensaje) {

        Alert alert =
                new Alert(tipo);

        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}





/*package com.nakel.frontend.controller;

import com.nakel.frontend.model.AjustesDivisa;
import com.nakel.frontend.service.ConfiguracionApiService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.Optional;

public class TabDivisasController {

    @FXML private TextField txtCotizacionDolar;
    @FXML private CheckBox chkActualizacionAutomatica;
    @FXML private ComboBox<String> cmbCategoriaAumento;
    @FXML private TextField txtPorcentajeAumento;
    @FXML private Button btnGuardarCotizacion;
    @FXML private Button btnAplicarAumento;
    @FXML private Button btnSincronizarDolar;

    private final ConfiguracionApiService apiService = new ConfiguracionApiService();

    @FXML
    public void initialize() {
        System.out.println("Pestaña Divisas Iniciada.");
        cmbCategoriaAumento.getItems().addAll("Todas las categorías", "Billeteras", "Mates", "Lámparas");
        cmbCategoriaAumento.setValue("Todas las categorías");

        AjustesDivisa ajustes = apiService.obtenerAjustesDivisa();
        txtCotizacionDolar.setText(String.valueOf(ajustes.getCotizacionUsd()));
        chkActualizacionAutomatica.setSelected(ajustes.isModoAutomatico());

        actualizarEstadoCampoCotizacion();
    }

    @FXML
    public void toggleModoAutomatico(ActionEvent event) {
        actualizarEstadoCampoCotizacion();
    }

    private void actualizarEstadoCampoCotizacion() {
        txtCotizacionDolar.setDisable(chkActualizacionAutomatica.isSelected());
    }

    @FXML
    public void guardarCotizacion(ActionEvent event) {
        try {
            Double valor = Double.parseDouble(txtCotizacionDolar.getText());
            boolean exito = apiService.guardarAjustesDivisa(new AjustesDivisa(valor, chkActualizacionAutomatica.isSelected()));

            if (exito) {
                new Alert(Alert.AlertType.INFORMATION, "Ajustes de divisa guardados correctamente.").showAndWait();
            } else {
                new Alert(Alert.AlertType.ERROR, "No se pudo guardar en el servidor.").showAndWait();
            }
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Ingresá un número válido para la cotización.").showAndWait();
        }
    }

    @FXML
    public void aplicarAumentoMasivo(ActionEvent event) {
        String categoria = cmbCategoriaAumento.getValue();
        String porcentaje = txtPorcentajeAumento.getText();

        if (porcentaje.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Por favor, ingresá un porcentaje.").showAndWait();
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Aumento Masivo");
        confirmacion.setHeaderText("Vas a aumentar: " + categoria);
        confirmacion.setContentText("¿Estás seguro de incrementar un " + porcentaje + "%?");

        Optional<ButtonType> result = confirmacion.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.out.println("🚀 Aumentando " + porcentaje + "% a " + categoria);
        }
    }

    @FXML
    public void sincronizarPreciosConDolar(ActionEvent event) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Sincronizar con Dólar");
        confirmacion.setHeaderText("Actualización por desfasaje");
        confirmacion.setContentText("¿Recalcular precios basándose en $" + txtCotizacionDolar.getText() + "?");

        Optional<ButtonType> result = confirmacion.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.out.println("🔄 Sincronizando con cotización: " + txtCotizacionDolar.getText());
        }
    }
}


 */