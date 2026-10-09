package com.ppai.domain.model.cu28;

/**
 * Entidad Sesion — CU 28: Registrar Recepción de Bolsín.
 * Representa la sesión activa del usuario que ejecuta el CU.
 */
public class Sesion {

    private Usuario usuario;

    public Sesion(Usuario usuario) {
        this.usuario = usuario;
    }

    /** DS-CU28 paso 5: Retorna el usuario autenticado en la sesión activa. */
    public Usuario getUsuario() {
        return usuario;
    }
}

