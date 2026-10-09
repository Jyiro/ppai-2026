package com.ppai.domain.model.cu28;

/**
 * Entidad Estado — CU 28: Registrar Recepción de Bolsín.
 * Estado polivalente para Bolsín, Remito o Documentación.
 * (Punto de extensión: Patrón State en domain/state/cu28/)
 */
public class Estado {

    public static final String AMBITO_BOLSIN = "BOLSIN";
    public static final String AMBITO_REMITO = "REMITO";
    public static final String AMBITO_DOCUMENTACION = "DOCUMENTACION";

    public static final String NOMBRE_ENVIADO = "ENVIADO";
    public static final String NOMBRE_RECIBIDO_CM_DESTINO = "RECIBIDO_EN_CM_DESTINO";
    public static final String NOMBRE_RECIBIDO_Y_ACEPTADO = "RECIBIDO_Y_ACEPTADO";
    public static final String NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL = "RECIBIDO_Y_ACEPTADO_PARCIAL";
    public static final String NOMBRE_NO_RECIBIDA = "NO_RECIBIDA";
    public static final String NOMBRE_RECIBIDA_Y_RECHAZADA = "RECIBIDA_Y_RECHAZADA";
    public static final String NOMBRE_PARA_REDIRIGIR = "PARA_REDIRIGIR";
    public static final String NOMBRE_EN_BOLSIN_ENVIADO = "EN_BOLSIN_ENVIADO";

    private String nombre;
    private String ambito;

    public Estado(String nombre, String ambito) {
        this.nombre = nombre;
        this.ambito = ambito;
    }

    /** DS-CU28 loop sec. 2: Verifica si es estado ENVIADO de Bolsín. */
    public boolean esEnviado() {
        return NOMBRE_ENVIADO.equals(this.nombre);
    }

    /** DS-CU28 loop sec. 6: Verifica si el ámbito es BOLSIN. */
    public boolean esAmbitoBolsin() {
        return AMBITO_BOLSIN.equals(this.ambito);
    }

    /** DS-CU28 loop sec. 6: Verifica si es estado RECIBIDO_EN_CM_DESTINO. */
    public boolean esEstadoRecibidoEnCMDestino() {
        return NOMBRE_RECIBIDO_CM_DESTINO.equals(this.nombre);
    }

    /** DS-CU28 loop sec. 6: Verifica si es estado RECIBIDO_Y_ACEPTADO. */
    public boolean esRecibidoYAceptado() {
        return NOMBRE_RECIBIDO_Y_ACEPTADO.equals(this.nombre);
    }

    /** Verifica si es estado RECIBIDO_Y_ACEPTADO_PARCIAL (Remito en A3 / A4). */
    public boolean esRecibidoYAceptadoParcial() {
        return NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL.equals(this.nombre);
    }

    /** Verifica si es estado NO_RECIBIDA (Documentación en A3). */
    public boolean esNoRecibida() {
        return NOMBRE_NO_RECIBIDA.equals(this.nombre);
    }

    /** Verifica si es estado RECIBIDA_Y_RECHAZADA (Documentación en A4). */
    public boolean esRecibidaYRechazada() {
        return NOMBRE_RECIBIDA_Y_RECHAZADA.equals(this.nombre);
    }

    /** Verifica si es estado PARA_REDIRIGIR (Documentación en A5). */
    public boolean esParaRedirigir() {
        return NOMBRE_PARA_REDIRIGIR.equals(this.nombre);
    }

    /** DS-CU28 loop sec. 6: Verifica si el ámbito es REMITO. */
    public boolean esAmbitoRemito() {
        return AMBITO_REMITO.equals(this.ambito);
    }

    /** DS-CU28 loop sec. 6: Verifica si el ámbito es DOCUMENTACION. */
    public boolean esAmbitoDocumentacion() {
        return AMBITO_DOCUMENTACION.equals(this.ambito);
    }

    public String getNombre() {
        return nombre;
    }

    public String getAmbito() {
        return ambito;
    }

    /** Retorna un nombre legible y estilizado para la UI. */
    public String getNombreDisplay() {
        if (nombre == null) return "-";
        return switch (nombre) {
            case NOMBRE_ENVIADO -> "Enviado";
            case NOMBRE_RECIBIDO_CM_DESTINO -> "Recibido en CM Destino";
            case NOMBRE_RECIBIDO_Y_ACEPTADO -> "Recibido y aceptado";
            case NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL -> "Recibido parcial";
            case NOMBRE_NO_RECIBIDA -> "No recibida";
            case NOMBRE_RECIBIDA_Y_RECHAZADA -> "Recibida y rechazada";
            case NOMBRE_PARA_REDIRIGIR -> "Para redirigir";
            case NOMBRE_EN_BOLSIN_ENVIADO -> "En bolsín enviado";
            default -> nombre.replace("_", " ");
        };
    }

    @Override
    public String toString() {
        return getNombreDisplay();
    }
}

