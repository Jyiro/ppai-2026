package com.ppai.controller;

import com.ppai.domain.model.EjemploEntidad;
import com.ppai.repository.JpaUtil;
import jakarta.persistence.EntityManager;

import java.util.List;

/**
 * Controller / Gestor del Caso de Uso (PPAI / ECB Pattern).
 * Orquesta los mensajes del Diagrama de Secuencia y mantiene el estado temporal del CU.
 */
public class GestorEjemplo {

    private EjemploEntidad entidadSeleccionada;

    public GestorEjemplo() {
    }

    /**
     * Inicia el Caso de Uso (ej: opción seleccionada en Pantalla).
     */
    public void opcionRegistrarEjemplo() {
        System.out.println("[GESTOR] Caso de Uso Iniciado.");
    }

    /**
     * Registra una nueva entidad en la base de datos usando Hibernate/JPA.
     */
    public void crearNuevaEntidad(String nombre) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            EjemploEntidad nueva = new EjemploEntidad(nombre);
            nueva.ejecutarOperacion();
            em.persist(nueva);
            em.getTransaction().commit();
            
            this.entidadSeleccionada = nueva;
            System.out.println("[GESTOR] Entidad creada y persistida con ID: " + nueva.getId());
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    /**
     * Recupera y lista las entidades registradas.
     */
    public List<EjemploEntidad> buscarEntidades() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery("SELECT e FROM EjemploEntidad e", EjemploEntidad.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Ejecuta una transición de estado en el objeto de dominio.
     */
    public void finalizarEntidad() {
        if (entidadSeleccionada == null) {
            System.out.println("[GESTOR] Ninguna entidad seleccionada para finalizar.");
            return;
        }
        
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            EjemploEntidad dbEntidad = em.find(EjemploEntidad.class, entidadSeleccionada.getId());
            if (dbEntidad != null) {
                dbEntidad.finalizar();
                em.merge(dbEntidad);
            }
            em.getTransaction().commit();
            System.out.println("[GESTOR] Transición de estado completada.");
        } finally {
            em.close();
        }
    }
}
