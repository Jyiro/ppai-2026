package com.ppai.domain.model.cu28;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Entidad Empleado — CU 28: Registrar Recepción de Bolsín.
 * Representa al empleado logueado en el sistema.
 */
@Entity
@Table(name = "empleado")
public class Empleado {

    @Id
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "comision_medica_id", nullable = false)
    private ComisionMedica comisionMedica;

    protected Empleado() {
    }

    public Empleado(Long id, String nombre, ComisionMedica comisionMedica) {
        this.id = id;
        this.nombre = nombre;
        this.comisionMedica = comisionMedica;
    }

    /** DS-CU28 paso 7: Retorna la Comisión Médica a la que pertenece el empleado. */
    public ComisionMedica getCM() {
        return comisionMedica;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Empleado that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Empleado{id=" + id + ", nombre='" + nombre + "'}";
    }
}

