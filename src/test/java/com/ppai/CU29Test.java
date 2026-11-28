package com.ppai;

import com.ppai.boundary.cu29.InterfazServidorCorreo;
import com.ppai.controller.cu28.GestorRecepcionBolsin;
import com.ppai.controller.cu29.GestorNotificarRecepcionBolsin;
import com.ppai.domain.model.cu28.Bolsin;
import com.ppai.domain.model.cu28.ComisionMedica;
import com.ppai.domain.model.cu28.Empleado;
import com.ppai.domain.model.cu28.Estado;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gate de Verificación Íntegra de Fase 3:
 * Pruebas unitarias y de integración para el CU 29 (<<includes>> GestorNotificarRecepcionBolsin)
 * y su disparo estrictamente post-commit desde GestorRecepcionBolsin.
 */
public class CU29Test {

    @TempDir
    Path tempDir;

    private IRepositorioCU28 repositorio;

    private static final class SpyServidorCorreo extends InterfazServidorCorreo {
        int envios = 0;
        String ultimoDestino;
        String ultimoAsunto;
        String ultimoCuerpo;

        @Override
        public boolean enviarNotificacion(String emailDestino, String asunto, String cuerpo) {
            this.envios++;
            this.ultimoDestino = emailDestino;
            this.ultimoAsunto = asunto;
            this.ultimoCuerpo = cuerpo;
            return true;
        }
    }

    @BeforeEach
    void setUp() {
        ConexionBD.configurarUrl("jdbc:h2:file:" + tempDir.resolve("ppaidb-cu29").toAbsolutePath());
        ReiniciarEscenarioBD.reiniciar("cmc");
        repositorio = new MapeadorPersistenciaCU28();
    }

    @AfterAll
    static void restaurar() {
        ConexionBD.restaurarUrlPorDefecto();
        ConexionBD.cerrar();
    }

    @Test
    @DisplayName("CU 29 unitario: arma correo multi-remito y llama a InterfazServidorCorreo solo con String")
    void testUnitarioGestorNotificarMultiRemito() {
        SpyServidorCorreo spy = new SpyServidorCorreo();
        GestorNotificarRecepcionBolsin cu29 = new GestorNotificarRecepcionBolsin(spy);

        String[] comprobante = cu29.notificarRecepcionBolsin(
                8820,
                "CMJ N° 4 - Mendoza",
                "despacho.mendoza@srt.gob.ar",
                "Recibido en CM Destino",
                new String[][]{
                        { "892", "Recibido parcial" },
                        { "893", "Recibido y aceptado" }
                },
                new String[][]{
                        { "892", "Expediente", "Asunto 201", "No recibida" },
                        { "892", "Estudio", "Asunto 202", "Recibido y aceptado" },
                        { "893", "Carta Documento", "Asunto 203", "Recibido y aceptado" },
                        { "893", "Dictamen", "Asunto 204", "Recibido y aceptado" }
                },
                "28/11/2026 10:30",
                "Juan Pérez"
        );

        assertEquals(1, spy.envios);
        assertEquals("despacho.mendoza@srt.gob.ar", spy.ultimoDestino);
        assertTrue(spy.ultimoAsunto.contains("8820"));
        assertTrue(spy.ultimoCuerpo.contains("Remito N° 892"));
        assertTrue(spy.ultimoCuerpo.contains("Remito N° 893"));
        assertTrue(spy.ultimoCuerpo.contains("Recibido parcial"));
        assertTrue(spy.ultimoCuerpo.contains("Asunto 201"));
        assertTrue(spy.ultimoCuerpo.contains("No recibida"));
        assertTrue(spy.ultimoCuerpo.contains("Asunto 204"));
        assertEquals("CMJ N° 4 - Mendoza", comprobante[0]);
        assertEquals("892 (Recibido parcial), 893 (Recibido y aceptado)", comprobante[7]);
    }

    @Test
    @DisplayName("Post-commit obligatorio: si falla persistirRecepcion() (rollback), NO se envía correo CU 29")
    void testNoEnviaCorreoSiFallaPersistencia() {
        SpyServidorCorreo spy = new SpyServidorCorreo();
        IRepositorioCU28 repoConFallo = new IRepositorioCU28() {
            @Override public boolean estaVacia() { return repositorio.estaVacia(); }
            @Override public Sesion obtenerSesionActiva() { return repositorio.obtenerSesionActiva(); }
            @Override public List<Estado> obtenerEstados() { return repositorio.obtenerEstados(); }
            @Override public List<ComisionMedica> obtenerComisionesMedicas() { return repositorio.obtenerComisionesMedicas(); }
            @Override public List<Bolsin> obtenerTodosBolsines() { return repositorio.obtenerTodosBolsines(); }
            @Override public Bolsin actualizarBolsin(Bolsin b, Empleado e) {
                throw new RuntimeException("Fallo simulado de BD -> rollback");
            }
        };

        GestorRecepcionBolsin gestor = new GestorRecepcionBolsin(
                repoConFallo.obtenerSesionActiva(), repoConFallo, new GestorNotificarRecepcionBolsin(spy));
        gestor.tomarOpcionRegistrarRecepcionBolsin();
        gestor.tomarSeleccionBolsin(7041);
        gestor.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_CONTENIDO_IGUAL, new int[0]);

