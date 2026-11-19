package com.ppai.domain.model.cu28;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Entidad ComisionMedica — CU 28: Registrar Recepción de Bolsín.
 * Representa una Comisión Médica (CM) del sistema.
 */
@Entity
@Table(name = "comision_medica")
public class ComisionMedica {

    @Id
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    protected ComisionMedica() {
    }

    public ComisionMedica(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    /** DS-CU28 loop sec. 2: Verifica si esta CM coincide con la consultante. */
    public boolean esTuCMDestino(ComisionMedica cmConsultante) {
        return cmConsultante != null && this.id != null && this.id.equals(cmConsultante.getId());
    }

    /** DS-CU28 loop sec. 2: Verifica si esta CM coincide con la consultante por nombre o identificador. */
    public boolean esTuCMDestino(String cmUsuario) {
        if (cmUsuario == null) {
            return false;
        }
        if (this.nombre != null && (this.nombre.equalsIgnoreCase(cmUsuario)
                || this.nombre.toLowerCase().contains(cmUsuario.toLowerCase())
                || cmUsuario.toLowerCase().contains(this.nombre.toLowerCase()))) {
            return true;
        }
        if (this.id != null && this.id.toString().equals(cmUsuario)) {
            return true;
        }
        return false;
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
        if (!(o instanceof ComisionMedica that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ComisionMedica{id=" + id + ", nombre='" + nombre + "'}";
    }
}

