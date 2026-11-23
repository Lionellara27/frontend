package com.nakel.frontend.controller;

import com.nakel.frontend.model.DetalleVenta;
import com.nakel.frontend.model.Venta;
import com.nakel.frontend.service.GeneradorPdfService;
import com.nakel.frontend.service.ImpresoraService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class OpcionesImpresionController {

    private Venta ventaActual;

    public void cargarVenta(Venta venta) {
        this.ventaActual = venta;
        System.out.println("📄 Venta cargada: " + (venta != null ? venta.getId() : "NULL"));
    }

    @FXML
    public void imprimirTicket(ActionEvent event) {
        if (ventaActual == null) {
            mostrarError("Venta no disponible", "No se pudo obtener la venta que se desea imprimir.");
            return;
        }

        System.out.println("🖨️ Enviando ticket de venta #" + ventaActual.getId() + " a la impresora térmica...");

        try {
            StringBuilder texto = new StringBuilder();

            // La fecha de la venta llega como TEXTO ("2026-11-23T15:27:50..."). Antes se la pasaba
            // a SimpleDateFormat, que solo acepta Date, y por eso imprimir fallaba SIEMPRE.
            String fecha = formatearFecha(ventaActual.getFechaHora());

            // Ticket de regalo: no debe mostrar precios
            boolean esRegalo = Boolean.TRUE.equals(ventaActual.getEsParaRegalo())
                    || "TICKET_REGALO".equalsIgnoreCase(ventaActual.getTipoComprobante());

            texto.append("------ NAKEL SOFTWARE ------\n");

            String tipoImpresion = ventaActual.getTipoComprobante() != null
                    && ventaActual.getTipoComprobante().equalsIgnoreCase("Presupuesto")
                    ? "       PRESUPUESTO\n"
                    : (esRegalo ? "     TICKET DE REGALO\n" : "      TICKET DE VENTA\n");

            texto.append(tipoImpresion);
            texto.append("----------------------------\n");

            texto.append("Operacion N°: ")
                    .append(String.format("%08d", ventaActual.getId()))
                    .append("\n");

            texto.append("Fecha: ")
                    .append(fecha)
                    .append("\n");

            String cliente = "Consumidor Final";

            if (ventaActual.getCliente() != null
                    && ventaActual.getCliente().getNombre() != null
                    && !ventaActual.getCliente().getNombre().isBlank()) {
                cliente = ventaActual.getCliente().getNombre();
            }

            texto.append("Cliente: ")
                    .append(cliente)
                    .append("\n");

            texto.append("----------------------------\n");
            texto.append("CANT  DESCRIPCION\n");
            texto.append("----------------------------\n");

            if (ventaActual.getDetalles() != null) {
                for (DetalleVenta det : ventaActual.getDetalles()) {
                    if (det == null || det.getArticulo() == null) {
                        continue;
                    }

                    String nombre = det.getArticulo().getNombre();

                    if (nombre == null || nombre.isBlank()) {
                        nombre = "Articulo";
                    }

                    if (nombre.length() > 20) {
                        nombre = nombre.substring(0, 20);
                    }

                    texto.append(String.format(
                            "%-4d %-20s\n",
                            det.getCantidad(),
                            nombre
                    ));

                    if (!esRegalo) {
                        texto.append("      ")
                                .append(com.nakel.frontend.util.FormatoMoneda.pesos(det.getSubtotal()))
                                .append("\n");
                    }
                }
            }

            texto.append("----------------------------\n");

            if (esRegalo) {
                texto.append("  Valido para cambio.\n");
                texto.append("  Sin valor comercial.\n");
            } else {
                // Total alineado a la derecha del ticket (28 columnas)
                String total = com.nakel.frontend.util.FormatoMoneda.pesos(ventaActual.getTotal());
                texto.append(String.format("TOTAL:%" + Math.max(1, 28 - 6) + "s\n", total));
            }

            texto.append("----------------------------\n");
            texto.append("    Gracias por su compra!\n");
            texto.append("----------------------------\n");
            texto.append("\n\n\n\n\n");

            // Devuelve false si se canceló la elección de impresora; lanza excepción si falló
            boolean enviado = ImpresoraService.imprimirTexto(texto.toString());
            if (!enviado) {
                return;
            }

            mostrarInformacion(
                    "Ticket enviado",
                    "El ticket se envió a la impresora: " + ImpresoraService.getImpresoraGuardada()
            );

            cerrarVentana(event);

        } catch (Exception e) {
            System.err.println("❌ Error al imprimir el ticket:");
            e.printStackTrace();

            mostrarError(
                    "Error de impresión",
                    "No se pudo imprimir el ticket.\n\n" + e.getMessage()
            );
        }
    }

    // Convierte la fecha que manda el backend (texto ISO) a "dd/MM/yyyy HH:mm"
    private String formatearFecha(String fechaIso) {
        java.time.format.DateTimeFormatter salida = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        if (fechaIso == null || fechaIso.isBlank()) {
            return java.time.LocalDateTime.now().format(salida);
        }
        try {
            return java.time.LocalDateTime.parse(fechaIso.trim()).format(salida);
        } catch (Exception e) {
            return fechaIso; // formato inesperado: la mostramos tal cual antes que fallar
        }
    }

    // Botón "Cambiar impresora": por si se cambió la ticketera o se eligió mal
    @FXML
    public void cambiarImpresora(ActionEvent event) {
        try {
            javax.print.PrintService elegida = ImpresoraService.elegirImpresora();
            if (elegida != null) {
                mostrarInformacion("Impresora de tickets", "Los tickets van a salir por: " + elegida.getName());
            }
        } catch (Exception e) {
            mostrarError("Impresora de tickets", e.getMessage());
        }
    }

    @FXML
    public void guardarPdf(ActionEvent event) {
        if (ventaActual == null) {
            mostrarError(
                    "Venta no disponible",
                    "No se pudo obtener la venta para generar el PDF."
            );
            return;
        }

        System.out.println("📄 Generando comprobante PDF de la venta #" + ventaActual.getId());

        try {
            FileChooser fileChooser = new FileChooser();

            fileChooser.setTitle("Guardar Comprobante PDF");

            String nombreCliente = "Consumidor_Final";

            if (ventaActual.getCliente() != null
                    && ventaActual.getCliente().getNombre() != null
                    && !ventaActual.getCliente().getNombre().isBlank()) {

                nombreCliente = ventaActual.getCliente()
                        .getNombre()
                        .replaceAll("[\\\\/:*?\"<>|]", "")
                        .replace(" ", "_");
            }

            String nombreSugerido =
                    "Venta_"
                            + String.format("%08d", ventaActual.getId())
                            + "_"
                            + nombreCliente
                            + ".pdf";

            fileChooser.setInitialFileName(nombreSugerido);

            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "Documento PDF",
                            "*.pdf"
                    )
            );

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            File file = fileChooser.showSaveDialog(stage);

            if (file == null) {
                System.out.println("📄 Guardado de PDF cancelado por el usuario.");
                return;
            }

            GeneradorPdfService pdfService = new GeneradorPdfService();

            pdfService.generarPdf(
                    ventaActual,
                    file.getAbsolutePath()
            );

            System.out.println("✅ PDF generado correctamente.");
            System.out.println("📄 Archivo: " + file.getAbsolutePath());
            System.out.println("📄 Existe: " + file.exists());
            System.out.println("📄 Tamaño: " + file.length() + " bytes");

            mostrarInformacion(
                    "PDF generado",
                    "El comprobante se guardó correctamente en:\n\n"
                            + file.getAbsolutePath()
            );

            cerrarVentana(event);

        } catch (Exception e) {
            System.err.println("❌ Error al generar el PDF:");
            e.printStackTrace();

            mostrarError(
                    "Error al generar PDF",
                    "No se pudo generar el comprobante PDF.\n\n"
                            + e.getMessage()
            );
        }
    }

    @FXML
    public void enviarCorreo(ActionEvent event) {
        System.out.println("📧 Opción de envío por correo seleccionada.");

        mostrarInformacion(
                "Envío por correo",
                "La función de envío por correo todavía está reservada para una futura versión."
        );
    }

    @FXML
    public void cerrarModal(ActionEvent event) {
        cerrarVentana(event);
    }

    private void cerrarVentana(ActionEvent event) {
        try {
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            System.err.println("❌ No se pudo cerrar la ventana.");
            e.printStackTrace();
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarInformacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
