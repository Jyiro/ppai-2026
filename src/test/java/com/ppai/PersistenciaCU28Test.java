package com.ppai;

import com.ppai.controller.cu28.GestorRecepcionBolsin;
import com.ppai.domain.model.cu28.*;
import com.ppai.repository.ConexionBD;
import com.ppai.repository.cu28.IRepositorioCU28;
import com.ppai.repository.cu28.MapeadorPersistenciaCU28;
import com.ppai.repository.cu28.ReiniciarEscenarioBD;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gate de Verificación Íntegra de Fase 2:
 * Pruebas de persistencia real en disco (H2 file) con Data Mapper JDBC (sin JPA ni Long id).
 */
public class PersistenciaCU28Test {

    @TempDir
    Path tempDir;

    private IRepositorioCU28 repositorio;
    private GestorRecepcionBolsin gestor;

    @BeforeEach
    void setUp() {
        ConexionBD.configurarUrl("jdbc:h2:file:" + tempDir.resolve("ppaidb-persistencia").toAbsolutePath());
        ReiniciarEscenarioBD.reiniciar("cmc");
        repositorio = new MapeadorPersistenciaCU28();
        Sesion sesion = repositorio.obtenerSesionActiva();
        assertNotNull(sesion, "ReiniciarEscenarioBD debe dejar una sesión activa");
        gestor = new GestorRecepcionBolsin(sesion, repositorio);
        gestor.tomarOpcionRegistrarRecepcionBolsin();
    }

    @AfterAll
    static void restaurarConexion() {
        ConexionBD.restaurarUrlPorDefecto();
        ConexionBD.cerrar();
    }

    private Bolsin buscarBolsinPersistido(IRepositorioCU28 repo, int precinto) {
        return repo.obtenerTodosBolsines().stream()
                .filter(b -> b.getNroPrecinto() == precinto)
                .findFirst()
                .orElseThrow();
    }

    private long contar(String sql) throws SQLException {
        try (Connection conn = ConexionBD.abrirConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : -1L;
        }
    }

    @Test
    @DisplayName("Seeder ReiniciarEscenarioBD es idempotente en catálogos y carga 7 bolsines / 8 remitos (con multi-remito)")
    void testReiniciarEscenarioSinDuplicarCatalogos() throws SQLException {
        // Ejecutar una segunda vez para comprobar que no duplica estados/CMs/tipos
        ReiniciarEscenarioBD.reiniciar("cmc");

        assertEquals(9L, contar("SELECT COUNT(*) FROM estado"));
        assertEquals(7L, contar("SELECT COUNT(*) FROM comision_medica"));
        assertEquals(4L, contar("SELECT COUNT(*) FROM tipo_documento"));
        assertEquals(7L, contar("SELECT COUNT(*) FROM bolsin"));
        assertEquals(8L, contar("SELECT COUNT(*) FROM remito"));
        assertEquals(1L, contar("SELECT COUNT(*) FROM sesion WHERE fecha_hora_fin IS NULL"));

        MapeadorPersistenciaCU28 nuevoMapeador = new MapeadorPersistenciaCU28();
        Bolsin multi = buscarBolsinPersistido(nuevoMapeador, 8820);
        assertEquals(2, multi.getRemitos().size(), "El bolsín 8820 debe tener 2 remitos en BD");
        assertEquals(4, multi.obtenerInformacionRemito().length, "El bolsín 8820 debe tener 4 documentaciones");
    }

