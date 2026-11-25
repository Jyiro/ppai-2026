package com.ppai.domain.model.cu28;

import com.ppai.iterator.IAgregado;
import com.ppai.iterator.IIterador;
import com.ppai.iterator.IteradorDetalleRemito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Entidad Remito — CU 28: Registrar Recepción de Bolsín.
 * Contiene la lista de DetalleRemito con sus documentaciones (Dominio OO puro).
 * Implementa IAgregado para delegar el recorrido a IteradorDetalleRemito.
 *
 * NOTA DE DISEÑO: No utiliza ni extiende java.util.Iterable.
 */
public class Remito implements IAgregado {

    private final int numero;
    private final LocalDateTime fecha;
    private final ComisionMedica cmOrigen;
    private final ComisionMedica cmDestino;
    private Estado estadoRemito;
    private final List<DetalleRemito> detalles;

    public Remito(int numero, LocalDateTime fecha, ComisionMedica cmOrigen, ComisionMedica cmDestino) {
        this.numero = numero;
        this.fecha = fecha;
        this.cmOrigen = cmOrigen;
        this.cmDestino = cmDestino;
        this.detalles = new ArrayList<>();
    }

    /** DS-CU28 loop sec. 4: Retorna el número de remito como entero. */
    public int getNumero() {
        return numero;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public ComisionMedica getCmOrigen() {
        return cmOrigen;
    }

    public ComisionMedica getCmDestino() {
        return cmDestino;
    }

    @Override
    public IIterador crearIterador(Object[] elementos) {
        Object[] fuente = (elementos != null) ? elementos : this.detalles.toArray(new DetalleRemito[0]);
        return new IteradorDetalleRemito(fuente);
    }

    /**
     * DS-CU28 loop sec. 4: Recorre los detalles con IteradorDetalleRemito y retorna
     * la matriz de [asunto, tipoDocumento] para cada documentación del remito.
     */
    public String[][] buscarDocumentacion() {
        DetalleRemito[] array = this.detalles.toArray(new DetalleRemito[0]);
        IIterador iterador = crearIterador(array);
        iterador.primero();
        List<String[]> filas = new ArrayList<>();
        while (!iterador.haFinalizado()) {
            DetalleRemito det = (DetalleRemito) iterador.elementoActual();
            if (det != null) {
                filas.add(det.obtenerAsuntoYTipoDoc());
            }
            iterador.siguiente();
        }
        return filas.toArray(new String[0][]);
    }

    /** DS-CU28 loop sec. 8 (Opción 1): Actualiza el remito y delega aceptación en cada detalle. */
    public void aceptar(LocalDateTime fechaHoraActual,
                        Estado estadoRemitoAceptado,
                        Estado estadoDocAceptada,
                        Empleado empleado) {
        this.estadoRemito = estadoRemitoAceptado;
        for (DetalleRemito detalle : detalles) {
            detalle.aceptar(fechaHoraActual, estadoDocAceptada, empleado);
        }
    }

    /**
     * Flujo alternativo A3: Marca las documentaciones faltantes como No recibida y el resto como Aceptada.
     * Solo este remito pasa a parcial si tuvo al menos una documentación faltante; sino queda aceptado.
     */
    public void registrarFaltaDocumentacion(LocalDateTime fechaHoraActual,
                                             Estado estadoRemitoAceptado,
                                             Estado estadoRemitoParcial,
                                             Estado estadoDocAceptada,
                                             Estado estadoDocNoRecibida,
                                             Set<Integer> numerosDocsNoConformes,
                                             Empleado empleado) {
        boolean huboFaltante = false;
        for (DetalleRemito detalle : detalles) {
            if (detalle.registrarFaltaDocumentacion(fechaHoraActual, estadoDocAceptada,
                    estadoDocNoRecibida, numerosDocsNoConformes, empleado)) {
                huboFaltante = true;
            }
        }
        this.estadoRemito = huboFaltante ? estadoRemitoParcial : estadoRemitoAceptado;
    }

    /**
     * Flujo alternativo A4: Marca las documentaciones erróneas como Recibida y rechazada y el resto como Aceptada.
     * Solo este remito pasa a parcial si tuvo al menos una documentación errónea; sino queda aceptado.
     */
    public void registrarDestinoErroneo(LocalDateTime fechaHoraActual,
                                        Estado estadoRemitoAceptado,
                                        Estado estadoRemitoParcial,
                                        Estado estadoDocAceptada,
                                        Estado estadoDocRechazada,
                                        Set<Integer> numerosDocsNoConformes,
                                        Empleado empleado) {
        boolean huboErronea = false;
        for (DetalleRemito detalle : detalles) {
            if (detalle.registrarDestinoErroneo(fechaHoraActual, estadoDocAceptada,
                    estadoDocRechazada, numerosDocsNoConformes, empleado)) {
                huboErronea = true;
            }
        }
        this.estadoRemito = huboErronea ? estadoRemitoParcial : estadoRemitoAceptado;
    }

    /** Flujo alternativo A5: Marca el remito como aceptado y todas sus documentaciones Para redirigir. */
    public void marcarParaRedirigir(LocalDateTime fechaHoraActual,
                                    Estado estadoRemitoAceptado,
                                    Estado estadoDocParaRedirigir,
                                    Empleado empleado) {
        this.estadoRemito = estadoRemitoAceptado;
        for (DetalleRemito detalle : detalles) {
            detalle.marcarParaRedirigir(fechaHoraActual, estadoDocParaRedirigir, empleado);
        }
    }

    public Estado getEstado() {
        return estadoRemito;
    }

    public void setEstado(Estado estado) {
        this.estadoRemito = estado;
    }

    public void agregarDetalle(DetalleRemito detalle) {
        if (detalle != null) {
            this.detalles.add(detalle);
        }
    }

    public List<DetalleRemito> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Remito that)) return false;
        return numero == that.numero;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    @Override
    public String toString() {
        return "Remito{numero=" + numero + ", cantidadDetalles=" + detalles.size() + "}";
    }
}
