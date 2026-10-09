package com.ppai.domain.model.cu28;

/**
 * Entidad TipoDocumento — CU 28: Registrar Recepción de Bolsín.
 * Categoría o tipo de una Documentación.
 */
public class TipoDocumento {

    private Long id;
    private String nombre;

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
    public String toString() {
        return "TipoDocumento{id=" + id + ", nombre='" + nombre + "'}";
    }
}

