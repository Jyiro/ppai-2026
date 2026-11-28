package com.ppai.boundary.cu28;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Modelo visual de fila para la tabla de Documentación en JavaFX.
 * Almacena exclusivamente primitivos / String (cero imports de dominio, cero lógica de negocio).
 */
public class DocumentacionRow {

    private final int numeroRemito;
    private final int numeroDoc;
    private final String tipo;
    private final String asunto;
    private final String estadoActual;
    private String estadoResultante;
    private String severidadResultante;
    private final BooleanProperty seleccionada;

    public DocumentacionRow(int numeroRemito,
                            int numeroDoc,
                            String tipo,
                            String asunto,
                            String estadoActual) {
        this.numeroRemito = numeroRemito;
        this.numeroDoc = numeroDoc;
        this.tipo = tipo;
        this.asunto = asunto;
        this.estadoActual = estadoActual;
        this.estadoResultante = "-";
        this.severidadResultante = "OK";
        this.seleccionada = new SimpleBooleanProperty(true);
    }

    public int getNumeroRemito() {
        return numeroRemito;
    }

    public int getNumero() {
        return numeroDoc;
    }

    public String getTipo() {
        return tipo;
    }

    public String getAsunto() {
        return asunto;
    }

    public String getEstadoActual() {
        return estadoActual;
    }

    public String getEstadoResultante() {
        return estadoResultante;
    }

    public String getSeveridadResultante() {
        return severidadResultante;
    }

    public void actualizarPrevisualizacion(String estadoResultante, String severidadResultante) {
        this.estadoResultante = (estadoResultante != null) ? estadoResultante : "-";
        this.severidadResultante = (severidadResultante != null) ? severidadResultante : "OK";
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
}
