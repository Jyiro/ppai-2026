package com.ppai.domain.model.cu28;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Entidad TipoDocumento — CU 28: Registrar Recepción de Bolsín.
 * Categoría o tipo de una Documentación.
 */
@Entity
@Table(name = "tipo_documento")
public class TipoDocumento {

    @Id
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    protected TipoDocumento() {
    }

    public TipoDocumento(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    /** DS-CU28 loop interno sec. 4: Retorna el nombre descriptivo del tipo. */
    public String getNombre() {
        return nombre;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TipoDocumento that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TipoDocumento{id=" + id + ", nombre='" + nombre + "'}";
    }
}

