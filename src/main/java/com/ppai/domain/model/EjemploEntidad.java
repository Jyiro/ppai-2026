package com.ppai.domain.model;

import com.ppai.domain.state.EstadoEjemplo;
import com.ppai.domain.state.EstadoInicial;
import com.ppai.domain.state.EstadoFinalizado;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entidad de Dominio con Mapeo JPA y Responsabilidad de Negocio OO.
 */
@Entity
@Table(name = "ejemplo_entidad")
public class EjemploEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Transient // El estado se maneja vía Patrón State en memoria o con Converter JPA
    private EstadoEjemplo estadoActual;

    public EjemploEntidad() {
        // Constructor requerido por JPA
        this.fechaCreacion = LocalDateTime.now();
        this.estadoActual = new EstadoInicial();
    }

    public EjemploEntidad(String nombre) {
        this();
        this.nombre = nombre;
    }

    // --- MÉTODOS DE NEGOCIO (OO Pura - Experto en Información) ---

    public void ejecutarOperacion() {
        if (estadoActual.esFinalizado()) {
            throw new IllegalStateException("No se puede ejecutar la operación en estado " + estadoActual.getNombre());
        }
        estadoActual.procesar(this);
    }

    public void finalizar() {
        this.estadoActual = new EstadoFinalizado();
        System.out.println("Entidad '" + nombre + "' ha cambiado al estado: " + estadoActual.getNombre());
    }

    // --- GETTERS & SETTERS ENCAPSULADOS ---

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public EstadoEjemplo getEstadoActual() {
        return estadoActual;
    }
}
