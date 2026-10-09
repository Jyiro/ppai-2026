package com.ppai.boundary.cu28;

import com.ppai.domain.model.cu28.Documentacion;
import com.ppai.domain.model.cu28.OpcionRecepcion;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Objeto Presentador de Fila para Documentación en la Pantalla JavaFX.
 * Encapsula la entidad Documentacion de Dominio y agrega el estado de selección
 * reactivo para los flujos alternativos A3 (No recibida) y A4 (No corresponde).
 */
public class DocumentacionRow {

    private final Documentacion documentacion;
    private final BooleanProperty seleccionada;

    public DocumentacionRow(Documentacion documentacion) {
        this.documentacion = documentacion;
        // Por defecto marcada como conforme / recibida
        this.seleccionada = new SimpleBooleanProperty(true);
    }

    public Documentacion getDocumentacion() {
        return documentacion;
    }

    public Long getId() {
        return documentacion.getId();
    }

    public String getAsunto() {
        return documentacion.getAsunto();
    }

    public String getTipo() {
        return documentacion.mostrarTipoDocumentacion();
    }

    public String getEstadoActual() {
        return documentacion.getEstadoActual() != null
                ? documentacion.getEstadoActual().getNombreDisplay()
                : "En bolsín enviado";
    }

    public boolean isSeleccionada() {
        return seleccionada.get();
    }

    public void setSeleccionada(boolean valor) {
        this.seleccionada.set(valor);
    }

    public BooleanProperty seleccionadaProperty() {
        return seleccionada;
    }

    /**
     * Calcula dinámicamente el estado resultante según la opción de recepción elegida.
     */
    public String getEstadoResultante(OpcionRecepcion opcion) {
        if (opcion == null) return "-";
        if (opcion.esContenidoIgual()) {
            return "Recibida y aceptada";
        } else if (opcion.esFaltaDocumentacion()) {
            return isSeleccionada() ? "Recibida y aceptada" : "No recibida (A3)";
        } else if (opcion.esNoCorrespondeDestino()) {
            return isSeleccionada() ? "Recibida y aceptada" : "Recibida y rechazada (A4)";
        } else if (opcion.esRedirigirAOtraArea()) {
            return "Para redirigir (A5)";
        }
        return "Pendiente";
    }
}
