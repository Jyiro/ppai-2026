package com.ppai.iterator;

import com.ppai.domain.model.cu28.Remito;

/**
 * Iterador concreto IteradorRemito — Patrón de Diseño Iterator (GoF).
 * Permite recorrer secuencialmente la colección de remitos asociados a un bolsín.
 *
 * NOTA DE DISEÑO: No utiliza ni extiende java.util.Iterator.
 */
public class IteradorRemito implements IIterador {

    private final Remito[] lista;
    private int posicionActual;

    public IteradorRemito(Object[] elementos) {
        if (elementos == null) {
            this.lista = new Remito[0];
        } else if (elementos instanceof Remito[] remitos) {
            this.lista = remitos.clone();
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
        if (posicionActual < lista.length) {
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
}
