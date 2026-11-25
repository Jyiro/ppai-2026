package com.ppai.domain.model.cu28;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entidad Documentacion — CU 28: Registrar Recepción de Bolsín.
 * Representa un documento adjunto en un DetalleRemito (Dominio OO puro).
 */
public class Documentacion {

    private final int numero;
    private final String asunto;
    private final LocalDateTime fechaPase;
    private final TipoDocumento tipoDocumento;
    private final ComisionMedica cmOrigen;
    private final List<CambioEstadoDocumentacion> cambiosDeEstado;

    public Documentacion(int numero,
                         String asunto,
                         LocalDateTime fechaPase,
                         TipoDocumento tipoDocumento,
                         ComisionMedica cmOrigen,
                         Estado estadoInicial,
                         LocalDateTime fechaHoraInicio,
                         Empleado empleadoInicial) {
        this.numero = numero;
        this.asunto = asunto;
        this.fechaPase = fechaPase;
        this.tipoDocumento = tipoDocumento;
        this.cmOrigen = cmOrigen;
        this.cambiosDeEstado = new ArrayList<>();
        if (estadoInicial != null) {
            this.cambiosDeEstado.add(new CambioEstadoDocumentacion(fechaHoraInicio, estadoInicial, empleadoInicial));
        }
    }

    public int getNumero() {
        return numero;
    }

    /** DS-CU28 loop interno sec. 4: Retorna el asunto del documento. */
    public String getAsunto() {
        return asunto;
    }

    public LocalDateTime getFechaPase() {
        return fechaPase;
    }

    /** DS-CU28 loop interno sec. 4: Delega en TipoDocumento#getNombre(). */
    public String mostrarTipoDocumentacion() {
        return tipoDocumento != null ? tipoDocumento.getNombre() : "";
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public ComisionMedica getCmOrigen() {
        return cmOrigen;
    }

    public List<CambioEstadoDocumentacion> getCambiosDeEstado() {
        return Collections.unmodifiableList(cambiosDeEstado);
    }

    /** DS-CU28 loop sec. 8: Cierra el estado actual y pasa a Recibida y aceptada. */
    public void aceptar(LocalDateTime fechaHoraActual, Estado estadoRecibidaYAceptada, Empleado empleado) {
        registrarTransicion(fechaHoraActual, estadoRecibidaYAceptada, empleado);
    }

    /** A3 / Máquina de estados: Marca la documentación como No recibida. */
    public void marcarNoRecibida(LocalDateTime fechaHoraActual, Estado estadoNoRecibida, Empleado empleado) {
        registrarTransicion(fechaHoraActual, estadoNoRecibida, empleado);
    }

    /** A4 / Máquina de estados: Marca la documentación como Recibida y rechazada. */
    public void marcarRecibidaRechazada(LocalDateTime fechaHoraActual, Estado estadoRecibidaYRechazada, Empleado empleado) {
        registrarTransicion(fechaHoraActual, estadoRecibidaYRechazada, empleado);
    }

    /** A5 / Máquina de estados: Marca la documentación como Para redirigir (solo CMC). */
    public void marcarParaRedirigir(LocalDateTime fechaHoraActual, Estado estadoParaRedirigir, Empleado empleado) {
        registrarTransicion(fechaHoraActual, estadoParaRedirigir, empleado);
    }

    /** DS-CU28 loop sec. 8: Crea y agrega el nuevo CambioEstadoDocumentacion. */
    public void newCambioEstadoDocumentacion(LocalDateTime fechaHoraInicio, Estado estado, Empleado empleado) {
        CambioEstadoDocumentacion nuevo = new CambioEstadoDocumentacion(fechaHoraInicio, estado, empleado);
        this.cambiosDeEstado.add(nuevo);
    }

    private void registrarTransicion(LocalDateTime fechaHoraActual, Estado nuevoEstado, Empleado empleado) {
        CambioEstadoDocumentacion actual = obtenerCambioActual();
        if (actual != null) {
            actual.setFechaHoraFin(fechaHoraActual);
        }
        newCambioEstadoDocumentacion(fechaHoraActual, nuevoEstado, empleado);
    }

    public CambioEstadoDocumentacion obtenerCambioActual() {
        for (CambioEstadoDocumentacion cambio : cambiosDeEstado) {
            if (cambio.sosActual()) {
                return cambio;
            }
        }
        return null;
    }

    /** Retorna el estado vigente de la documentación. */
    public Estado getEstadoActual() {
        CambioEstadoDocumentacion actual = obtenerCambioActual();
        return actual != null ? actual.getEstado() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Documentacion that)) return false;
        return numero == that.numero;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    @Override
    public String toString() {
        return "Documentacion{numero=" + numero + ", asunto='" + asunto
                + "', tipo=" + tipoDocumento + "}";
    }
}
