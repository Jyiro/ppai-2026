package com.ppai.domain.model.cu28;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entidad Documentacion — CU 28: Registrar Recepción de Bolsín.
 * Representa un documento adjunto en un DetalleRemito.
 * (Punto de extensión: Patrón State en domain/state/cu28/)
 */
@Entity
@Table(name = "documentacion")
public class Documentacion {

    @Id
    private Long id;

    @Column(name = "asunto", nullable = false)
    private String asunto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_documento_id", nullable = false)
    private TipoDocumento tipoDocumento;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "documentacion_id")
    @OrderColumn(name = "orden")
    private List<CambioEstadoDocumentacion> cambiosDeEstado = new ArrayList<>();

    protected Documentacion() {
    }

    public Documentacion(Long id, String asunto, TipoDocumento tipoDocumento,
                         Estado estadoInicial, LocalDateTime fechaHoraInicio) {
        this.id = id;
        this.asunto = asunto;
        this.tipoDocumento = tipoDocumento;
        this.cambiosDeEstado = new ArrayList<>();
        this.cambiosDeEstado.add(new CambioEstadoDocumentacion(estadoInicial, fechaHoraInicio));
    }

    /** DS-CU28 loop interno sec. 4: Retorna el asunto del documento. */
    public String getAsunto() {
        return asunto;
    }

    /** DS-CU28 loop interno sec. 4: Delega en TipoDocumento#getNombre(). */
    public String mostrarTipoDocumentacion() {
        return tipoDocumento.getNombre();
    }

    /** DS-CU28 loop sec. 8: Cierra el estado actual y crea el nuevo cambio de estado (Flujo básico y docs conformes). */
    public void aceptar(Estado estadoRecibidoYAceptado, LocalDateTime fechaHoraActual) {
        aceptar(estadoRecibidoYAceptado, fechaHoraActual, null);
    }

    public void aceptar(Estado estadoRecibidoYAceptado, LocalDateTime fechaHoraActual, Empleado empleado) {
        cerrarCambioActual(fechaHoraActual);
        newCambioEstadoDocumentacion(estadoRecibidoYAceptado, fechaHoraActual, empleado);
    }

    /** A3: Marca la documentación como NO_RECIBIDA. */
    public void marcarNoRecibida(Estado estadoNoRecibida, LocalDateTime fechaHoraActual, Empleado empleado) {
        cerrarCambioActual(fechaHoraActual);
        newCambioEstadoDocumentacion(estadoNoRecibida, fechaHoraActual, empleado);
    }

    /** A4: Marca la documentación como RECIBIDA_Y_RECHAZADA (no corresponde al destino). */
    public void rechazar(Estado estadoRechazado, LocalDateTime fechaHoraActual, Empleado empleado) {
        cerrarCambioActual(fechaHoraActual);
        newCambioEstadoDocumentacion(estadoRechazado, fechaHoraActual, empleado);
    }

    /** A5: Marca la documentación como PARA_REDIRIGIR (destino es CMC). */
    public void paraRedirigir(Estado estadoParaRedirigir, LocalDateTime fechaHoraActual, Empleado empleado) {
        cerrarCambioActual(fechaHoraActual);
        newCambioEstadoDocumentacion(estadoParaRedirigir, fechaHoraActual, empleado);
    }

    private void cerrarCambioActual(LocalDateTime fechaHoraActual) {
        CambioEstadoDocumentacion actual = obtenerCambioActual();
        if (actual != null) {
            actual.setFechaHoraFin(fechaHoraActual);
        }
    }

    /** DS-CU28 loop sec. 8: Crea y agrega el nuevo CambioEstadoDocumentacion. */
    public void newCambioEstadoDocumentacion(Estado estado, LocalDateTime fechaHoraInicio) {
        newCambioEstadoDocumentacion(estado, fechaHoraInicio, null);
    }

    public void newCambioEstadoDocumentacion(Estado estado, LocalDateTime fechaHoraInicio, Empleado empleado) {
        CambioEstadoDocumentacion nuevo = new CambioEstadoDocumentacion(estado, fechaHoraInicio, empleado);
        this.cambiosDeEstado.add(nuevo);
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

    public Long getId() {
        return id;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public List<CambioEstadoDocumentacion> getCambiosDeEstado() {
        return cambiosDeEstado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Documentacion that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Documentacion{id=" + id + ", asunto='" + asunto
                + "', tipo=" + tipoDocumento + "}";
    }
}