        assertThrows(RuntimeException.class, () -> gestor.tomarConfirmacionSeleccion(true));
        assertEquals(0, spy.envios, "No debe enviarse notificación si falló la persistencia");
        assertNull(gestor.getUltimaNotificacionDatos());
    }

    @Test
    @DisplayName("A6 y Obs. 1: no confirmar o cancelar nunca disparan el envío de correo CU 29")
    void testNoEnviaCorreoEnA6NiCancelacion() {
        SpyServidorCorreo spy = new SpyServidorCorreo();
        GestorRecepcionBolsin gestor = new GestorRecepcionBolsin(
                repositorio.obtenerSesionActiva(), repositorio, new GestorNotificarRecepcionBolsin(spy));
        gestor.tomarOpcionRegistrarRecepcionBolsin();

        // A6: no confirmar
        gestor.tomarSeleccionBolsin(7041);
        gestor.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_CONTENIDO_IGUAL, new int[0]);
        gestor.tomarConfirmacionSeleccion(false);
        assertEquals(0, spy.envios);

        // Obs. 1: cancelar
        gestor.tomarSeleccionBolsin(8820);
        gestor.cancelarRecepcion();
        assertEquals(0, spy.envios);
    }

    @Test
    @DisplayName("Integración CU 28 -> CU 29 multi-remito (Opción 1, A3, A4, A5) incluye todos los remitos y docs")
    void testIntegracionMultiRemitoOpciones1A3A4A5() {
        // Caso 1: A3 sobre bolsín 8820 (2 remitos: 892 con doc 201 faltante, 893 completo)
        SpyServidorCorreo spyA3 = new SpyServidorCorreo();
        GestorRecepcionBolsin g1 = new GestorRecepcionBolsin(
                repositorio.obtenerSesionActiva(), repositorio, new GestorNotificarRecepcionBolsin(spyA3));
        g1.tomarOpcionRegistrarRecepcionBolsin();
        g1.tomarSeleccionBolsin(8820);
        g1.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_FALTA_DOCUMENTACION, new int[]{ 201 });
        g1.tomarConfirmacionSeleccion(true);

        assertEquals(1, spyA3.envios);
        assertEquals("despacho.mendoza@srt.gob.ar", spyA3.ultimoDestino);
        assertTrue(spyA3.ultimoCuerpo.contains("Remito N° 892"));
        assertTrue(spyA3.ultimoCuerpo.contains("Remito N° 893"));
        assertTrue(spyA3.ultimoCuerpo.contains("EXP-2026-1188"));
        assertTrue(spyA3.ultimoCuerpo.contains("DIC-2026-505"));
        assertTrue(spyA3.ultimoCuerpo.contains("No recibida"));
        assertTrue(spyA3.ultimoCuerpo.contains("Recibido parcial"));

        // Caso 2: A4 sobre bolsín 7041 (doc 101 rechazada)
        SpyServidorCorreo spyA4 = new SpyServidorCorreo();
        GestorRecepcionBolsin g2 = new GestorRecepcionBolsin(
                repositorio.obtenerSesionActiva(), repositorio, new GestorNotificarRecepcionBolsin(spyA4));
        g2.tomarOpcionRegistrarRecepcionBolsin();
        g2.tomarSeleccionBolsin(7041);
        g2.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_NO_CORRESPONDE_DESTINO, new int[]{ 101 });
        g2.tomarConfirmacionSeleccion(true);

        assertEquals(1, spyA4.envios);
        assertEquals("despacho.cordoba@srt.gob.ar", spyA4.ultimoDestino);
        assertTrue(spyA4.ultimoCuerpo.contains("Recibida y rechazada"));

        // Caso 3: A5 sobre bolsín 9150 (doc 301 para redirigir)
        SpyServidorCorreo spyA5 = new SpyServidorCorreo();
        GestorRecepcionBolsin g3 = new GestorRecepcionBolsin(
                repositorio.obtenerSesionActiva(), repositorio, new GestorNotificarRecepcionBolsin(spyA5));
        g3.tomarOpcionRegistrarRecepcionBolsin();
        g3.tomarSeleccionBolsin(9150);
        g3.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_REDIRIGIR_OTRA_AREA, new int[0]);
        g3.tomarConfirmacionSeleccion(true);

        assertEquals(1, spyA5.envios);
        assertEquals("despacho.laplata@srt.gob.ar", spyA5.ultimoDestino);
        assertTrue(spyA5.ultimoCuerpo.contains("Para redirigir"));
        g3.buscarBolsinEstadoEnviado();
        assertEquals(0, g3.getPrecintosBolsinesEnviados().length);
    }
}
