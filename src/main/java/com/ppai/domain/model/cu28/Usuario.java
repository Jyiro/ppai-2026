package com.ppai.domain.model.cu28;

/**
 * Entidad Usuario — CU 28: Registrar Recepción de Bolsín.
 * Representa al usuario autenticado.
 */
public class Usuario {

    private String username;
    private Empleado empleado;

    public Usuario(String username, Empleado empleado) {
        this.username = username;
        this.empleado = empleado;
    }

    /** DS-CU28 paso 6: Delega en Empleado#getCM() (paso 7). */
    public ComisionMedica getCMEmpleado() {
        return empleado.getCM();
    }

    public String getUsername() {
        return username;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    @Override
    public String toString() {
        return "Usuario{username='" + username + "'}";
    }
}

