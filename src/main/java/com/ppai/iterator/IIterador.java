package com.ppai.iterator;

/**
 * Interfaz IIterador — Patrón de Diseño Iterator (GoF).
 * Define las operaciones fundamentales para recorrer secuencialmente una colección
 * y consultar el elemento actual sin exponer la estructura interna del agregado.
 *
 * NOTA DE DISEÑO: No utiliza ni extiende java.util.Iterator.
 */
public interface IIterador {

    /**
     * Posiciona el iterador en el primer elemento de la colección.
     */
    void primero();

    /**
     * Avanza el iterador hacia la siguiente posición de la colección.
     */
    void siguiente();

    /**
     * Indica si se ha recorrido la totalidad de la colección.
     *
     * @return true si ya no quedan elementos por recorrer; false en caso contrario.
     */
    boolean haFinalizado();

    /**
     * Retorna el elemento situado en la posición actual del recorrido.
     *
     * @return El objeto actual, o null si ha finalizado o no cumple el criterio de filtrado.
     */
    Object elementoActual();
}
