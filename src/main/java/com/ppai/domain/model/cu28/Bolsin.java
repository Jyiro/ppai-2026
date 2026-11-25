package com.ppai.domain.model.cu28;

import com.ppai.iterator.IAgregado;
import com.ppai.iterator.IIterador;
import com.ppai.iterator.IteradorRemito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Entidad Bolsin — CU 28: Registrar Recepción de Bolsín (Dominio OO puro).
 * Implementa IAgregado para delegar el recorrido de sus remitos a IteradorRemito.
 *
 * NOTA DE DISEÑO: No utiliza ni extiende java.util.Iterable.
 */
public class Bolsin implements IAgregado {

    private final int numeroBolsin;
    private final int numeroPrecinto;
    private final LocalDateTime fecha;
    private final double peso;
    private final ComisionMedica cmOrigen;
    private final ComisionMedica cmDestino;
    private final List<Remito> remitos;
    private final List<CambioEstadoBolsin> cambiosDeEstado;

    public Bolsin(int numeroBolsin,
                  int numeroPrecinto,
                  LocalDateTime fecha,
                  double peso,
                  ComisionMedica cmOrigen,
                  ComisionMedica cmDestino,
                  List<Remito> remitos,
                  Estado estadoInicial,
                  LocalDateTime fechaHoraInicio,
                  Empleado empleadoInicial) {
        this.numeroBolsin = numeroBolsin;
        this.numeroPrecinto = numeroPrecinto;
        this.fecha = fecha;
        this.peso = peso;
        this.cmOrigen = cmOrigen;
        this.cmDestino = cmDestino;
        this.remitos = (remitos != null) ? new ArrayList<>(remitos) : new ArrayList<>();
        this.cambiosDeEstado = new ArrayList<>();
        if (estadoInicial != null) {
            this.cambiosDeEstado.add(new CambioEstadoBolsin(fechaHoraInicio, estadoInicial, empleadoInicial));
        }
    }

    /** DS-CU28 loop sec. 2: Verifica que el estado actual sea ENVIADO. */
    public boolean sosEnviado() {
        CambioEstadoBolsin actual = obtenerCambioActual();
        return actual != null && actual.sosEnviado();
    }

    /** DS-CU28 loop sec. 2: Verifica coincidencia exacta entre cmDestino y cmUsuario. */
    public boolean esTuCMDestino(String cmUsuario) {
        return cmDestino != null && cmDestino.esTuCMDestino(cmUsuario);
    }

    /** DS-CU28 loop sec. 2: Retorna el nombre de la CM de origen. */
    public String getNombreCMOrigen() {
        return (cmOrigen != null) ? cmOrigen.getNombre() : "";
    }

    /** DS-CU28 loop sec. 2: Retorna el número de precinto como entero. */
    public int getNroPrecinto() {
        return numeroPrecinto;
    }

