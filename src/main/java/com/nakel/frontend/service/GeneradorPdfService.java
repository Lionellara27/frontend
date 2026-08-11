package com.nakel.frontend.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.nakel.frontend.model.DetalleVenta;
import com.nakel.frontend.model.Venta;

import java.io.FileOutputStream;

public class GeneradorPdfService {

    // ====================================================================
    // 🟢 1. GENERAR PDF DE VENTA / PRESUPUESTO / TICKET DE REGALO
    // ====================================================================
    public void generarPdf(Venta venta, String rutaDestino) {
        Document documento = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(documento, new FileOutputStream(rutaDestino));
            documento.open();

            // 🎨 Fuentes
            Font fuenteTitulo = new Font(
                    Font.FontFamily.HELVETICA,
                    18,
                    Font.BOLD
            );

            Font fuenteSubTitulo = new Font(
                    Font.FontFamily.HELVETICA,
                    12,
                    Font.BOLD
            );

            Font fuenteNormal = new Font(
                    Font.FontFamily.HELVETICA,
                    12,
                    Font.NORMAL
            );

            // ================================================================
            // 🎁 DETERMINAR TIPO DE COMPROBANTE
            // ================================================================

            boolean esTicketRegalo =
                    Boolean.TRUE.equals(venta.getEsParaRegalo());

            boolean esPresupuesto =
                    venta.getTipoComprobante() != null
                            && venta.getTipoComprobante()
                            .equalsIgnoreCase("Presupuesto");

            String tipo;

            if (esTicketRegalo) {
                tipo = "TICKET DE REGALO";
            } else if (esPresupuesto) {
                tipo = "PRESUPUESTO";
            } else {
                tipo = "TICKET DE VENTA (NO FISCAL)";
            }

            Paragraph titulo = new Paragraph(tipo, fuenteTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(titulo);
            documento.add(new Paragraph(" "));

            // ================================================================
            // 📅 FECHAS Y DATOS
            // ================================================================

            System.out.println(
                    "🔎 fechaHora = " + venta.getFechaHora()
            );

            System.out.println(
                    "🔎 tipo fechaHora = "
                            + (venta.getFechaHora() != null
                            ? venta.getFechaHora().getClass().getName()
                            : "NULL")
            );

            String fecha;

            if (venta.getFechaHora() != null
                    && !venta.getFechaHora().isBlank()) {

                try {
                    java.time.LocalDateTime fechaHora =
                            java.time.LocalDateTime.parse(
                                    venta.getFechaHora()
                            );

                    fecha = fechaHora.format(
                            java.time.format.DateTimeFormatter.ofPattern(
                                    "dd/MM/yyyy HH:mm"
                            )
                    );

                } catch (Exception e) {

                    System.err.println(
                            "⚠️ No se pudo interpretar fechaHora: "
                                    + venta.getFechaHora()
                    );

                    fecha = venta.getFechaHora();
                }

            } else {

                fecha =
                        java.time.LocalDateTime.now().format(
                                java.time.format.DateTimeFormatter.ofPattern(
                                        "dd/MM/yyyy HH:mm"
                                )
                        );
            }

            // ================================================================
            // 👤 CLIENTE
            // ================================================================

            String cliente = "Consumidor Final";

            if (venta.getCliente() != null
                    && venta.getCliente().getNombre() != null
                    && !venta.getCliente().getNombre().isBlank()) {

                cliente = venta.getCliente().getNombre();
            }

            documento.add(
                    new Paragraph(
                            "Local: NAKEL",
                            fuenteSubTitulo
                    )
            );

            documento.add(
                    new Paragraph(
                            "Fecha: " + fecha,
                            fuenteNormal
                    )
            );

            documento.add(
                    new Paragraph(
                            "Cliente: " + cliente,
                            fuenteNormal
                    )
            );

            documento.add(
                    new Paragraph(
                            "Operación N°: "
                                    + String.format(
                                    "%08d",
                                    venta.getId()
                            ),
                            fuenteNormal
                    )
            );

            documento.add(new Paragraph(" "));

            // ================================================================
            // 📊 TABLA DE PRODUCTOS
            // ================================================================

            PdfPTable tabla;

            if (esTicketRegalo) {

                // ------------------------------------------------------------
                // 🎁 TICKET DE REGALO
                // Solo mostramos cantidad y descripción.
                // NO mostramos precios.
                // ------------------------------------------------------------

                tabla = new PdfPTable(2);
                tabla.setWidthPercentage(100);
                tabla.setWidths(
                        new float[]{1f, 5f}
                );

                tabla.addCell(
                        new PdfPCell(
                                new Phrase(
                                        "Cant",
                                        fuenteSubTitulo
                                )
                        )
                );

                tabla.addCell(
                        new PdfPCell(
                                new Phrase(
                                        "Descripción",
                                        fuenteSubTitulo
                                )
                        )
                );

            } else {

                // ------------------------------------------------------------
                // 🧾 VENTA NORMAL / PRESUPUESTO
                // Mostramos precios y subtotales.
                // ------------------------------------------------------------

                tabla = new PdfPTable(4);
                tabla.setWidthPercentage(100);
                tabla.setWidths(
                        new float[]{1f, 4f, 2f, 2f}
                );

                tabla.addCell(
                        new PdfPCell(
                                new Phrase(
                                        "Cant",
                                        fuenteSubTitulo
                                )
                        )
                );

                tabla.addCell(
                        new PdfPCell(
                                new Phrase(
                                        "Descripción",
                                        fuenteSubTitulo
                                )
                        )
                );

                tabla.addCell(
                        new PdfPCell(
                                new Phrase(
                                        "Precio Unit.",
                                        fuenteSubTitulo
                                )
                        )
                );

                tabla.addCell(
                        new PdfPCell(
                                new Phrase(
                                        "Subtotal",
                                        fuenteSubTitulo
                                )
                        )
                );
            }

            // ================================================================
            // 📦 DETALLES
            // ================================================================

            if (venta.getDetalles() != null) {

                for (DetalleVenta det : venta.getDetalles()) {

                    if (det == null
                            || det.getArticulo() == null) {
                        continue;
                    }

                    String nombreArticulo =
                            det.getArticulo().getNombre();

                    if (nombreArticulo == null
                            || nombreArticulo.isBlank()) {

                        nombreArticulo = "Artículo";
                    }

                    // Cantidad
                    tabla.addCell(
                            new Phrase(
                                    String.valueOf(
                                            det.getCantidad()
                                    ),
                                    fuenteNormal
                            )
                    );

                    // Descripción
                    tabla.addCell(
                            new Phrase(
                                    nombreArticulo,
                                    fuenteNormal
                            )
                    );

                    // --------------------------------------------------------
                    // 💰 PRECIOS
                    // Solo aparecen si NO es ticket de regalo.
                    // --------------------------------------------------------

                    if (!esTicketRegalo) {

                        tabla.addCell(
                                new Phrase(
                                        String.format(
                                                "$ %.2f",
                                                det.getPrecioUnitario()
                                        ),
                                        fuenteNormal
                                )
                        );

                        tabla.addCell(
                                new Phrase(
                                        String.format(
                                                "$ %.2f",
                                                det.getSubtotal()
                                        ),
                                        fuenteNormal
                                )
                        );
                    }
                }
            }

            documento.add(tabla);
            documento.add(new Paragraph(" "));

            // ================================================================
            // 💰 TOTAL / MENSAJE DE REGALO
            // ================================================================

            if (!esTicketRegalo) {

                Paragraph total =
                        new Paragraph(
                                "TOTAL: "
                                        + String.format(
                                        "$ %.2f",
                                        venta.getTotal()
                                ),
                                fuenteTitulo
                        );

                total.setAlignment(
                        Element.ALIGN_RIGHT
                );

                documento.add(total);

            } else {

                Paragraph mensajeRegalo =
                        new Paragraph(
                                "Conserve este ticket para realizar cambios.",
                                fuenteNormal
                        );

                mensajeRegalo.setAlignment(
                        Element.ALIGN_CENTER
                );

                documento.add(mensajeRegalo);
            }

            // ================================================================
            // ✅ FINALIZAR PDF
            // ================================================================

            documento.close();

            System.out.println(
                    "✅ ¡PDF A4 generado con éxito en "
                            + rutaDestino
                            + "!"
            );

        } catch (Exception e) {

            System.err.println(
                    "❌ Error al generar PDF A4: "
                            + e.getMessage()
            );

            e.printStackTrace();

            try {
                if (documento.isOpen()) {
                    documento.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    // ====================================================================
    // 🔥 2. COMPROBANTE DE CAMBIO / VALE
    // ====================================================================
    public static void generarComprobanteCambio(
            String rutaDestino,
            String nombreCliente,
            double saldo,
            String codigoVale
    ) {

        try {

            // Creamos un documento finito,
            // simulando un ticket de 80mm.
            Document document =
                    new Document(
                            new Rectangle(226, 400)
                    );

            document.setMargins(
                    10,
                    10,
                    10,
                    10
            );

            PdfWriter.getInstance(
                    document,
                    new FileOutputStream(rutaDestino)
            );

            document.open();

            // ================================================================
            // 🔤 TIPOGRAFÍAS
            // ================================================================

            Font fontTitulo =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            14
                    );

            Font fontTexto =
                    FontFactory.getFont(
                            FontFactory.HELVETICA,
                            10
                    );

            Font fontVale =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            16
                    );

            // ================================================================
            // 🏷️ ENCABEZADO
            // ================================================================

            Paragraph titulo =
                    new Paragraph(
                            "NAKEL SOFTWARE",
                            fontTitulo
                    );

            titulo.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(titulo);

            document.add(
                    new Paragraph(
                            "----------------------------------",
                            fontTexto
                    )
            );

            document.add(
                    new Paragraph(
                            "COMPROBANTE DE CAMBIO",
                            fontTitulo
                    )
            );

            document.add(
                    new Paragraph(
                            "Cliente: " + nombreCliente,
                            fontTexto
                    )
            );

            document.add(
                    new Paragraph(
                            "Saldo a favor: $"
                                    + String.format(
                                    "%.2f",
                                    saldo
                            ),
                            fontTexto
                    )
            );

            // ================================================================
            // 🎟️ VALE
            // ================================================================

            if (codigoVale != null
                    && !codigoVale.isEmpty()) {

                document.add(
                        new Paragraph(
                                "----------------------------------",
                                fontTexto
                        )
                );

                Paragraph subtituloVale =
                        new Paragraph(
                                "CÓDIGO DE VALE:",
                                fontTexto
                        );

                subtituloVale.setAlignment(
                        Element.ALIGN_CENTER
                );

                document.add(
                        subtituloVale
                );

                Paragraph pVale =
                        new Paragraph(
                                codigoVale,
                                fontVale
                        );

                pVale.setAlignment(
                        Element.ALIGN_CENTER
                );

                document.add(pVale);
            }

            // ================================================================
            // 📝 PIE
            // ================================================================

            document.add(
                    new Paragraph(
                            "----------------------------------",
                            fontTexto
                    )
            );

            Paragraph pie =
                    new Paragraph(
                            "¡Gracias por su visita!",
                            fontTexto
                    );

            pie.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(pie);

            document.close();

            System.out.println(
                    "✅ PDF Ticket Generado exitosamente en: "
                            + rutaDestino
            );

        } catch (Exception e) {

            System.err.println(
                    "❌ Error al generar el PDF de Cambio: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }
}
