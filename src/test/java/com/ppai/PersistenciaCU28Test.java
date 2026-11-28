package com.ppai;

import com.ppai.controller.cu28.GestorRecepcionBolsin;
import com.ppai.domain.model.cu28.Bolsin;
import com.ppai.domain.model.cu28.DetalleRemito;
import com.ppai.domain.model.cu28.Documentacion;
import com.ppai.domain.model.cu28.Estado;
import com.ppai.domain.model.cu28.Remito;
import com.ppai.domain.model.cu28.Sesion;
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
        gestor.tomarSeleccionBolsin(7041);
        gestor.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_CONTENIDO_IGUAL, new int[0]);
        gestor.tomarConfirmacionSeleccion(true);

        // Confirmar A3 sobre bolsín multi-remito 8820 (falta doc 201 del Remito 892; Remito 893 completo)
        gestor.tomarSeleccionBolsin(8820);
        gestor.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_FALTA_DOCUMENTACION, new int[]{ 201 });
        gestor.tomarConfirmacionSeleccion(true);

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
        gestor.tomarSeleccionBolsin(7041);
        gestor.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_CONTENIDO_IGUAL, new int[0]);
        gestor.tomarConfirmacionSeleccion(true);

        IRepositorioCU28 repoNuevo = new MapeadorPersistenciaCU28();
        assertFalse(repoNuevo.estaVacia());
        GestorRecepcionBolsin gestorNuevo = new GestorRecepcionBolsin(repoNuevo.obtenerSesionActiva(), repoNuevo);
        gestorNuevo.tomarOpcionRegistrarRecepcionBolsin();

        Bolsin enBD = buscarBolsinPersistido(repoNuevo, 7041);
        assertEquals(Estado.NOMBRE_RECIBIDO_CM_DESTINO, enBD.getEstadoActual().getNombre());
        for (int precinto : gestorNuevo.getPrecintosBolsinesEnviados()) {
            assertNotEquals(7041, precinto, "El bolsín 7041 ya recibido no debe volver a aparecer como ENVIADO");
        }
    }

    @Test
    @DisplayName("BD H2: Flujo Alternativo A4 persiste remito parcial/completo y doc RECIBIDA_Y_RECHAZADA")
    void testPersistenciaFlujoAlternativoA4() {
        gestor.tomarSeleccionBolsin(8820);
        // Doc 203 pertenece al Remito 893
        gestor.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_NO_CORRESPONDE_DESTINO, new int[]{ 203 });
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
        gestor.tomarSeleccionBolsin(9150);
        gestor.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_REDIRIGIR_OTRA_AREA, new int[0]);
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
        gestor.tomarSeleccionBolsin(7041);
        gestor.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_CONTENIDO_IGUAL, new int[0]);
        gestor.tomarConfirmacionSeleccion(false);

        Bolsin enBD = buscarBolsinPersistido(new MapeadorPersistenciaCU28(), 7041);
        assertEquals(1, enBD.getCambiosDeEstado().size());
        assertEquals(Estado.NOMBRE_ENVIADO, enBD.getEstadoActual().getNombre());
        assertNull(enBD.getRemitos().get(0).getEstado());
    }
}