    public int getNumeroBolsin() {
        return numeroBolsin;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public double getPeso() {
        return peso;
    }

    public ComisionMedica getCMOrigen() {
        return cmOrigen;
    }

    public ComisionMedica getCmDestino() {
        return cmDestino;
    }

    @Override
    public IIterador crearIterador(Object[] elementos) {
        Object[] fuente = (elementos != null) ? elementos : this.remitos.toArray(new Remito[0]);
        return new IteradorRemito(fuente);
    }

    /**
     * DS-CU28 paso 14 / loop sec. 4: Recorre todos los remitos con IteradorRemito
     * y retorna matriz [numeroRemito, asunto, tipoDocumentacion] con la totalidad
     * de remitos y sus documentaciones.
     */
    public String[][] obtenerInformacionRemito() {
        Remito[] array = this.remitos.toArray(new Remito[0]);
        IIterador iterador = crearIterador(array);
        iterador.primero();
        List<String[]> filas = new ArrayList<>();
        while (!iterador.haFinalizado()) {
            Remito r = (Remito) iterador.elementoActual();
            if (r != null) {
                String nroRemito = String.valueOf(r.getNumero());
                String[][] docs = r.buscarDocumentacion();
                for (String[] doc : docs) {
                    String asunto = (doc != null && doc.length > 0) ? doc[0] : "";
                    String tipoDoc = (doc != null && doc.length > 1) ? doc[1] : "";
                    filas.add(new String[]{ nroRemito, asunto, tipoDoc });
                }
            }
            iterador.siguiente();
        }
        return filas.toArray(new String[0][]);
    }

    /** DS-CU28 pasos 25-30: Cierra el estado vigente del bolsín y crea el nuevo cambio de estado. */
    public void marcarRecibido(LocalDateTime fechaHoraActual,
                               Estado estadoRecibidoCMDestino,
                               Empleado empleado) {
        CambioEstadoBolsin actual = obtenerCambioActual();
        if (actual != null) {
            actual.setFechaHoraFin(fechaHoraActual);
        }
        crearNuevoCambioDeEstado(fechaHoraActual, estadoRecibidoCMDestino, empleado);
    }

    /** DS-CU28 pasos 29-30: Crea y agrega el nuevo CambioEstadoBolsin. */
    public void crearNuevoCambioDeEstado(LocalDateTime fechaHoraInicio, Estado estado, Empleado empleado) {
        CambioEstadoBolsin nuevo = new CambioEstadoBolsin(fechaHoraInicio, estado, empleado);
        this.cambiosDeEstado.add(nuevo);
    }

    /** DS-CU28 sec. 8 (Opción 1): Actualiza todos los remitos asociados y sus documentaciones. */
    public void actualizarRemitoYDocumentacion(LocalDateTime fechaHoraActual,
                                               Estado estadoRemitoAceptado,
                                               Estado estadoDocAceptada,
                                               Empleado empleado) {
        for (Remito r : remitos) {
            r.aceptar(fechaHoraActual, estadoRemitoAceptado, estadoDocAceptada, empleado);
        }
    }

    /** Flujo alternativo A3 multi-remito: delega en todos los remitos la marcación de faltantes. */
    public void registrarFaltaDocumentacion(LocalDateTime fechaHoraActual,
                                             Estado estadoRemitoAceptado,
                                             Estado estadoRemitoParcial,
                                             Estado estadoDocAceptada,
                                             Estado estadoDocNoRecibida,
                                             Set<Integer> numerosDocsNoConformes,
                                             Empleado empleado) {
        for (Remito r : remitos) {
            r.registrarFaltaDocumentacion(fechaHoraActual, estadoRemitoAceptado, estadoRemitoParcial,
                    estadoDocAceptada, estadoDocNoRecibida, numerosDocsNoConformes, empleado);
        }
    }

    /** Flujo alternativo A4 multi-remito: delega en todos los remitos la marcación de destino erróneo. */
    public void registrarDestinoErroneo(LocalDateTime fechaHoraActual,
                                        Estado estadoRemitoAceptado,
                                        Estado estadoRemitoParcial,
                                        Estado estadoDocAceptada,
                                        Estado estadoDocRechazada,
                                        Set<Integer> numerosDocsNoConformes,
                                        Empleado empleado) {
        for (Remito r : remitos) {
            r.registrarDestinoErroneo(fechaHoraActual, estadoRemitoAceptado, estadoRemitoParcial,
                    estadoDocAceptada, estadoDocRechazada, numerosDocsNoConformes, empleado);
        }
    }

    /** Flujo alternativo A5 multi-remito: delega en todos los remitos la redirección interna (solo CMC). */
    public void marcarParaRedirigir(LocalDateTime fechaHoraActual,
                                    Estado estadoRemitoAceptado,
                                    Estado estadoDocParaRedirigir,
                                    Empleado empleado) {
        for (Remito r : remitos) {
            r.marcarParaRedirigir(fechaHoraActual, estadoRemitoAceptado, estadoDocParaRedirigir, empleado);
        }
    }

    public CambioEstadoBolsin obtenerCambioActual() {
        for (CambioEstadoBolsin cambio : cambiosDeEstado) {
            if (cambio.sosActual()) {
                return cambio;
            }
        }
        return null;
    }

    public Estado getEstadoActual() {
        CambioEstadoBolsin actual = obtenerCambioActual();
        return actual != null ? actual.getEstado() : null;
    }

    public LocalDateTime getFechaEnvio() {
        if (!cambiosDeEstado.isEmpty()) {
            return cambiosDeEstado.get(0).getFechaHoraInicio();
        }
        return this.fecha;
    }

    public void agregarRemito(Remito r) {
        if (r != null) {
            this.remitos.add(r);
        }
    }

    public List<Remito> getRemitos() {
        return Collections.unmodifiableList(remitos);
    }

    public List<CambioEstadoBolsin> getCambiosDeEstado() {
        return Collections.unmodifiableList(cambiosDeEstado);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bolsin that)) return false;
        return numeroBolsin == that.numeroBolsin;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroBolsin);
    }

    @Override
    public String toString() {
        return "Bolsin{numeroBolsin=" + numeroBolsin + ", numeroPrecinto=" + numeroPrecinto
                + ", cmOrigen=" + cmOrigen + ", cmDestino=" + cmDestino + "}";
    }
}
