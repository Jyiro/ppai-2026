package com.ppai;

import com.ppai.controller.cu28.GestorRecepcionBolsin;
import com.ppai.domain.model.cu28.*;
import com.ppai.repository.JpaUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de pruebas de persistencia (a actualizar con Data Mapper en Fase 2).
 */
public class PersistenciaCU28Test {

    private GestorRecepcionBolsin gestor;

    @BeforeEach
    void setUp() {
        ComisionMedica cmCentral = new ComisionMedica(1, "Comisión Médica Central (CMC) - CABA",
                "Av. Roque Sáenz Peña 1100", "despacho.cmc@srt.gob.ar", "011-4321-1000");
        Empleado empleado = new Empleado("Juan Pérez (Encargado Central)", cmCentral);
        Usuario usuario = new Usuario("jperez.cmc", "Operador Central", empleado);
        Sesion sesionCentral = new Sesion(usuario, LocalDateTime.now());

        gestor = new GestorRecepcionBolsin(sesionCentral);
        gestor.tomarOpcionRegistrarRecepcionBolsin();
    }

    private Bolsin consultarBolsinEnBD(int numeroPrecinto) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT b FROM Bolsin b WHERE b.numeroPrecinto = :precinto",
                            Bolsin.class)
                    .setParameter("precinto", numeroPrecinto)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    @Test
    @DisplayName("BD H2: Tablas maestras y 7 bolsines iniciales quedan persistidos al arrancar")
    void testTablasYDatosInicialesPersistidosEnH2() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            Long cantEstados = em.createQuery("SELECT COUNT(e) FROM Estado e", Long.class).getSingleResult();
            Long cantCMs = em.createQuery("SELECT COUNT(c) FROM ComisionMedica c", Long.class).getSingleResult();
            Long cantTipos = em.createQuery("SELECT COUNT(t) FROM TipoDocumento t", Long.class).getSingleResult();
            Long cantBolsines = em.createQuery("SELECT COUNT(b) FROM Bolsin b", Long.class).getSingleResult();
            Long cantRemitos = em.createQuery("SELECT COUNT(r) FROM Remito r", Long.class).getSingleResult();

            assertEquals(9L, cantEstados);
            assertEquals(7L, cantCMs);
            assertEquals(4L, cantTipos);
            assertEquals(7L, cantBolsines);
            assertEquals(7L, cantRemitos);
        } finally {
            em.close();
        }
    }

    @Test
    @DisplayName("BD H2: Flujo Básico (Opción 1) persiste historial de Bolsín, Remito y Documentación")
    void testPersistenciaFlujoBasicoOpcion1() {
        Bolsin prec7041 = gestor.getBolsinesEnviados().stream()
                .filter(b -> b.getNroPrecinto() == 7041)
                .findFirst().orElseThrow();
        gestor.tomarSeleccionBolsin(prec7041);

        OpcionRecepcion opc1 = gestor.buscarOpcionesRecepcion().stream()
                .filter(OpcionRecepcion::esContenidoIgual)
                .findFirst().orElseThrow();
        gestor.tomarSeleccionOpcion(opc1, Collections.emptyList());
        gestor.tomarConfirmacionSeleccion(true);

        Bolsin enBD = consultarBolsinEnBD(7041);
        assertEquals(2, enBD.getCambiosDeEstado().size());
        assertNotNull(enBD.getCambiosDeEstado().get(0).getFechaHoraFin());
        assertEquals(Estado.NOMBRE_ENVIADO, enBD.getCambiosDeEstado().get(0).getEstado().getNombre());

        CambioEstadoBolsin actualBD = enBD.getCambiosDeEstado().get(1);
        assertNull(actualBD.getFechaHoraFin());
        assertEquals(Estado.NOMBRE_RECIBIDO_CM_DESTINO, actualBD.getEstado().getNombre());
        assertNotNull(actualBD.getEmpleado());
        assertEquals("Juan Pérez (Encargado Central)", actualBD.getEmpleado().getNombre());

        Remito remitoBD = enBD.getRemitos().get(0);
        assertNotNull(remitoBD.getEstado());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, remitoBD.getEstado().getNombre());
        assertEquals(Estado.AMBITO_REMITO, remitoBD.getEstado().getAmbito());

        for (DetalleRemito det : remitoBD.getDetalles()) {
            Documentacion docBD = det.getDocumentacion();
            assertEquals(2, docBD.getCambiosDeEstado().size());
            assertNotNull(docBD.getCambiosDeEstado().get(0).getFechaHoraFin());
            assertNull(docBD.getCambiosDeEstado().get(1).getFechaHoraFin());
            assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, docBD.getCambiosDeEstado().get(1).getEstado().getNombre());
            assertNotNull(docBD.getCambiosDeEstado().get(1).getEmpleado());
        }
    }

    @Test
    @DisplayName("BD H2: Flujo Alternativo A3 persiste Remito parcial y documentación NO_RECIBIDA")
    void testPersistenciaFlujoAlternativoA3() {
        Bolsin prec8820 = gestor.getBolsinesEnviados().stream()
                .filter(b -> b.getNroPrecinto() == 8820)
                .findFirst().orElseThrow();
        gestor.tomarSeleccionBolsin(prec8820);

        OpcionRecepcion opc2 = gestor.buscarOpcionesRecepcion().stream()
                .filter(OpcionRecepcion::esFaltaDocumentacion)
                .findFirst().orElseThrow();
        Documentacion docFaltante = prec8820.getRemitos().get(0).getDetalles().get(0).getDocumentacion();

        gestor.tomarSeleccionOpcion(opc2, List.of(docFaltante));
        gestor.tomarConfirmacionSeleccion(true);

        Bolsin enBD = consultarBolsinEnBD(8820);
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL, enBD.getRemitos().get(0).getEstado().getNombre());

        List<DetalleRemito> detallesBD = enBD.getRemitos().get(0).getDetalles();
        assertEquals(Estado.NOMBRE_NO_RECIBIDA, detallesBD.get(0).getDocumentacion().getEstadoActual().getNombre());
        assertEquals(2, detallesBD.get(0).getDocumentacion().getCambiosDeEstado().size());
        for (int i = 1; i < detallesBD.size(); i++) {
            assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO,
                    detallesBD.get(i).getDocumentacion().getEstadoActual().getNombre());
        }
    }

    @Test
    @DisplayName("BD H2: Flujo Alternativo A4 persiste Remito parcial y documentación RECIBIDA_Y_RECHAZADA")
    void testPersistenciaFlujoAlternativoA4() {
        Bolsin prec8820 = gestor.getBolsinesEnviados().stream()
                .filter(b -> b.getNroPrecinto() == 8820)
                .findFirst().orElseThrow();
        gestor.tomarSeleccionBolsin(prec8820);

        OpcionRecepcion opc3 = gestor.buscarOpcionesRecepcion().stream()
                .filter(OpcionRecepcion::esNoCorrespondeDestino)
                .findFirst().orElseThrow();
        Documentacion docErroneo = prec8820.getRemitos().get(0).getDetalles().get(0).getDocumentacion();

        gestor.tomarSeleccionOpcion(opc3, List.of(docErroneo));
        gestor.tomarConfirmacionSeleccion(true);

        Bolsin enBD = consultarBolsinEnBD(8820);
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL, enBD.getRemitos().get(0).getEstado().getNombre());

        List<DetalleRemito> detallesBD = enBD.getRemitos().get(0).getDetalles();
        assertEquals(Estado.NOMBRE_RECIBIDA_Y_RECHAZADA, detallesBD.get(0).getDocumentacion().getEstadoActual().getNombre());
        for (int i = 1; i < detallesBD.size(); i++) {
            assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO,
                    detallesBD.get(i).getDocumentacion().getEstadoActual().getNombre());
        }
    }

    @Test
    @DisplayName("BD H2: Flujo Alternativo A5 (CMC) persiste documentación PARA_REDIRIGIR")
    void testPersistenciaFlujoAlternativoA5() {
        Bolsin prec9150 = gestor.getBolsinesEnviados().stream()
                .filter(b -> b.getNroPrecinto() == 9150)
                .findFirst().orElseThrow();
        gestor.tomarSeleccionBolsin(prec9150);

        OpcionRecepcion opc4 = gestor.buscarOpcionesRecepcion().stream()
                .filter(OpcionRecepcion::esRedirigirAOtraArea)
                .findFirst().orElseThrow();

        gestor.tomarSeleccionOpcion(opc4, Collections.emptyList());
        gestor.tomarConfirmacionSeleccion(true);

        Bolsin enBD = consultarBolsinEnBD(9150);
        assertEquals(Estado.NOMBRE_RECIBIDO_CM_DESTINO, enBD.getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, enBD.getRemitos().get(0).getEstado().getNombre());
        assertEquals(Estado.NOMBRE_PARA_REDIRIGIR,
                enBD.getRemitos().get(0).getDetalles().get(0).getDocumentacion().getEstadoActual().getNombre());
    }

    @Test
    @DisplayName("BD H2: Flujo Alternativo A6 (no confirmar) mantiene el bolsín ENVIADO sin cambios en BD")
    void testNoConfirmarA6NoModificaBaseDeDatos() {
        Bolsin prec7041 = gestor.getBolsinesEnviados().stream()
                .filter(b -> b.getNroPrecinto() == 7041)
                .findFirst().orElseThrow();
        gestor.tomarSeleccionBolsin(prec7041);

        OpcionRecepcion opc1 = gestor.buscarOpcionesRecepcion().stream()
                .filter(OpcionRecepcion::esContenidoIgual)
                .findFirst().orElseThrow();
        gestor.tomarSeleccionOpcion(opc1, Collections.emptyList());
        gestor.tomarConfirmacionSeleccion(false);

        Bolsin enBD = consultarBolsinEnBD(7041);
        assertEquals(1, enBD.getCambiosDeEstado().size());
        assertEquals(Estado.NOMBRE_ENVIADO, enBD.getEstadoActual().getNombre());
        assertNull(enBD.getRemitos().get(0).getEstado());
    }
}
