package com.ppai.domain.model.cu28;

/**
 * Entidad Empleado — CU 28: Registrar Recepción de Bolsín.
 * Representa al empleado logueado en el sistema.
 */
public class Empleado {

    private Long id;
    private String nombre;
    private ComisionMedica comisionMedica;

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
    public String toString() {
        return "Empleado{id=" + id + ", nombre='" + nombre + "'}";
    }
}

