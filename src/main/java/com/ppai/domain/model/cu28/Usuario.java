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
 * Entidad Usuario — CU 28: Registrar Recepción de Bolsín.
 * Representa al usuario autenticado.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @Column(name = "username", nullable = false)
    private String username;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    protected Usuario() {
    }

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario that)) return false;
        return Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return "Usuario{username='" + username + "'}";
    }
}

