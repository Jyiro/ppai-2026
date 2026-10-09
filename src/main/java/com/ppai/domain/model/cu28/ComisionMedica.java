package com.ppai.domain.model.cu28;

/**
 * Entidad ComisionMedica — CU 28: Registrar Recepción de Bolsín.
 * Representa una Comisión Médica (CM) del sistema.
 */
public class ComisionMedica {

    private Long id;
    private String nombre;

    public ComisionMedica(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    /** DS-CU28 loop sec. 2: Verifica si esta CM coincide con la consultante. */
    public boolean esTuCMDestino(ComisionMedica cmConsultante) {
        return this.id != null && this.id.equals(cmConsultante.getId());
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return "ComisionMedica{id=" + id + ", nombre='" + nombre + "'}";
    }
}

