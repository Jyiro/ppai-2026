package com.ppai.domain.model.cu28;

import java.util.Objects;

/**
 * Entidad TipoDocumento — CU 28: Registrar Recepción de Bolsín.
 * Categoría o tipo de una Documentación (Dominio OO puro).
 */
public class TipoDocumento {

    private final String nombre;
    private final String descripcion;

    public TipoDocumento(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    /** DS-CU28 loop interno sec. 4: Retorna el nombre descriptivo del tipo. */
    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TipoDocumento that)) return false;
        return Objects.equals(nombre, that.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre);
    }

    @Override
    public String toString() {
        return "TipoDocumento{nombre='" + nombre + "'}";
    }
}
