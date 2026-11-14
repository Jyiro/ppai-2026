package com.ppai.iterator;

import com.ppai.domain.model.cu28.Bolsin;
import com.ppai.domain.model.cu28.ComisionMedica;

/**
 * Iterador concreto IteradorBolsin — Patrón de Diseño Iterator (GoF).
 * Permite recorrer una colección de bolsines aplicando el filtro de estado ENVIADO
 * y Comisión Médica destino correspondiente al usuario logueado.
 *
 * NOTA DE DISEÑO: No utiliza ni extiende java.util.Iterator.
 */
public class IteradorBolsin implements IIterador {

    private Bolsin[] lista;
    private int posicionActual;
    private Object[] filtros;

    public IteradorBolsin(Bolsin[] lista, Object[] filtros) {
        this.lista = (lista != null) ? lista : new Bolsin[0];
        this.filtros = filtros;
        if (this.lista.length == 0) {
            this.posicionActual = 0;
        } else {
            this.posicionActual = -1;
        }
    }

    public IteradorBolsin(Object[] elementos, Object[] filtros) {
        if (elementos == null) {
            this.lista = new Bolsin[0];
        } else if (elementos instanceof Bolsin[]) {
            this.lista = (Bolsin[]) elementos;
        } else {
            this.lista = new Bolsin[elementos.length];
            for (int i = 0; i < elementos.length; i++) {
                this.lista[i] = (Bolsin) elementos[i];
            }
        }
        this.filtros = filtros;
        if (this.lista.length == 0) {
            this.posicionActual = 0;
        } else {
            this.posicionActual = -1;
        }
    }

    /**
     * Comprueba si el bolsín en la posición actual cumple con las condiciones:
     * 1. Se encuentra en estado ENVIADO.
     * 2. Su Comisión Médica destino coincide con la CM del usuario pasada en filtros.
     *
     * @param filtro Criterios de filtrado (por ejemplo, ComisionMedica o String con nombre de CM).
     * @return true si cumple el filtro; false en caso contrario.
     */
    public boolean comprobarFiltro(Object[] filtro) {
        if (posicionActual < 0 || posicionActual >= lista.length) {
            return false;
        }
        Bolsin bolsin = lista[posicionActual];
        if (bolsin == null) {
            return false;
        }

        // Estado debe ser ENVIADO
        if (!bolsin.sosEnviado()) {
            return false;
        }

        // Si hay criterios de CM destino en el filtro, comprobar coincidencia
        if (filtro != null && filtro.length > 0 && filtro[0] != null) {
            Object cmFiltro = filtro[0];
            if (cmFiltro instanceof ComisionMedica) {
                return bolsin.esTuCMDestino((ComisionMedica) cmFiltro);
            } else if (cmFiltro instanceof String) {
                return bolsin.esTuCMDestino((String) cmFiltro);
            }
        }

        return true;
    }

    public boolean comprobarFiltro() {
        return comprobarFiltro(this.filtros);
    }

    @Override
    public void primero() {
        this.posicionActual = 0;
        while (posicionActual < lista.length && !comprobarFiltro(this.filtros)) {
            posicionActual++;
        }
    }

    @Override
    public void siguiente() {
        if (posicionActual < lista.length) {
            posicionActual++;
            while (posicionActual < lista.length && !comprobarFiltro(this.filtros)) {
                posicionActual++;
            }
        }
    }

    @Override
    public boolean haFinalizado() {
        if (lista.length == 0) {
            return true;
        }
        return posicionActual >= lista.length;
    }

    @Override
    public Object elementoActual() {
        if (haFinalizado() || posicionActual < 0) {
            return null;
        }
        // Invoca comprobarFiltro conforme al diagrama de secuencia de diseño
        comprobarFiltro(this.filtros);
        return lista[posicionActual];
    }

    @Override
    public Object actual() {
        return elementoActual();
    }

    // Getters auxiliares para inspección y testing
    public Bolsin[] getLista() {
        return lista;
    }

    public int getPosicionActual() {
        return posicionActual;
    }

    public Object[] getFiltros() {
        return filtros;
    }
}
