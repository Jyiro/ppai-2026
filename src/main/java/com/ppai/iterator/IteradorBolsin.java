package com.ppai.iterator;

import com.ppai.domain.model.cu28.Bolsin;

/**
 * Iterador concreto IteradorBolsin — Patrón de Diseño Iterator (GoF).
 * Permite recorrer una colección de bolsines aplicando en elementoActual() el filtro
 * de estado ENVIADO y Comisión Médica destino correspondiente al usuario logueado.
 *
 * NOTA DE DISEÑO: No utiliza ni extiende java.util.Iterator.
 */
public class IteradorBolsin implements IIterador {

    private final Bolsin[] lista;
    private int posicionActual;
    private final Object[] filtros;

    public IteradorBolsin(Object[] elementos, Object[] filtros) {
        if (elementos == null) {
            this.lista = new Bolsin[0];
        } else if (elementos instanceof Bolsin[] bolsines) {
            this.lista = bolsines.clone();
        } else {
            this.lista = new Bolsin[elementos.length];
            for (int i = 0; i < elementos.length; i++) {
                this.lista[i] = (Bolsin) elementos[i];
            }
        }
        this.filtros = filtros;
        this.posicionActual = 0;
    }

    /**
     * Comprueba si el bolsín en la posición actual cumple con las condiciones:
     * 1. Se encuentra en estado ENVIADO.
     * 2. Su Comisión Médica destino coincide exactamente con la CM del usuario (String) en filtros.
     *
     * @param filtro Criterios de filtrado (filtro[0] = cmUsuario: String).
     * @return true si cumple el filtro; false en caso contrario.
     */
    public boolean comprobarFiltro(Object[] filtro) {
        if (posicionActual < 0 || posicionActual >= lista.length) {
            return false;
        }
        Bolsin bolsin = lista[posicionActual];
        if (bolsin == null || !bolsin.sosEnviado()) {
            return false;
        }
        if (filtro != null && filtro.length > 0 && filtro[0] != null) {
            if (filtro[0] instanceof String cmUsuario) {
                return bolsin.esTuCMDestino(cmUsuario);
            }
            return false;
        }
        return true;
    }

    @Override
    public void primero() {
        this.posicionActual = 0;
    }

    @Override
    public void siguiente() {
        if (posicionActual < lista.length) {
            posicionActual++;
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
        if (comprobarFiltro(this.filtros)) {
            return lista[posicionActual];
        }
        return null;
    }
}
