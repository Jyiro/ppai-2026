package com.ppai.boundary.cu29;

/**
 * Boundary InterfazServidorCorreo — CU 29: Notificar Recepción de Bolsín.
 * Recibe exclusivamente primitivos String (BCE estricto, cero imports de dominio).
 */
public class InterfazServidorCorreo {

    /**
     * Envía la notificación electrónica de recepción a la Comisión Médica de origen.
     */
    public boolean enviarNotificacion(String emailDestino, String asunto, String cuerpo) {
        System.out.println("\n[SERVIDOR-CORREO] ═════════════════════════════════════════════");
        System.out.println("[CORREO] Para: " + emailDestino);
        System.out.println("[CORREO] Asunto: " + asunto);
        System.out.println("[CORREO] Longitud del mensaje: " + (cuerpo != null ? cuerpo.length() : 0) + " caracteres");
        System.out.println("[CORREO] Correo transmitido exitosamente.");
        System.out.println("════════════════════════════════════════════════════════════════\n");
        return true;
    }
}
