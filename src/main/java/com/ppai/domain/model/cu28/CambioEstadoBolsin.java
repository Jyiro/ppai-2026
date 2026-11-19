package com.ppai.domain.model.cu28;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Entidad CambioEstadoBolsin — CU 28: Registrar Recepción de Bolsín.
 * Registra el historial de estados de un Bolsín.
 * (Punto de extensión: Patrón State en domain/state/cu28/)
 */
@Entity
@Table(name = "cambio_estado_bolsin")
public class CambioEstadoBolsin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin")
    private LocalDateTime fechaHoraFin;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empleado_id")
    private Empleado empleado;

    protected CambioEstadoBolsin() {
    }

    public CambioEstadoBolsin(Estado estado, LocalDateTime fechaHoraInicio) {
        this(estado, fechaHoraInicio, null);
    }

    public CambioEstadoBolsin(Estado estado, LocalDateTime fechaHoraInicio, Empleado empleado) {
        this.estado = estado;
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = null;
        this.empleado = empleado;
    }

    public Long getId() {
        return id;
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

