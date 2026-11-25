package com.ppai.iterator;

import com.ppai.domain.model.cu28.DetalleRemito;

/**
 * Iterador concreto IteradorDetalleRemito — Patrón de Diseño Iterator (GoF).
 * Permite recorrer secuencialmente la colección de detalles de remito para
 * consultar el asunto y tipo de documentación.
 *
 * NOTA DE DISEÑO: No utiliza ni extiende java.util.Iterator.
 */
public class IteradorDetalleRemito implements IIterador {

    private final DetalleRemito[] lista;
    private int posicionActual;

    public IteradorDetalleRemito(Object[] elementos) {
        if (elementos == null) {
            this.lista = new DetalleRemito[0];
        } else if (elementos instanceof DetalleRemito[] detalles) {
            this.lista = detalles;
        } else {
            this.lista = new DetalleRemito[elementos.length];
            for (int i = 0; i < elementos.length; i++) {
                this.lista[i] = (DetalleRemito) elementos[i];
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
