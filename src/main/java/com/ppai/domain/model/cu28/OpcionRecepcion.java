package com.ppai.domain.model.cu28;

/**
 * Entidad OpcionRecepcion — CU 28: Registrar Recepción de Bolsín.
 * Modela como objeto de primera clase cada una de las 4 opciones de recepción
 * establecidas en la Especificación de Requisitos de Software (ERS) y la regla de negocio.
 */
public class OpcionRecepcion {

    private final int numero;
    private final String titulo;
    private final String descripcion;
    private final String impactoRemito;
    private final String impactoDocumentacion;
    private final boolean requiereSeleccionDocs;
    private final boolean soloCMC;

    public OpcionRecepcion(int numero,
                           String titulo,
                           String descripcion,
                           String impactoRemito,
                           String impactoDocumentacion,
                           boolean requiereSeleccionDocs,
                           boolean soloCMC) {
        this.numero = numero;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.impactoRemito = impactoRemito;
        this.impactoDocumentacion = impactoDocumentacion;
        this.requiereSeleccionDocs = requiereSeleccionDocs;
        this.soloCMC = soloCMC;
    }

    public int getNumero() {
        return numero;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getImpactoRemito() {
        return impactoRemito;
    }

    public String getImpactoDocumentacion() {
        return impactoDocumentacion;
    }

    public boolean isRequiereSeleccionDocs() {
        return requiereSeleccionDocs;
    }

    public boolean isSoloCMC() {
        return soloCMC;
    }

    public boolean esContenidoIgual() {
        return numero == 1;
    }

    public boolean esFaltaDocumentacion() {
        return numero == 2;
    }

    public boolean esNoCorrespondeDestino() {
        return numero == 3;
    }

    public boolean esRedirigirAOtraArea() {
        return numero == 4;
    }

    @Override
    public String toString() {
        return "[" + numero + "] " + titulo;
    }
}
