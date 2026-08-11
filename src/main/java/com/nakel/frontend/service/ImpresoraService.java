package com.nakel.frontend.service;

import com.nakel.frontend.model.DetalleVenta;
import com.nakel.frontend.model.Venta;
import javax.print.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImpresoraService {

    // ====================================================================
    // 🟢 1. TU MÉTODO ORIGINAL (Para imprimir el Ticket de Venta normal)
    // ====================================================================
    public void imprimirTicket(Venta venta) {
        String textoTicket = armarDisenoTicket(venta);

        try {
            // Le decimos a Java que vamos a mandar un texto plano a imprimir
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(textoTicket.getBytes(), flavor, null);

            // 🔥 BUSCA LA IMPRESORA PREDETERMINADA DE WINDOWS
            PrintService impresoraPredeterminada = PrintServiceLookup.lookupDefaultPrintService();

            if (impresoraPredeterminada != null) {
                System.out.println("🖨️ Mandando ticket a: " + impresoraPredeterminada.getName());
                DocPrintJob job = impresoraPredeterminada.createPrintJob();
                job.print(doc, null);
                System.out.println("✅ ¡Impresión finalizada!");
            } else {
                System.out.println("❌ No hay ninguna impresora instalada/predeterminada en Windows.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error al imprimir: " + e.getMessage());
        }
    }

    // 🎨 EL DISEÑO DEL TICKET (Formato 58mm/80mm)
    private String armarDisenoTicket(Venta venta) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String fecha = (venta.getFechaHora() != null) ? sdf.format(venta.getFechaHora()) : sdf.format(new Date());

        sb.append("================================\n");
        sb.append("         NAKEL SOFTWARE         \n"); // Cambialo por el nombre del local
        sb.append("================================\n");
        sb.append("Fecha: ").append(fecha).append("\n");

        String cliente = (venta.getCliente() != null && venta.getCliente().getNombre() != null)
                ? venta.getCliente().getNombre()
                : "Consumidor Final";
        sb.append("Cliente: ").append(cliente).append("\n");
        sb.append("--------------------------------\n");
        sb.append("CANT  DESCRIPCION      SUBTOTAL \n");
        sb.append("--------------------------------\n");

        // Recorremos los productos
        if (venta.getDetalles() != null) {
            for (DetalleVenta det : venta.getDetalles()) {
                String nombre = det.getArticulo().getNombre();
                // Si el nombre es muy largo, lo cortamos para que no descontrole el ticket
                if (nombre.length() > 14) {
                    nombre = nombre.substring(0, 14);
                }
                // Formato: 2 espacios para cantidad, 14 para nombre, 7 para plata
                sb.append(String.format("%-4d %-14s $%7.2f\n", det.getCantidad(), nombre, det.getSubtotal()));
            }
        }

        sb.append("--------------------------------\n");
        sb.append(String.format("TOTAL:                 $%7.2f\n", venta.getTotal()));
        sb.append("================================\n");
        sb.append("    ¡Gracias por tu compra!     \n");

        // ✂️ Estos saltos de línea son clave para que la impresora saque el papel
        // lo suficiente para que la clienta lo pueda cortar
        sb.append("\n\n\n\n\n");

        return sb.toString();
    }

    // ====================================================================
    // 🔥 2. EL NUEVO MÉTODO ESTÁTICO (Para imprimir el Vale desde el Modal)
    // ====================================================================
    public static void imprimirTexto(String texto) {
        try {
            PrintService impresora = PrintServiceLookup.lookupDefaultPrintService();

            if (impresora == null) {
                System.out.println("❌ No se encontró ninguna impresora predeterminada en Windows.");
                return;
            }

            System.out.println("🖨️ Mandando comprobante a: " + impresora.getName());

            // Usamos UTF-8 para que los acentos y símbolos salgan bien
            byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
            Doc doc = new SimpleDoc(bytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
            DocPrintJob job = impresora.createPrintJob();

            job.print(doc, null);
            System.out.println("✅ ¡Impresión de texto libre finalizada!");

        } catch (Exception e) {
            System.err.println("❌ Error al imprimir texto: " + e.getMessage());
            e.printStackTrace();
        }
    }
}