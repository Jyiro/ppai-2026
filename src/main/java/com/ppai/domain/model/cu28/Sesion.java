package com.ppai.domain.model.cu28;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Entidad Sesion — CU 28: Registrar Recepción de Bolsín.
 * Representa la sesión activa del usuario que ejecuta el CU.
 */
@Entity
@Table(name = "sesion")
public class Sesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    protected Sesion() {
        this.fechaHoraInicio = LocalDateTime.now();
    }

    public Sesion(Usuario usuario) {
        this.usuario = usuario;
        this.fechaHoraInicio = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    /** DS-CU28 paso 5: Retorna el usuario autenticado en la sesión activa. */
    public Usuario getUsuario() {
        return usuario;
    }
}

