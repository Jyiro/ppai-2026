package com.ppai.domain.model.cu28;

import java.util.Objects;

/**
 * Entidad Empleado — CU 28: Registrar Recepción de Bolsín.
 * Representa al empleado logueado en el sistema (Dominio OO puro).
 */
public class Empleado {

    private final String nombre;
    private final ComisionMedica comisionMedica;

    public Empleado(String nombre, ComisionMedica comisionMedica) {
        this.nombre = nombre;
        this.comisionMedica = comisionMedica;
    }

    /** DS-CU28 paso 7: Retorna la Comisión Médica a la que pertenece el empleado. */
    public ComisionMedica getCM() {
        return comisionMedica;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Empleado that)) return false;
        return Objects.equals(nombre, that.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre);
    }

    @Override
    public String toString() {
        return "Empleado{nombre='" + nombre + "'}";
    }
}
