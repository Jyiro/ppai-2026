package com.ppai.domain.model.cu28;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Remito — CU 28: Registrar Recepción de Bolsín.
 * Contiene la lista de DetalleRemito con sus documentaciones.
 * (Punto de extensión: Patrón Iterator para recorrer detalles)
 */
public class Remito {

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

    /** DS-CU28 loop sec. 4: Retorna los detalles con sus documentaciones. */
    public List<DetalleRemito> buscarDocumentacion() {
        return detalles;
    }

    /** DS-CU28 loop sec. 8: Orquesta la aceptación en cada detalle de remito. */
    public void aceptar(Estado estadoRecibidoYAceptado, LocalDateTime fechaHoraActual) {
        this.estado = estadoRecibidoYAceptado;
        for (DetalleRemito detalle : detalles) {
            detalle.aceptar(estadoRecibidoYAceptado, fechaHoraActual);
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

