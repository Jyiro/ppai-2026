package com.ppai.domain.model.cu28;

import java.time.LocalDateTime;

/**
 * Entidad Sesion — CU 28: Registrar Recepción de Bolsín.
 * Representa la sesión activa del usuario que ejecuta el CU (Dominio OO puro).
 */
public class Sesion {

    private final Usuario usuario;
    private final LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;

    public Sesion(Usuario usuario, LocalDateTime fechaHoraInicio) {
        this.usuario = usuario;
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = null;
    }

    /** Indica si la sesión continúa abierta (sin fechaHoraFin). */
    public boolean estaActiva() {
        return this.fechaHoraFin == null;
    }

    /** DS-CU28 paso 5: Retorna el usuario autenticado en la sesión activa. */
    public Usuario getUsuario() {
        return usuario;
    }

    /** DS-CU28 paso 6: Delega en Usuario#getCMEmpleado(). */
    public ComisionMedica getCMEmpleado() {
        return usuario != null ? usuario.getCMEmpleado() : null;
    }

    /** Retorna el empleado asociado al usuario logueado para auditoría de estados. */
    public Empleado getEmpleadoLogueado() {
        return usuario != null ? usuario.getEmpleado() : null;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(LocalDateTime fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }
}
