package com.ppai.boundary.cu28;

import com.ppai.domain.model.cu28.NotificacionRecepcionBolsin;

/**
 * Boundary InterfazServidorCorreo — CU 28 / CU 29.
 * Representa la interfaz de comunicación con el Servidor de Correo Electrónico
 * para enviar las notificaciones a las Comisiones Médicas de origen.
 */
public class InterfazServidorCorreo {

    /**
     * Envía la notificación electrónica de recepción (CU 29).
     */
    public boolean enviarNotificacion(NotificacionRecepcionBolsin notificacion) {
        System.out.println("\n[CU29 / SERVIDOR-CORREO] ══════════════════════════════════════");
        System.out.println("[CU29] Enviando correo electrónico a: " + notificacion.getEmailDestino());
        System.out.println("[CU29] Asunto: Recepción de Bolsín " + notificacion.getNumeroPrecinto());
        System.out.println("[CU29] Estado informado: " + notificacion.getEstadoBolsin());
        System.out.println("[CU29] Correo transmitido exitosamente.");
        System.out.println("════════════════════════════════════════════════════════════════\n");
        return true;
    }
}
