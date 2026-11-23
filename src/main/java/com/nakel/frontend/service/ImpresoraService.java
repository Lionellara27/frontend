package com.nakel.frontend.service;

import javafx.scene.control.ChoiceDialog;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Impresión de tickets en la ticketera térmica.
 *
 * Antes se imprimía siempre en la impresora PREDETERMINADA de Windows y, si no había ninguna
 * o fallaba, no avisaba (la pantalla decía "ticket enviado" igual). Ahora:
 *  - La primera vez pregunta cuál es la impresora de tickets y la recuerda en esta PC.
 *  - Si algo falla lanza una excepción con un mensaje claro, para que la pantalla lo muestre.
 */
public class ImpresoraService {

    // Preferencias de Windows del usuario: sobreviven a reinstalar el sistema
    private static final Preferences PREFS = Preferences.userRoot().node("nakelsoftware");
    private static final String CLAVE_IMPRESORA = "impresoraTickets";

    /** Nombre de la impresora de tickets guardada, o null si todavía no se eligió. */
    public static String getImpresoraGuardada() {
        String nombre = PREFS.get(CLAVE_IMPRESORA, null);
        return (nombre == null || nombre.isBlank()) ? null : nombre;
    }

    private static PrintService buscarPorNombre(String nombre) {
        if (nombre == null) return null;
        for (PrintService ps : PrintServiceLookup.lookupPrintServices(null, null)) {
            if (ps.getName().equalsIgnoreCase(nombre)) return ps;
        }
        return null;
    }

    /**
     * Muestra la lista de impresoras instaladas para elegir la de tickets y la guarda.
     * Devuelve la elegida, o null si se canceló. Lanza excepción si no hay ninguna instalada.
     */
    public static PrintService elegirImpresora() {
        PrintService[] todas = PrintServiceLookup.lookupPrintServices(null, null);
        if (todas == null || todas.length == 0) {
            throw new IllegalStateException("No hay ninguna impresora instalada en esta computadora.");
        }

        List<String> nombres = new ArrayList<>();
        for (PrintService ps : todas) nombres.add(ps.getName());

        // Preseleccionamos la guardada; si no hay, la predeterminada de Windows
        String sugerida = getImpresoraGuardada();
        if (sugerida == null || !nombres.contains(sugerida)) {
            PrintService predeterminada = PrintServiceLookup.lookupDefaultPrintService();
            sugerida = (predeterminada != null && nombres.contains(predeterminada.getName()))
                    ? predeterminada.getName()
                    : nombres.get(0);
        }

        ChoiceDialog<String> dialogo = new ChoiceDialog<>(sugerida, nombres);
        dialogo.setTitle("Impresora de tickets");
        dialogo.setHeaderText("¿En qué impresora salen los tickets?");
        dialogo.setContentText("Elegí la ticketera:");

        Optional<String> elegida = dialogo.showAndWait();
        if (elegida.isEmpty()) return null;

        PREFS.put(CLAVE_IMPRESORA, elegida.get());
        return buscarPorNombre(elegida.get());
    }

    // Las ticketeras térmicas no entienden UTF-8: mandamos texto simple sin acentos ni símbolos raros
    private static String aTextoSimple(String texto) {
        String sinSimbolos = texto
                .replace("°", "o").replace("º", "o").replace("ª", "a")
                .replace("¡", "").replace("¿", "")
                .replace("ñ", "n").replace("Ñ", "N");
        String sinAcentos = Normalizer.normalize(sinSimbolos, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.replaceAll("[^\\x00-\\x7F]", "?");
    }

    /**
     * Imprime texto plano en la impresora de tickets.
     * Devuelve false si el usuario canceló la elección de impresora.
     * Lanza IllegalStateException con un mensaje claro si no se pudo imprimir.
     */
    public static boolean imprimirTexto(String texto) {
        PrintService impresora = buscarPorNombre(getImpresoraGuardada());

        // Si nunca se eligió, o la guardada ya no existe (se cambió la ticketera), preguntamos
        if (impresora == null) {
            impresora = elegirImpresora();
            if (impresora == null) return false;
        }

        try {
            System.out.println("🖨️ Mandando comprobante a: " + impresora.getName());

            byte[] bytes = aTextoSimple(texto).getBytes(StandardCharsets.US_ASCII);
            Doc doc = new SimpleDoc(bytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
            DocPrintJob job = impresora.createPrintJob();
            job.print(doc, null);

            System.out.println("✅ ¡Impresión finalizada!");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("No se pudo imprimir en \"" + impresora.getName() + "\".\n"
                    + "Revisá que esté encendida, conectada y con papel.\n\nDetalle: " + e.getMessage());
        }
    }
}
