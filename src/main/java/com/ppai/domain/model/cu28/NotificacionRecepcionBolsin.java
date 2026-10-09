package com.ppai.domain.model.cu28;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Objeto de Notificación CU 29 (Include de CU 28).
 * Representa la notificación formal enviada a la Comisión Médica de origen
 * informando la recepción del bolsín y el estado final de remitos y documentaciones.
 */
public class NotificacionRecepcionBolsin {

    private final String destinatarioCM;
    private final String emailDestino;
    private final String numeroPrecinto;
    private final String numeroRemito;
    private final String estadoBolsin;
    private final String estadoRemito;
    private final String detalleDocumentaciones;
    private final LocalDateTime fechaHoraEnvio;
    private final String empleadoResponsable;

    public NotificacionRecepcionBolsin(String destinatarioCM,
                                       String emailDestino,
                                       String numeroPrecinto,
                                       String numeroRemito,
                                       String estadoBolsin,
                                       String estadoRemito,
                                       String detalleDocumentaciones,
                                       LocalDateTime fechaHoraEnvio,
                                       String empleadoResponsable) {
        this.destinatarioCM = destinatarioCM;
        this.emailDestino = emailDestino;
        this.numeroPrecinto = numeroPrecinto;
        this.numeroRemito = numeroRemito;
        this.estadoBolsin = estadoBolsin;
        this.estadoRemito = estadoRemito;
        this.detalleDocumentaciones = detalleDocumentaciones;
        this.fechaHoraEnvio = fechaHoraEnvio;
        this.empleadoResponsable = empleadoResponsable;
    }

    public String getDestinatarioCM() {
        return destinatarioCM;
    }

    public String getEmailDestino() {
        return emailDestino;
    }

    public String getNumeroPrecinto() {
        return numeroPrecinto;
    }

    public String getNumeroRemito() {
        return numeroRemito;
    }

    public String getEstadoBolsin() {
        return estadoBolsin;
    }

    public String getEstadoRemito() {
        return estadoRemito;
    }

    public String getDetalleDocumentaciones() {
        return detalleDocumentaciones;
    }

    public LocalDateTime getFechaHoraEnvio() {
        return fechaHoraEnvio;
    }

    public String getEmpleadoResponsable() {
        return empleadoResponsable;
    }

    public String getCuerpoCorreo() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return "DE: Sistema Central de Bolsines (CU 29)\n"
                + "PARA: " + destinatarioCM + " <" + emailDestino + ">\n"
                + "FECHA: " + fechaHoraEnvio.format(dtf) + "\n"
                + "ASUNTO: [Notificación de Recepción] Bolsín Precinto N° " + numeroPrecinto + "\n\n"
                + "Se ha registrado la recepción del bolsín con la siguiente información:\n"
                + "  • Precinto: " + numeroPrecinto + "\n"
                + "  • Remito Asociado: " + numeroRemito + " (" + estadoRemito + ")\n"
                + "  • Estado del Bolsín: " + estadoBolsin + "\n"
                + "  • Encargado actuante: " + empleadoResponsable + "\n\n"
                + "Detalle de Documentación recibida:\n"
                + detalleDocumentaciones + "\n\n"
                + "Esta es una notificación automática generada por el CU 29.";
    }
}
