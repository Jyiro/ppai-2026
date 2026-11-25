package com.ppai.domain.model.cu28;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Entidad DetalleRemito — CU 28: Registrar Recepción de Bolsín.
 * Asocia una Documentación a un Remito y su área destino en CMC (Dominio OO puro).
 */
public class DetalleRemito {

    private final Documentacion documentacion;
    private final String areaCMCDestino;

    public DetalleRemito(Documentacion documentacion, String areaCMCDestino) {
        this.documentacion = documentacion;
        this.areaCMCDestino = areaCMCDestino;
    }

    public Documentacion getDocumentacion() {
        return documentacion;
    }

    public String getAreaCMCDestino() {
        return areaCMCDestino;
    }

    /**
     * DS-CU28 loop sec. 4: Retorna el asunto y tipo de documentación.
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
    public void aceptar(LocalDateTime fechaHoraActual, Estado estadoRecibidaYAceptada, Empleado empleado) {
        if (documentacion != null) {
            documentacion.aceptar(fechaHoraActual, estadoRecibidaYAceptada, empleado);
        }
    }

    /**
     * Flujo alternativo A3: Marca como No recibida si su número figura entre las no conformes,
     * o Aceptada en caso contrario.
     *
     * @return true si este detalle fue marcado como faltante (No recibida).
     */
    public boolean registrarFaltaDocumentacion(LocalDateTime fechaHoraActual,
                                                Estado estadoAceptada,
                                                Estado estadoNoRecibida,
                                                Set<Integer> numerosDocsNoConformes,
                                                Empleado empleado) {
        if (documentacion == null) {
            return false;
        }
        if (numerosDocsNoConformes != null && numerosDocsNoConformes.contains(documentacion.getNumero())) {
            documentacion.marcarNoRecibida(fechaHoraActual, estadoNoRecibida, empleado);
            return true;
        }
        documentacion.aceptar(fechaHoraActual, estadoAceptada, empleado);
        return false;
    }

    /**
     * Flujo alternativo A4: Marca como Recibida y rechazada si su número figura entre las erróneas,
     * o Aceptada en caso contrario.
     *
     * @return true si este detalle fue marcado como destino erróneo (Recibida y rechazada).
     */
    public boolean registrarDestinoErroneo(LocalDateTime fechaHoraActual,
                                           Estado estadoAceptada,
                                           Estado estadoRechazada,
                                           Set<Integer> numerosDocsNoConformes,
                                           Empleado empleado) {
        if (documentacion == null) {
            return false;
        }
        if (numerosDocsNoConformes != null && numerosDocsNoConformes.contains(documentacion.getNumero())) {
            documentacion.marcarRecibidaRechazada(fechaHoraActual, estadoRechazada, empleado);
            return true;
        }
        documentacion.aceptar(fechaHoraActual, estadoAceptada, empleado);
        return false;
    }

    /** Flujo alternativo A5: Marca la documentación como Para redirigir. */
    public void marcarParaRedirigir(LocalDateTime fechaHoraActual, Estado estadoParaRedirigir, Empleado empleado) {
        if (documentacion != null) {
            documentacion.marcarParaRedirigir(fechaHoraActual, estadoParaRedirigir, empleado);
        }
    }

    @Override
    public String toString() {
        return "DetalleRemito{documentacion=" + documentacion + ", areaCMCDestino='" + areaCMCDestino + "'}";
    }
}
