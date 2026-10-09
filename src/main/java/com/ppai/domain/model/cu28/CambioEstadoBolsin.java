package com.ppai.domain.model.cu28;

import java.time.LocalDateTime;

/**
 * Entidad CambioEstadoBolsin — CU 28: Registrar Recepción de Bolsín.
 * Registra el historial de estados de un Bolsín.
 * (Punto de extensión: Patrón State en domain/state/cu28/)
 */
public class CambioEstadoBolsin {

    private Estado estado;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private Empleado empleado;

    public CambioEstadoBolsin(Estado estado, LocalDateTime fechaHoraInicio) {
        this(estado, fechaHoraInicio, null);
    }

    public CambioEstadoBolsin(Estado estado, LocalDateTime fechaHoraInicio, Empleado empleado) {
        this.estado = estado;
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = null;
        this.empleado = empleado;
    }

    /** DS-CU28 paso 27 / loop sec. 2: Verifica si este cambio sigue vigente. */
    public boolean sosActual() {
        return this.fechaHoraFin == null;
    }

    /** DS-CU28 loop sec. 2: Delega en Estado#esEnviado(). */
    public boolean sosEnviado() {
        return estado.esEnviado();
    }

    /** DS-CU28 paso 28: Cierra el estado actual registrando la fecha/hora de fin. */
    public void setFechaHoraFin(LocalDateTime fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }

    public Estado getEstado() {
        return estado;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    @Override
    public String toString() {
        return "CambioEstadoBolsin{estado=" + estado + ", inicio=" + fechaHoraInicio
                + ", fin=" + fechaHoraFin + ", empleado=" + (empleado != null ? empleado.getNombre() : "N/A") + "}";
    }
}

