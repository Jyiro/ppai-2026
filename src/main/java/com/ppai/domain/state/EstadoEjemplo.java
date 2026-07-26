package com.ppai.domain.state;

/**
 * Interfaz/Clase Abstracta base para el Patrón State (Máquina de Estados de Entidades).
 * Cada estado concreto define el comportamiento en su estado específico y gestiona las transiciones.
 */
public interface EstadoEjemplo {
    
    String getNombre();
    
    /**
     * Regla de negocio / Acción ejecutada según el estado actual.
     */
    void procesar(Object contexto);
    
    /**
     * Determina si el estado permite realizar una transición o cancelación.
     */
    boolean esFinalizado();
}
