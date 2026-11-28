package com.ppai.controller.cu29;

import com.ppai.boundary.cu29.InterfazServidorCorreo;

/**
 * Controlador GestorNotificarRecepcionBolsin — CU 29: Notificar Recepción de Bolsín
 * (<<includes>> desde CU 28).
 * Recibe exclusivamente datos primitivos / String de la recepción ya confirmada y persistida,
 * arma la notificación electrónica completa (multi-remito) y delega el envío en InterfazServidorCorreo.
 */
public class GestorNotificarRecepcionBolsin {

    private static final String REMITENTE_SISTEMA = "Sistema de Bolsines — SRT";

    private final InterfazServidorCorreo servidorCorreo;

    public GestorNotificarRecepcionBolsin() {
        this(new InterfazServidorCorreo());
    }

    public GestorNotificarRecepcionBolsin(InterfazServidorCorreo servidorCorreo) {
        this.servidorCorreo = servidorCorreo;
    }

    /**
     * Construye y envía la notificación de recepción para todos los remitos y documentaciones del bolsín.
     *
     * @return Arreglo con los datos del comprobante de notificación:
     *         [0]=cmOrigenNombre, [1]=emailOrigen, [2]=remitente, [3]=asunto, [4]=fechaHoraFormateada,
     *         [5]=cuerpo, [6]=estadoBolsin, [7]=resumenRemitosTexto, [8]=numeroPrecinto, [9]=empleadoResponsable
     */
    public String[] notificarRecepcionBolsin(
            int numeroPrecinto,
            String cmOrigenNombre,
            String emailOrigen,
            String estadoBolsin,
            String[][] resumenRemitos,
            String[][] detalleDocs,
            String fechaHoraFormateada,
            String empleadoResponsable) {

        String precintoTxt = String.valueOf(numeroPrecinto);
        String destinatario = (cmOrigenNombre != null) ? cmOrigenNombre : "";
        String email = (emailOrigen != null) ? emailOrigen : "";
        String estadoBolsinTxt = (estadoBolsin != null) ? estadoBolsin : "";
        String fechaTxt = (fechaHoraFormateada != null) ? fechaHoraFormateada : "";
        String responsable = (empleadoResponsable != null && !empleadoResponsable.isBlank())
                ? empleadoResponsable
                : "Encargado de Bolsines";

        StringBuilder remitosResumen = new StringBuilder();
        StringBuilder remitosBloque = new StringBuilder();
        if (resumenRemitos != null && resumenRemitos.length > 0) {
            for (int i = 0; i < resumenRemitos.length; i++) {
                String[] rem = resumenRemitos[i];
                String nroRem = (rem != null && rem.length > 0) ? rem[0] : "-";
                String estRem = (rem != null && rem.length > 1) ? rem[1] : "-";
                if (i > 0) {
                    remitosResumen.append(", ");
                }
                remitosResumen.append(nroRem).append(" (").append(estRem).append(")");
                remitosBloque.append("    • Remito N° ").append(nroRem)
                        .append(" ── Estado: ").append(estRem).append("\n");
            }
        } else {
            remitosResumen.append("-");
            remitosBloque.append("    • Sin remitos asociados\n");
        }

        StringBuilder docsBloque = new StringBuilder();
        if (detalleDocs != null && detalleDocs.length > 0) {
            for (String[] doc : detalleDocs) {
                String nroRem = (doc != null && doc.length > 0) ? doc[0] : "-";
                String tipoDoc = (doc != null && doc.length > 1) ? doc[1] : "-";
                String asuntoDoc = (doc != null && doc.length > 2) ? doc[2] : "-";
                String estadoDoc = (doc != null && doc.length > 3) ? doc[3] : "-";
                docsBloque.append("    • [Remito ").append(nroRem).append("] [")
                        .append(tipoDoc).append("] ")
                        .append(asuntoDoc)
                        .append(" ── Estado: ").append(estadoDoc).append("\n");
            }
        } else {
            docsBloque.append("    • Sin documentaciones registradas\n");
        }

        String asunto = "Notificación de Recepción — Bolsín Precinto N° " + precintoTxt;
        String cuerpo = "DE: " + REMITENTE_SISTEMA + "\n"
                + "PARA: " + destinatario + " <" + email + ">\n"
                + "FECHA: " + fechaTxt + "\n"
                + "ASUNTO: " + asunto + "\n\n"
                + "Se ha registrado la recepción del bolsín con la siguiente información:\n"
                + "  • Precinto: " + precintoTxt + "\n"
                + "  • Estado del Bolsín: " + estadoBolsinTxt + "\n"
                + "  • Encargado actuante: " + responsable + "\n\n"
                + "Remitos asociados:\n"
                + remitosBloque + "\n"
                + "Detalle de Documentación recibida:\n"
                + docsBloque + "\n"
                + "Notificación automática emitida por el Sistema de Seguimiento de Bolsines.";

        servidorCorreo.enviarNotificacion(email, asunto, cuerpo);

        return new String[]{
                destinatario,
                email,
                REMITENTE_SISTEMA,
                asunto,
                fechaTxt,
                cuerpo,
                estadoBolsinTxt,
                remitosResumen.toString(),
                precintoTxt,
                responsable
        };
    }
}
