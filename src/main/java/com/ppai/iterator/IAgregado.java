package com.ppai.iterator;

/**
 * Interfaz IAgregado — Patrón de Diseño Iterator (GoF).
 * Define la operación de fabricación pura para instanciar el iterador concreto.
 * Implementada por los agregados del CU 28 (GestorRecepcionBolsin, Bolsin, Remito).
 * 
 * NOTA DE DISEÑO: No utiliza ni extiende java.util.Iterable.
 */
public interface IAgregado {

    /**
     * Crea un iterador concreto para recorrer la colección de elementos.
     *
     * @param elementos Colección de elementos a recorrer en forma de arreglo.
     * @return Instancia que implementa la interfaz propia IIterador.
     */
    IIterador crearIterador(Object[] elementos);
}
