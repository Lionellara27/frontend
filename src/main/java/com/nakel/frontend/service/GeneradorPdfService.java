package com.nakel.frontend.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.nakel.frontend.model.DetalleVenta;
import com.nakel.frontend.model.Venta;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GeneradorPdfService {

    public void generarPdf(Venta venta, String rutaDestino) {
        // Creamos un documento tamaño A4 con márgenes
        Document documento = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(documento, new FileOutputStream(rutaDestino));
            documento.open();

            // 🎨 Definimos las fuentes (Letras)
            Font fuenteTitulo = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Font fuenteSubTitulo = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font fuenteNormal = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

            // 🏷️ TÍTULO DINÁMICO (Acá aplicamos la regla de negocio del Presupuesto)
            String tipo = (venta.getTipoComprobante() != null && venta.getTipoComprobante().equalsIgnoreCase("Presupuesto"))
                    ? "PRESUPUESTO"
                    : "TICKET DE VENTA (NO FISCAL)";

            Paragraph titulo = new Paragraph(tipo, fuenteTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(titulo);
            documento.add(new Paragraph(" ")); // Salto de línea

            // 📅 Fechas y Datos
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            String fecha = (venta.getFechaHora() != null) ? sdf.format(venta.getFechaHora()) : sdf.format(new Date());
            String cliente = (venta.getCliente() != null && venta.getCliente().getNombre() != null) ? venta.getCliente().getNombre() : "Consumidor Final";

            documento.add(new Paragraph("Local: NAKEL", fuenteSubTitulo));
            documento.add(new Paragraph("Fecha: " + fecha, fuenteNormal));
            documento.add(new Paragraph("Cliente: " + cliente, fuenteNormal));
            documento.add(new Paragraph("Operación N°: " + String.format("%08d", venta.getId()), fuenteNormal));
            documento.add(new Paragraph(" ")); // Salto de línea

            // 📊 Tabla de Productos (4 Columnas)
            PdfPTable tabla = new PdfPTable(4);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{1f, 4f, 2f, 2f}); // Proporción de los anchos de columna

            // Cabeceras de la tabla
            tabla.addCell(new PdfPCell(new Phrase("Cant", fuenteSubTitulo)));
            tabla.addCell(new PdfPCell(new Phrase("Descripción", fuenteSubTitulo)));
            tabla.addCell(new PdfPCell(new Phrase("Precio Unit.", fuenteSubTitulo)));
            tabla.addCell(new PdfPCell(new Phrase("Subtotal", fuenteSubTitulo)));

            // Llenamos la tabla con el for
            if (venta.getDetalles() != null) {
                for (DetalleVenta det : venta.getDetalles()) {
                    tabla.addCell(new Phrase(String.valueOf(det.getCantidad()), fuenteNormal));
                    tabla.addCell(new Phrase(det.getArticulo().getNombre(), fuenteNormal));
                    tabla.addCell(new Phrase(String.format("$ %.2f", det.getPrecioUnitario()), fuenteNormal));
                    tabla.addCell(new Phrase(String.format("$ %.2f", det.getSubtotal()), fuenteNormal));
                }
            }
            documento.add(tabla);
            documento.add(new Paragraph(" "));

            // 💰 TOTAL
            Paragraph total = new Paragraph("TOTAL: " + String.format("$ %.2f", venta.getTotal()), fuenteTitulo);
            total.setAlignment(Element.ALIGN_RIGHT);
            documento.add(total);

            documento.close();
            System.out.println("✅ ¡PDF generado con éxito en " + rutaDestino + "!");

        } catch (Exception e) {
            System.err.println("❌ Error al generar PDF: " + e.getMessage());
        }
    }
}