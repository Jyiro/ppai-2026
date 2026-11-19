package com.ppai.domain.model.cu28;

import jakarta.persistence.CascadeType;
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
 * Entidad DetalleRemito — CU 28: Registrar Recepción de Bolsín.
 * Asocia una Documentación a un Remito.
 */
@Entity
@Table(name = "detalle_remito")
public class DetalleRemito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "documentacion_id", nullable = false)
    private Documentacion documentacion;

    protected DetalleRemito() {
    }

    public DetalleRemito(Documentacion documentacion) {
        this.documentacion = documentacion;
    }

    public Long getId() {
        return id;
    }

    /** DS-CU28 loop sec. 4: Retorna la documentación asociada a este detalle. */
    public Documentacion getDocumentacion() {
        return documentacion;
    }

    /**
     * DS-CU28 loop sec. 4: Retorna el asunto y tipo de documentación (método nuevo de diseño).
     *
     * @return Arreglo con [asunto, tipoDocumento].
     */
    public String[] obtenerAsuntoYTipoDoc() {
        if (documentacion == null) {
            return new String[]{"", ""};
        }
        return new String[]{
                documentacion.getAsunto(),
                documentacion.mostrarTipoDocumentacion()
        };
    }

    /** DS-CU28 loop sec. 8: Delega la aceptación en Documentacion#aceptar(). */
    public void aceptar(Estado estadoRecibidoYAceptado, LocalDateTime fechaHoraActual) {
        aceptar(estadoRecibidoYAceptado, fechaHoraActual, null);
    }

    /** DS-CU28 loop sec. 8: Delega la aceptación en Documentacion#aceptar() registrando empleado. */
    public void aceptar(Estado estadoRecibidoYAceptado, LocalDateTime fechaHoraActual, Empleado empleado) {
        if (documentacion != null) {
            documentacion.aceptar(estadoRecibidoYAceptado, fechaHoraActual, empleado);
        }
    }

    public Documentacion getDocumentacionAsociada() {
        return documentacion;
    }

    @Override
    public String toString() {
        return "DetalleRemito{documentacion=" + documentacion + "}";
    }
}

