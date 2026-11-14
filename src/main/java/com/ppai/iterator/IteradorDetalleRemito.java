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

    private DetalleRemito[] lista;
    private int posicionActual;

    public IteradorDetalleRemito(DetalleRemito[] lista) {
        this.lista = (lista != null) ? lista : new DetalleRemito[0];
        this.posicionActual = 0;
    }

    public IteradorDetalleRemito(Object[] elementos) {
        if (elementos == null) {
            this.lista = new DetalleRemito[0];
        } else if (elementos instanceof DetalleRemito[]) {
            this.lista = (DetalleRemito[]) elementos;
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
    public DetalleRemito[] getLista() {
        return lista;
    }

    public int getPosicionActual() {
        return posicionActual;
    }
}
