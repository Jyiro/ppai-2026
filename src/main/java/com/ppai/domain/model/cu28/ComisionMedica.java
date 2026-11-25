package com.ppai.domain.model.cu28;

import java.util.Objects;

/**
 * Entidad ComisionMedica — CU 28: Registrar Recepción de Bolsín.
 * Representa una Comisión Médica (CM) del sistema (Dominio OO puro).
 */
public class ComisionMedica {

    private final int codigo;
    private final String nombre;
    private final String direccion;
    private final String email;
    private final String telefono;

    public ComisionMedica(int codigo, String nombre, String direccion, String email, String telefono) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.direccion = direccion;
        this.email = email;
        this.telefono = telefono;
    }

    /** DS-CU28 loop sec. 2: Verifica coincidencia exacta con la CM del usuario logueado. */
    public boolean esTuCMDestino(String cmUsuario) {
        if (cmUsuario == null || this.nombre == null) {
            return false;
        }
        return this.nombre.trim().equalsIgnoreCase(cmUsuario.trim());
    }

    /** Regla de negocio A5: Indica si corresponde a la Comisión Médica Central (CMC). */
    public boolean esCentral() {
        return this.codigo == 1
                || (this.nombre != null
                && (this.nombre.toUpperCase().contains("CENTRAL")
                || this.nombre.toUpperCase().contains("CMC")));
    }

    public int getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComisionMedica that)) return false;
        return codigo == that.codigo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    @Override
    public String toString() {
        return "ComisionMedica{codigo=" + codigo + ", nombre='" + nombre + "'}";
    }
}
