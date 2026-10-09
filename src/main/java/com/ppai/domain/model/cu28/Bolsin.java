package com.ppai.domain.model.cu28;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Bolsin — CU 28: Registrar Recepción de Bolsín.
 * Entidad central del caso de uso.
 * (Punto de extensión: Patrón State en domain/state/cu28/, Patrón Iterator para historial)
 */
public class Bolsin {

    private String numeroPrecinto;
    private ComisionMedica cmOrigen;
    private ComisionMedica cmDestino;
    private Remito remito;
    private List<CambioEstadoBolsin> cambiosDeEstado;

    public Bolsin(String numeroPrecinto, ComisionMedica cmOrigen, ComisionMedica cmDestino,
                  Remito remito, Estado estadoInicial, LocalDateTime fechaHoraInicio) {
        this.numeroPrecinto = numeroPrecinto;
        this.cmOrigen = cmOrigen;
        this.cmDestino = cmDestino;
        this.remito = remito;
        this.cambiosDeEstado = new ArrayList<>();
        this.cambiosDeEstado.add(new CambioEstadoBolsin(estadoInicial, fechaHoraInicio));
    }

    /** DS-CU28 loop sec. 2: Verifica que la CM destino coincida y el estado actual sea ENVIADO. */
    public boolean sosEnviado(ComisionMedica cmEmpleado) {
        if (!cmDestino.esTuCMDestino(cmEmpleado)) {
            return false;
        }
        CambioEstadoBolsin actual = obtenerCambioActual();
        return actual != null && actual.sosEnviado();
    }

    /** DS-CU28 loop sec. 3: Retorna el número de precinto. */
    public String getNumeroPrecinto() {
        return numeroPrecinto;
    }

    /** DS-CU28 loop sec. 3: Retorna la CM de origen. */
    public ComisionMedica getCMOrigen() {
        return cmOrigen;
    }

    /** DS-CU28 paso 14: Retorna el remito asociado. */
    public Remito obtenerInformacionRemito() {
        return remito;
    }

    /** DS-CU28 pasos 26-30 y loop sec. 8: Cierra estado actual, crea nuevo estado y orquesta aceptación de remito. */
    public void marcarRecibido(Estado estadoRecibidoCMDestino,
                               Estado estadoDocRecibidaAceptada,
                               LocalDateTime fechaHoraActual) {
        marcarRecibido(estadoRecibidoCMDestino, estadoDocRecibidaAceptada, fechaHoraActual, null);
    }

    public void marcarRecibido(Estado estadoRecibidoCMDestino,
                               Estado estadoDocRecibidaAceptada,
                               LocalDateTime fechaHoraActual,
                               Empleado empleado) {
        CambioEstadoBolsin actual = obtenerCambioActual();
        if (actual != null) {
            actual.setFechaHoraFin(fechaHoraActual);
        }

        crearNuevoCambioDeEstado(estadoRecibidoCMDestino, fechaHoraActual, empleado);

        if (remito != null && estadoDocRecibidaAceptada != null) {
            remito.aceptar(estadoDocRecibidaAceptada, fechaHoraActual);
        }
    }

    /** Marca el bolsín como recibido registrando fecha y empleado actuante. */
    public void marcarRecibido(Estado estadoRecibidoCMDestino,
                               LocalDateTime fechaHoraActual,
                               Empleado empleado) {
        CambioEstadoBolsin actual = obtenerCambioActual();
        if (actual != null) {
            actual.setFechaHoraFin(fechaHoraActual);
        }
        crearNuevoCambioDeEstado(estadoRecibidoCMDestino, fechaHoraActual, empleado);
    }

    /** DS-CU28 pasos 29-30: Crea y agrega el nuevo CambioEstadoBolsin. */
    public void crearNuevoCambioDeEstado(Estado estado, LocalDateTime fechaHoraInicio) {
        crearNuevoCambioDeEstado(estado, fechaHoraInicio, null);
    }

    public void crearNuevoCambioDeEstado(Estado estado, LocalDateTime fechaHoraInicio, Empleado empleado) {
        CambioEstadoBolsin nuevo = new CambioEstadoBolsin(estado, fechaHoraInicio, empleado);
        this.cambiosDeEstado.add(nuevo);
    }

    public CambioEstadoBolsin obtenerCambioActual() {
        for (CambioEstadoBolsin cambio : cambiosDeEstado) {
            if (cambio.sosActual()) {
                return cambio;
            }
        }
        return null;
    }

    public Estado getEstadoActual() {
        CambioEstadoBolsin actual = obtenerCambioActual();
        return actual != null ? actual.getEstado() : null;
    }

    public LocalDateTime getFechaEnvio() {
        if (!cambiosDeEstado.isEmpty()) {
            return cambiosDeEstado.get(0).getFechaHoraInicio();
        }
        return LocalDateTime.now();
    }

    public Remito getRemito() {
        return remito;
    }

    public ComisionMedica getCmDestino() {
        return cmDestino;
    }

    public List<CambioEstadoBolsin> getCambiosDeEstado() {
        return cambiosDeEstado;
    }

    @Override
    public String toString() {
        return "Bolsin{numeroPrecinto='" + numeroPrecinto
                + "', cmOrigen=" + cmOrigen + ", cmDestino=" + cmDestino + "}";
    }
}