    @Test
    @DisplayName("Durabilidad real tras cierre y reapertura: Opción 1 y A3 multi-remito sobre disco")
    void testDurabilidadRealTrasCierreYReapertura() {
        // 1. Confirmar recepción Opción 1 en bolsín 7041
        Bolsin prec7041 = gestor.getBolsinesEnviados().stream()
                .filter(b -> b.getNroPrecinto() == 7041)
                .findFirst().orElseThrow();
        gestor.tomarSeleccionBolsin(prec7041);
        OpcionRecepcion opc1 = gestor.buscarOpcionesRecepcion().stream()
                .filter(OpcionRecepcion::esContenidoIgual)
                .findFirst().orElseThrow();
        gestor.tomarSeleccionOpcion(opc1, Collections.emptyList());
        gestor.tomarConfirmacionSeleccion(true);

        // 2. Confirmar recepción A3 sobre bolsín multi-remito 8820 (falta doc 201 del Remito 892; Remito 893 completo)
        Bolsin prec8820 = gestor.getBolsinesEnviados().stream()
                .filter(b -> b.getNroPrecinto() == 8820)
                .findFirst().orElseThrow();
        gestor.tomarSeleccionBolsin(prec8820);
        OpcionRecepcion opc2 = gestor.buscarOpcionesRecepcion().stream()
                .filter(OpcionRecepcion::esFaltaDocumentacion)
                .findFirst().orElseThrow();
        Documentacion doc201Faltante = prec8820.getRemitos().get(0).getDetalles().get(0).getDocumentacion();
        gestor.tomarSeleccionOpcion(opc2, List.of(doc201Faltante));
        gestor.tomarConfirmacionSeleccion(true);

        // 3. Simular cierre total de aplicación y reapertura contra el mismo archivo H2 en disco
        ConexionBD.cerrar();
        IRepositorioCU28 repoReabierto = new MapeadorPersistenciaCU28();

        Bolsin b7041BD = buscarBolsinPersistido(repoReabierto, 7041);
        assertEquals(2, b7041BD.getCambiosDeEstado().size());
        assertNotNull(b7041BD.getCambiosDeEstado().get(0).getFechaHoraFin());
        assertEquals(Estado.NOMBRE_ENVIADO, b7041BD.getCambiosDeEstado().get(0).getEstado().getNombre());
        assertNull(b7041BD.getCambiosDeEstado().get(1).getFechaHoraFin());
        assertEquals(Estado.NOMBRE_RECIBIDO_CM_DESTINO, b7041BD.getCambiosDeEstado().get(1).getEstado().getNombre());
        assertEquals("Juan Pérez (Encargado Central)", b7041BD.getCambiosDeEstado().get(1).getEmpleado().getNombre());

        Remito rem458BD = b7041BD.getRemitos().get(0);
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, rem458BD.getEstado().getNombre());
        for (DetalleRemito det : rem458BD.getDetalles()) {
            Documentacion docBD = det.getDocumentacion();
            assertEquals(2, docBD.getCambiosDeEstado().size());
            assertNotNull(docBD.getCambiosDeEstado().get(0).getFechaHoraFin());
            assertNull(docBD.getCambiosDeEstado().get(1).getFechaHoraFin());
            assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, docBD.getCambiosDeEstado().get(1).getEstado().getNombre());
            assertNotNull(docBD.getCambiosDeEstado().get(1).getEmpleado());
        }

        Bolsin b8820BD = buscarBolsinPersistido(repoReabierto, 8820);
        assertEquals(2, b8820BD.getRemitos().size());
        Remito rem892 = b8820BD.getRemitos().get(0);
        Remito rem893 = b8820BD.getRemitos().get(1);
        assertEquals(892, rem892.getNumero());
        assertEquals(893, rem893.getNumero());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL, rem892.getEstado().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, rem893.getEstado().getNombre());
        assertEquals(Estado.NOMBRE_NO_RECIBIDA, rem892.getDetalles().get(0).getDocumentacion().getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, rem892.getDetalles().get(1).getDocumentacion().getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, rem893.getDetalles().get(0).getDocumentacion().getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, rem893.getDetalles().get(1).getDocumentacion().getEstadoActual().getNombre());
    }

    @Test
    @DisplayName("No-destrucción: instanciar un nuevo Gestor y ejecutar finCU() no borra recepciones previas en disco")
    void testNoDestruccionAlIniciarNiEnFinCU() {
        Bolsin prec7041 = gestor.getBolsinesEnviados().stream()
                .filter(b -> b.getNroPrecinto() == 7041)
                .findFirst().orElseThrow();
        gestor.tomarSeleccionBolsin(prec7041);
        OpcionRecepcion opc1 = gestor.buscarOpcionesRecepcion().stream()
                .filter(OpcionRecepcion::esContenidoIgual)
                .findFirst().orElseThrow();
        gestor.tomarSeleccionOpcion(opc1, Collections.emptyList());
        gestor.tomarConfirmacionSeleccion(true); // Ejecuta persistirRecepcion() y finCU()

        // Simular nuevo arranque de la aplicación sobre la misma BD en disco
        IRepositorioCU28 repoNuevo = new MapeadorPersistenciaCU28();
        assertFalse(repoNuevo.estaVacia(), "La BD ya poblada no debe considerarse vacía");
        GestorRecepcionBolsin gestorNuevo = new GestorRecepcionBolsin(repoNuevo.obtenerSesionActiva(), repoNuevo);
        gestorNuevo.tomarOpcionRegistrarRecepcionBolsin();

        Bolsin enBD = buscarBolsinPersistido(repoNuevo, 7041);
        assertEquals(Estado.NOMBRE_RECIBIDO_CM_DESTINO, enBD.getEstadoActual().getNombre(),
                "Reiniciar el Gestor jamás debe pisar recepciones confirmadas en disco");
        assertTrue(gestorNuevo.getBolsinesEnviados().stream().noneMatch(b -> b.getNroPrecinto() == 7041),
                "El bolsín 7041 ya recibido no debe volver a aparecer como ENVIADO");
    }

    @Test
    @DisplayName("BD H2: Flujo Alternativo A4 persiste remito parcial/completo y doc RECIBIDA_Y_RECHAZADA")
    void testPersistenciaFlujoAlternativoA4() {
        Bolsin prec8820 = gestor.getBolsinesEnviados().stream()
                .filter(b -> b.getNroPrecinto() == 8820)
                .findFirst().orElseThrow();
        gestor.tomarSeleccionBolsin(prec8820);

        OpcionRecepcion opc3 = gestor.buscarOpcionesRecepcion().stream()
                .filter(OpcionRecepcion::esNoCorrespondeDestino)
                .findFirst().orElseThrow();
        Documentacion doc203Erroneo = prec8820.getRemitos().get(1).getDetalles().get(0).getDocumentacion();

        gestor.tomarSeleccionOpcion(opc3, List.of(doc203Erroneo));
        gestor.tomarConfirmacionSeleccion(true);

        Bolsin enBD = buscarBolsinPersistido(new MapeadorPersistenciaCU28(), 8820);
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, enBD.getRemitos().get(0).getEstado().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL, enBD.getRemitos().get(1).getEstado().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDA_Y_RECHAZADA,
                enBD.getRemitos().get(1).getDetalles().get(0).getDocumentacion().getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO,
                enBD.getRemitos().get(1).getDetalles().get(1).getDocumentacion().getEstadoActual().getNombre());
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

        Bolsin enBD = buscarBolsinPersistido(new MapeadorPersistenciaCU28(), 9150);
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

        Bolsin enBD = buscarBolsinPersistido(new MapeadorPersistenciaCU28(), 7041);
        assertEquals(1, enBD.getCambiosDeEstado().size());
        assertEquals(Estado.NOMBRE_ENVIADO, enBD.getEstadoActual().getNombre());
        assertNull(enBD.getRemitos().get(0).getEstado());
    }
}
