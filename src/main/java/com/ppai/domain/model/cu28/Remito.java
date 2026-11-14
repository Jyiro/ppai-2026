package com.ppai.domain.model.cu28;

import com.ppai.iterator.IAgregado;
import com.ppai.iterator.IIterador;
import com.ppai.iterator.IteradorDetalleRemito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Remito — CU 28: Registrar Recepción de Bolsín.
 * Contiene la lista de DetalleRemito con sus documentaciones.
 * Implementa IAgregado para delegar el recorrido a IteradorDetalleRemito.
 *
 * NOTA DE DISEÑO: No utiliza ni extiende java.util.Iterable.
 */
public class Remito implements IAgregado {

    private String numero;
    private List<DetalleRemito> detalles;
    private Estado estado;

    public Remito(String numero) {
        this.numero = numero;
        this.detalles = new ArrayList<>();
    }

    /** DS-CU28 loop sec. 4: Retorna el número de remito. */
    public String getNumero() {
        return numero;
    }

    @Override
    public IIterador crearIterador(Object[] elementos) {
        DetalleRemito[] array;
        if (elementos instanceof DetalleRemito[]) {
            array = (DetalleRemito[]) elementos;
        } else if (elementos != null) {
            array = new DetalleRemito[elementos.length];
            for (int i = 0; i < elementos.length; i++) {
                array[i] = (DetalleRemito) elementos[i];
            }
        } else {
            array = this.detalles.toArray(new DetalleRemito[0]);
        }
        return new IteradorDetalleRemito(array);
    }

    public IIterador crearIterador(DetalleRemito[] elementos) {
        return new IteradorDetalleRemito(elementos != null ? elementos : this.detalles.toArray(new DetalleRemito[0]));
    }

    /** DS-CU28 loop sec. 4: Retorna los detalles con sus documentaciones utilizando IteradorDetalleRemito. */
    public List<DetalleRemito> buscarDocumentacion() {
        DetalleRemito[] array = this.detalles.toArray(new DetalleRemito[0]);
        IIterador iterador = crearIterador(array);
        iterador.primero();
        List<DetalleRemito> resultado = new ArrayList<>();
        while (!iterador.haFinalizado()) {
            DetalleRemito det = (DetalleRemito) iterador.actual();
            if (det != null) {
                det.obtenerAsuntoYTipoDoc();
                resultado.add(det);
            }
            iterador.siguiente();
        }
        return resultado;
    }

    /** DS-CU28 loop sec. 8: Orquesta la aceptación en cada detalle de remito. */
    public void aceptar(Estado estadoRecibidoYAceptado, LocalDateTime fechaHoraActual) {
        aceptar(estadoRecibidoYAceptado, fechaHoraActual, null);
    }

    /** DS-CU28 loop sec. 8: Orquesta la aceptación en cada detalle de remito con empleado (orquestador único). */
    public void aceptar(Estado estadoRecibidoYAceptado, LocalDateTime fechaHoraActual, Empleado empleado) {
        this.estado = estadoRecibidoYAceptado;
        for (DetalleRemito detalle : detalles) {
            detalle.aceptar(estadoRecibidoYAceptado, fechaHoraActual, empleado);
        }
    }

    public void marcarRecibidoYAceptado(Estado estado) {
        this.estado = estado;
    }

    public void marcarRecibidoYAceptadoParcial(Estado estado) {
        this.estado = estado;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public void agregarDetalle(DetalleRemito detalle) {
        this.detalles.add(detalle);
    }

    public List<DetalleRemito> getDetalles() {
        return detalles;
    }

    @Override
    public String toString() {
        return "Remito{numero='" + numero + "', cantidadDetalles=" + detalles.size() + "}";
    }
}

