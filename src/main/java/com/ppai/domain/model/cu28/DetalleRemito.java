package com.ppai.domain.model.cu28;

import java.time.LocalDateTime;

/**
 * Entidad DetalleRemito — CU 28: Registrar Recepción de Bolsín.
 * Asocia una Documentación a un Remito.
 */
public class DetalleRemito {

    private Documentacion documentacion;

    public DetalleRemito(Documentacion documentacion) {
        this.documentacion = documentacion;
    }

    /** DS-CU28 loop sec. 4: Retorna la documentación asociada a este detalle. */
    public Documentacion getDocumentacion() {
        return documentacion;
    }

    /** DS-CU28 loop sec. 8: Delega la aceptación en Documentacion#aceptar(). */
    public void aceptar(Estado estadoRecibidoYAceptado, LocalDateTime fechaHoraActual) {
        documentacion.aceptar(estadoRecibidoYAceptado, fechaHoraActual);
    }

    public Documentacion getDocumentacionAsociada() {
        return documentacion;
    }

    @Override
    public String toString() {
        return "DetalleRemito{documentacion=" + documentacion + "}";
    }
}

