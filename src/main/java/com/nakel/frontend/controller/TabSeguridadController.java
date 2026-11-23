package com.nakel.frontend.controller;

import com.nakel.frontend.service.UsuarioApiService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

public class TabSeguridadController {

    // Campos de texto para el Usuario
    @FXML private TextField txtUsuarioActual;
    @FXML private TextField txtNuevoUsuario;

    // 🔥 Campos de contraseña 1 (Actual)
    @FXML private PasswordField txtPassActual;
    @FXML private TextField txtPassActualVisible;
    @FXML private ToggleButton btnVerActual;

    // 🔥 Campos de contraseña 2 (Nueva)
    @FXML private PasswordField txtPassNueva;
    @FXML private TextField txtPassNuevaVisible;
    @FXML private ToggleButton btnVerNueva;

    // 🔥 Campos de contraseña 3 (Repetir)
    @FXML private PasswordField txtPassRepetir;
    @FXML private TextField txtPassRepetirVisible;
    @FXML private ToggleButton btnVerRepetir;

    // Servicio de API
    private final UsuarioApiService usuarioApiService = new UsuarioApiService();

    @FXML
    public void initialize() {
        System.out.println("✅ Pestaña Seguridad Iniciada con función 'Ojito'.");

        // 🔥 Vinculamos mágicamente los campos ocultos con los visibles
        configurarOjito(txtPassActual, txtPassActualVisible, btnVerActual);
        configurarOjito(txtPassNueva, txtPassNuevaVisible, btnVerNueva);
        configurarOjito(txtPassRepetir, txtPassRepetirVisible, btnVerRepetir);

        ponerUsuarioDeLaSesion();
    }

    /**
     * El usuario ya viene escrito (es el que inició sesión), arriba y abajo: así, para cambiar
     * solo la contraseña, no hay que tipear el usuario dos veces.
     */
    private void ponerUsuarioDeLaSesion() {
        String usuario = com.nakel.frontend.util.SesionActual.getUsuarioLogueado();
        if (usuario != null && !usuario.isBlank()) {
            txtUsuarioActual.setText(usuario);
            txtNuevoUsuario.setText(usuario);
        }
    }

    // Método que hace el truco del ojito
    private void configurarOjito(PasswordField oculto, TextField visible, ToggleButton boton) {
        // Vincula el texto de ambos en tiempo real para que sean idénticos
        visible.textProperty().bindBidirectional(oculto.textProperty());

        // Si el botón está apretado, muestra el texto plano. Si no, muestra los puntitos.
        visible.visibleProperty().bind(boton.selectedProperty());
        oculto.visibleProperty().bind(boton.selectedProperty().not());

        // Ojito vectorial (antes era un emoji): abierto = ver la clave, tachado = ocultarla
        org.kordamp.ikonli.javafx.FontIcon ojo = new org.kordamp.ikonli.javafx.FontIcon("fas-eye");
        ojo.setIconSize(16);
        ojo.setIconColor(javafx.scene.paint.Color.web("#155643"));
        boton.setText("");
        boton.setGraphic(ojo);
        boton.selectedProperty().addListener((obs, antes, viendo) -> ojo.setIconLiteral(viendo ? "fas-eye-slash" : "fas-eye"));
        com.nakel.frontend.util.Acciones.ayuda(boton, "Mostrar u ocultar la contraseña");
    }

    @FXML
    public void cambiarContrasena(ActionEvent event) {
        // Leemos desde los PasswordField (como están vinculados, siempre tienen el texto correcto)
        String usuarioActual = txtUsuarioActual.getText().trim();
        String nuevoUsuario = txtNuevoUsuario.getText().trim();
        String actual = txtPassActual.getText().trim();
        String nueva = txtPassNueva.getText().trim();
        String repetir = txtPassRepetir.getText().trim();

        // 1. Validar campos vacíos
        if (usuarioActual.isEmpty() || nuevoUsuario.isEmpty() || actual.isEmpty() || nueva.isEmpty() || repetir.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Completá todos los campos.");
            return;
        }

        // 2. Validar que la contraseña nueva se repita correctamente
        if (!nueva.equals(repetir)) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Las contraseñas nuevas no coinciden.");
            return;
        }

        // 3. Verificar que el usuario y la contraseña ACTUALES sean correctos.
        //    Antes este dato se pedía pero no se comprobaba: cualquiera podía cambiar la clave.
        if (!usuarioApiService.login(usuarioActual, actual)) {
            mostrarAlerta(Alert.AlertType.ERROR, "Datos incorrectos",
                    usuarioApiService.isUltimoFalloDeConexion()
                            ? "No se pudo conectar con el servidor del sistema. Probá de nuevo en unos segundos."
                            : "El usuario o la contraseña actual no son correctos.");
            return;
        }

        System.out.println("Mandando a cambiar credenciales al backend...");

        // 4. Pegarle al Backend
        boolean exito = usuarioApiService.actualizarCredenciales(usuarioActual, nuevoUsuario, nueva);

        if (exito) {
            // La sesión tiene que seguir con el nombre NUEVO: si quedaba el viejo,
            // las ventas y la caja se mandaban con un usuario que ya no existía.
            com.nakel.frontend.util.SesionActual.setUsuarioLogueado(nuevoUsuario);

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "¡Usuario y contraseña actualizados correctamente!");
            limpiarCampos();
        } else {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrió un problema al guardar las credenciales.");
        }
    }

    // Método auxiliar para limpiar los campos después del éxito
    private void limpiarCampos() {
        txtUsuarioActual.clear();
        txtNuevoUsuario.clear();
        txtPassActual.clear();
        txtPassNueva.clear();
        txtPassRepetir.clear();

        // 🔥 Desmarcamos los ojitos por si la clienta los dejó apretados
        btnVerActual.setSelected(false);
        btnVerNueva.setSelected(false);
        btnVerRepetir.setSelected(false);

        // Queda escrito el usuario nuevo, listo para un próximo cambio
        ponerUsuarioDeLaSesion();
    }

    // Método auxiliar para no repetir código de alertas
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo, mensaje);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.showAndWait();
    }
}