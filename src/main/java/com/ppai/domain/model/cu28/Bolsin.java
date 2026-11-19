package com.ppai.domain.model.cu28;

import com.ppai.iterator.IAgregado;
import com.ppai.iterator.IIterador;
import com.ppai.iterator.IteradorRemito;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entidad Bolsin — CU 28: Registrar Recepción de Bolsín.
 * Entidad central del caso de uso.
 * Implementa IAgregado para delegar el recorrido de remitos a IteradorRemito.
 *
 * NOTA DE DISEÑO: No utiliza ni extiende java.util.Iterable.
 */
@Entity
@Table(name = "bolsin")
public class Bolsin implements IAgregado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_precinto", nullable = false, unique = true)
    private String numeroPrecinto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cm_origen_id", nullable = false)
    private ComisionMedica cmOrigen;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cm_destino_id", nullable = false)
    private ComisionMedica cmDestino;

    @Transient
    private Remito remito;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "bolsin_id")
    @OrderColumn(name = "orden")
    private List<Remito> remitos = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "bolsin_id")
    @OrderColumn(name = "orden")
    private List<CambioEstadoBolsin> cambiosDeEstado = new ArrayList<>();

    protected Bolsin() {
    }

    public Bolsin(String numeroPrecinto, ComisionMedica cmOrigen, ComisionMedica cmDestino,
                  Remito remito, Estado estadoInicial, LocalDateTime fechaHoraInicio) {
        this.numeroPrecinto = numeroPrecinto;
        this.cmOrigen = cmOrigen;
        this.cmDestino = cmDestino;
        this.remito = remito;
        this.remitos = new ArrayList<>();
        if (remito != null) {
            this.remitos.add(remito);
        }
        this.cambiosDeEstado = new ArrayList<>();
        this.cambiosDeEstado.add(new CambioEstadoBolsin(estadoInicial, fechaHoraInicio));
    }

    public Long getId() {
        return id;
    }

    /** DS-CU28 loop sec. 2: Verifica que el estado actual sea ENVIADO. */
    public boolean sosEnviado() {
        CambioEstadoBolsin actual = obtenerCambioActual();
        return actual != null && actual.sosEnviado();
    }

    /** DS-CU28 loop sec. 2: Verifica que la CM destino coincida y el estado actual sea ENVIADO. */
    public boolean sosEnviado(ComisionMedica cmEmpleado) {
        if (!esTuCMDestino(cmEmpleado)) {
            return false;
        }
        return sosEnviado();
    }

    /** DS-CU28 loop sec. 2: Verifica que la CM destino coincida por nombre y el estado sea ENVIADO. */
    public boolean sosEnviado(String cmUsuario) {
        if (!esTuCMDestino(cmUsuario)) {
            return false;
        }
        return sosEnviado();
    }

    /** DS-CU28 loop sec. 2: Verifica si la CM destino coincide con la pasada por parámetro. */
    public boolean esTuCMDestino(ComisionMedica cmEmpleado) {
        return cmDestino != null && cmDestino.esTuCMDestino(cmEmpleado);
    }

    /** DS-CU28 loop sec. 2: Verifica si la CM destino coincide con el nombre de CM del usuario. */
    public boolean esTuCMDestino(String cmUsuario) {
        return cmDestino != null && cmDestino.esTuCMDestino(cmUsuario);
    }

    /** DS-CU28 loop sec. 3: Retorna el número de precinto como String. */
    public String getNumeroPrecinto() {
        return numeroPrecinto;
    }

    /**
     * DS-CU28 loop sec. 2: Retorna el número de precinto como entero según especificación DSI.
     * Extrae los dígitos numéricos de la cadena de precinto (e.g. "PREC-7041" -> 7041).
     */
    public int getNroPrecinto() {
        if (numeroPrecinto == null) {
            return 0;
        }
        String digits = numeroPrecinto.replaceAll("\\D+", "");
        if (!digits.isEmpty()) {
            try {
                return Integer.parseInt(digits);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    /** DS-CU28 loop sec. 3: Retorna la CM de origen. */
    public ComisionMedica getCMOrigen() {
        return cmOrigen;
    }

    /**
     * DS-CU28 loop sec. 2: Retorna el nombre de la CM de origen (método nuevo de diseño).
     * Evita el acoplamiento directo entre el Gestor y ComisionMedica.
     */
    public String getNombreCMOrigen() {
        return (cmOrigen != null) ? cmOrigen.getNombre() : "";
    }

    @Override
    public IIterador crearIterador(Object[] elementos) {
        Remito[] array;
        if (elementos instanceof Remito[]) {
            array = (Remito[]) elementos;
        } else if (elementos != null) {
            array = new Remito[elementos.length];
            for (int i = 0; i < elementos.length; i++) {
                array[i] = (Remito) elementos[i];
            }
        } else {
            array = this.remitos.toArray(new Remito[0]);
        }
        return new IteradorRemito(array);
    }

    public IIterador crearIterador(Remito[] elementos) {
        return new IteradorRemito(elementos != null ? elementos : this.remitos.toArray(new Remito[0]));
    }

    /**
     * DS-CU28 paso 14: Retorna el remito asociado recorriendo la colección con IteradorRemito.
     */
    public Remito obtenerInformacionRemito() {
        Remito[] array = this.remitos.toArray(new Remito[0]);
        IIterador iterador = crearIterador(array);
        iterador.primero();
        Remito primerRemito = null;
        while (!iterador.haFinalizado()) {
            Remito r = (Remito) iterador.actual();
            if (primerRemito == null) {
                primerRemito = r;
            }
            if (r != null) {
                r.getNumero();
                r.buscarDocumentacion();
            }
            iterador.siguiente();
        }
        return primerRemito != null ? primerRemito : this.remito;
    }

    /** DS-CU28 pasos 26-30 y loop sec. 8: Cierra estado actual, crea nuevo estado y orquesta aceptación de remito. */
    public void marcarRecibido(Estado estadoRecibidoCMDestino,
                               Estado estadoDocRecibidaAceptada,
                               LocalDateTime fechaHoraActual) {
        marcarRecibido(estadoRecibidoCMDestino, estadoDocRecibidaAceptada, fechaHoraActual, null);
    }

    public void marcarRecibido(Estado estadoRecibidoCMDestino,
                               Estado estadoDocRecibidaAceptada,
                               LocalDateTime fechaHoraActual,
                               Empleado empleado) {
        CambioEstadoBolsin actual = obtenerCambioActual();
        if (actual != null) {
            actual.setFechaHoraFin(fechaHoraActual);
        }

        crearNuevoCambioDeEstado(estadoRecibidoCMDestino, fechaHoraActual, empleado);

        if (estadoDocRecibidaAceptada != null) {
            actualizarRemitoYDocumentacion(estadoDocRecibidaAceptada, fechaHoraActual, empleado);
        }
    }

    /** Marca el bolsín como recibido registrando fecha y empleado actuante. */
    public void marcarRecibido(Estado estadoRecibidoCMDestino,
                               LocalDateTime fechaHoraActual,
                               Empleado empleado) {
        CambioEstadoBolsin actual = obtenerCambioActual();
        if (actual != null) {
            actual.setFechaHoraFin(fechaHoraActual);
        }
        crearNuevoCambioDeEstado(estadoRecibidoCMDestino, fechaHoraActual, empleado);
    }

    /** DS-CU28 sec. 8: Actualiza todos los remitos asociados y sus documentaciones (orquestador). */
    public void actualizarRemitoYDocumentacion(Estado estadoRecibidaYAceptada,
                                              LocalDateTime fechaHoraActual,
                                              Empleado empleado) {
        for (Remito r : remitos) {
            r.aceptar(estadoRecibidaYAceptada, fechaHoraActual, empleado);
        }
    }

    /** DS-CU28 pasos 29-30: Crea y agrega el nuevo CambioEstadoBolsin. */
    public void crearNuevoCambioDeEstado(Estado estado, LocalDateTime fechaHoraInicio) {
        crearNuevoCambioDeEstado(estado, fechaHoraInicio, null);
    }

    public void crearNuevoCambioDeEstado(Estado estado, LocalDateTime fechaHoraInicio, Empleado empleado) {
        CambioEstadoBolsin nuevo = new CambioEstadoBolsin(estado, fechaHoraInicio, empleado);
        this.cambiosDeEstado.add(nuevo);
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
        return LocalDateTime.now();
    }

    public void agregarRemito(Remito r) {
        if (r != null) {
            this.remitos.add(r);
            if (this.remito == null) {
                this.remito = r;
            }
        }
    }

    public List<Remito> getRemitos() {
        return remitos;
    }

    public Remito getRemito() {
        return (!remitos.isEmpty()) ? remitos.get(0) : this.remito;
    }

    public ComisionMedica getCmDestino() {
        return cmDestino;
    }

    public List<CambioEstadoBolsin> getCambiosDeEstado() {
        return cambiosDeEstado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bolsin that)) return false;
        return Objects.equals(numeroPrecinto, that.numeroPrecinto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroPrecinto);
    }

    @Override
    public String toString() {
        return "Bolsin{numeroPrecinto='" + numeroPrecinto
                + "', cmOrigen=" + cmOrigen + ", cmDestino=" + cmDestino + "}";
    }
}

