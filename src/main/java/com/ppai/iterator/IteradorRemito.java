package com.ppai.iterator;

import com.ppai.domain.model.cu28.Remito;

/**
 * Iterador concreto IteradorRemito — Patrón de Diseño Iterator (GoF).
 * Permite recorrer secuencialmente la colección de remitos asociados a un bolsín.
 *
 * NOTA DE DISEÑO: No utiliza ni extiende java.util.Iterator.
 */
public class IteradorRemito implements IIterador {

    private Remito[] lista;
    private int posicionActual;

    public IteradorRemito(Remito[] lista) {
        this.lista = (lista != null) ? lista : new Remito[0];
        this.posicionActual = 0;
    }

    public IteradorRemito(Object[] elementos) {
        if (elementos == null) {
            this.lista = new Remito[0];
        } else if (elementos instanceof Remito[]) {
            this.lista = (Remito[]) elementos;
        } else {
            this.lista = new Remito[elementos.length];
            for (int i = 0; i < elementos.length; i++) {
                this.lista[i] = (Remito) elementos[i];
            }
        }
        this.posicionActual = 0;
    }

    @Override
    public void primero() {
        this.posicionActual = 0;
    }

    @Override
    public void siguiente() {
        if (!haFinalizado()) {
            this.posicionActual++;
        }
    }

    @Override
    public boolean haFinalizado() {
        return posicionActual >= lista.length;
    }

    @Override
    public Object elementoActual() {
        if (haFinalizado() || posicionActual < 0) {
            return null;
        }
        return lista[posicionActual];
    }

    @Override
    public Object actual() {
        return elementoActual();
    }

    // Getters auxiliares para inspección y testing
    public Remito[] getLista() {
        return lista;
    }

    public int getPosicionActual() {
        return posicionActual;
    }
}
