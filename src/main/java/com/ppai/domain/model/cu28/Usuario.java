package com.ppai.domain.model.cu28;

import java.util.Objects;

/**
 * Entidad Usuario — CU 28: Registrar Recepción de Bolsín.
 * Representa al usuario autenticado (Dominio OO puro).
 */
public class Usuario {

    private final String nombre;
    private final String descripcion;
    private final Empleado empleado;

    public Usuario(String nombre, String descripcion, Empleado empleado) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.empleado = empleado;
    }

    /** DS-CU28 paso 6: Delega en Empleado#getCM() (paso 7). */
    public ComisionMedica getCMEmpleado() {
        return empleado != null ? empleado.getCM() : null;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario that)) return false;
        return Objects.equals(nombre, that.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre);
    }

    @Override
    public String toString() {
        return "Usuario{nombre='" + nombre + "'}";
    }
}
