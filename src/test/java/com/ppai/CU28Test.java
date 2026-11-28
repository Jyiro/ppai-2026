package com.ppai;

import com.ppai.boundary.cu28.PantallaRecepcionBolsin;
import com.ppai.controller.cu28.GestorRecepcionBolsin;
import com.ppai.domain.model.cu28.Bolsin;
import com.ppai.domain.model.cu28.ComisionMedica;
import com.ppai.domain.model.cu28.DetalleRemito;
import com.ppai.domain.model.cu28.Estado;
import com.ppai.domain.model.cu28.Remito;
import com.ppai.domain.model.cu28.Sesion;
import com.ppai.repository.ConexionBD;
import com.ppai.repository.cu28.IRepositorioCU28;
import com.ppai.repository.cu28.MapeadorPersistenciaCU28;
import com.ppai.repository.cu28.ReiniciarEscenarioBD;
import javafx.application.Platform;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas Unitarias y de Integración — CU 28: Registrar Recepción de Bolsín.
 * Valida el Flujo Básico y todos los Flujos Alternativos (A1..A6, Obs. 1)
 * contra la frontera 100% primitivos del GestorRecepcionBolsin y PantallaRecepcionBolsin.
 */
public class CU28Test {

    @TempDir
    Path tempDir;

    private IRepositorioCU28 repo;
    private GestorRecepcionBolsin gestor;

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX Toolkit ya inicializado
        }
    }

    @AfterAll
    static void restaurarConexion() {
        ConexionBD.restaurarUrlPorDefecto();
        ConexionBD.cerrar();
    }

    @BeforeEach
    void setUp() {
        ConexionBD.configurarUrl("jdbc:h2:file:" + tempDir.resolve("ppaidb-cu28").toAbsolutePath());
        ReiniciarEscenarioBD.reiniciar("cmc");
        repo = new MapeadorPersistenciaCU28();
        Sesion sesionCentral = repo.obtenerSesionActiva();
        gestor = new GestorRecepcionBolsin(sesionCentral, repo);
        gestor.tomarOpcionRegistrarRecepcionBolsin();
    }

    private Bolsin buscarBolsinEnRepo(int precinto) {
        return repo.obtenerTodosBolsines().stream()
                .filter(b -> b.getNroPrecinto() == precinto)
                .findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Flujo Básico (Opción 1): Contenido igual al registrado")
    void testFlujoBasicoOpcion1() {
        int[] precintos = gestor.getPrecintosBolsinesEnviados();
        assertEquals(3, precintos.length, "Debe encontrar 3 bolsines enviados para CMC");

        gestor.tomarSeleccionBolsin(7041);
        gestor.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_CONTENIDO_IGUAL, new int[0]);
        gestor.tomarConfirmacionSeleccion(true);

        Bolsin enBD = buscarBolsinEnRepo(7041);
        assertEquals(Estado.NOMBRE_RECIBIDO_CM_DESTINO, enBD.getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, enBD.getRemitos().get(0).getEstado().getNombre());
        for (DetalleRemito det : enBD.getRemitos().get(0).getDetalles()) {
            assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, det.getDocumentacion().getEstadoActual().getNombre());
            assertNotNull(det.getDocumentacion().obtenerCambioActual().getEmpleado());
        }

        assertNotNull(gestor.getUltimaNotificacionDatos());
        assertEquals("7041", gestor.getUltimaNotificacionDatos()[8]);
    }

    @Test
    @DisplayName("Flujo Alternativo A3 (Opción 2): Multi-remito con falta de documentación")
    void testFlujoAlternativoA3_FaltaDocumentacion() {
        gestor.tomarSeleccionBolsin(8820);
        // Doc 201 pertenece al Remito 892; Remito 893 queda completo
        gestor.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_FALTA_DOCUMENTACION, new int[]{ 201 });
        gestor.tomarConfirmacionSeleccion(true);

        Bolsin enBD = buscarBolsinEnRepo(8820);
        assertEquals(Estado.NOMBRE_RECIBIDO_CM_DESTINO, enBD.getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL, enBD.getRemitos().get(0).getEstado().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, enBD.getRemitos().get(1).getEstado().getNombre());

        assertEquals(Estado.NOMBRE_NO_RECIBIDA,
                enBD.getRemitos().get(0).getDetalles().get(0).getDocumentacion().getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO,
                enBD.getRemitos().get(0).getDetalles().get(1).getDocumentacion().getEstadoActual().getNombre());
    }

    @Test
    @DisplayName("Flujo Alternativo A4 (Opción 3): Documentación que no corresponde al destino")
    void testFlujoAlternativoA4_DocumentacionErronea() {
        gestor.tomarSeleccionBolsin(8820);
        // Doc 203 pertenece al Remito 893; Remito 892 queda completo
        gestor.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_NO_CORRESPONDE_DESTINO, new int[]{ 203 });
        gestor.tomarConfirmacionSeleccion(true);

        Bolsin enBD = buscarBolsinEnRepo(8820);
        assertEquals(Estado.NOMBRE_RECIBIDO_CM_DESTINO, enBD.getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, enBD.getRemitos().get(0).getEstado().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL, enBD.getRemitos().get(1).getEstado().getNombre());

        assertEquals(Estado.NOMBRE_RECIBIDA_Y_RECHAZADA,
                enBD.getRemitos().get(1).getDetalles().get(0).getDocumentacion().getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO,
                enBD.getRemitos().get(1).getDetalles().get(1).getDocumentacion().getEstadoActual().getNombre());
    }

    @Test
    @DisplayName("Flujo Alternativo A5 (Opción 4): Redirigir a otra área (Destino CMC)")
    void testFlujoAlternativoA5_RedirigirCMC() {
        gestor.tomarSeleccionBolsin(9150);
        gestor.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_REDIRIGIR_OTRA_AREA, new int[0]);
        gestor.tomarConfirmacionSeleccion(true);

        Bolsin enBD = buscarBolsinEnRepo(9150);
        assertEquals(Estado.NOMBRE_RECIBIDO_CM_DESTINO, enBD.getEstadoActual().getNombre());
        for (DetalleRemito det : enBD.getRemitos().get(0).getDetalles()) {
            assertEquals(Estado.NOMBRE_PARA_REDIRIGIR, det.getDocumentacion().getEstadoActual().getNombre());
        }
    }

    @Test
    @DisplayName("Flujo Alternativo A6: No confirmar mantiene el bolsín enviado sin notificar")
    void testFlujoAlternativoA6_NoConfirmar() {
        gestor.tomarSeleccionBolsin(7041);
        gestor.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_CONTENIDO_IGUAL, new int[0]);
        gestor.tomarConfirmacionSeleccion(false);

        Bolsin enBD = buscarBolsinEnRepo(7041);
        assertEquals(Estado.NOMBRE_ENVIADO, enBD.getEstadoActual().getNombre());
        assertNull(gestor.getUltimaNotificacionDatos());
        assertEquals(0, gestor.getOpcionSeleccionadaNumero());
    }

    @Test
    @DisplayName("Flujos Alternativos A1 y A2: Filtros de precinto y CM origen")
    void testFiltrosA1yA2() {
        int[] resInexistente = gestor.filtrarBolsines("999999", null);
        assertEquals(0, resInexistente.length);

        int[] resValido = gestor.filtrarBolsines("7041", null);
        assertArrayEquals(new int[]{ 7041 }, resValido);

        int[] resCMInexistente = gestor.filtrarBolsines(null, "CMJ Inexistente");
        assertEquals(0, resCMInexistente.length);

        int[] resCMMendoza = gestor.filtrarBolsines(null, "CMJ N° 4 - Mendoza");
        assertArrayEquals(new int[]{ 8820 }, resCMMendoza);
    }

    @Test
    @DisplayName("Flujo Alternativo A5 (Negativo): Opción 4 bloqueada en CM Regional")
    void testFlujoAlternativoA5_NoAplicaEnCMNoCentral() {
        ReiniciarEscenarioBD.reiniciar("rosario");
        GestorRecepcionBolsin gestorRosario = new GestorRecepcionBolsin(repo.obtenerSesionActiva(), repo);
        gestorRosario.tomarOpcionRegistrarRecepcionBolsin();

        int[] precintos = gestorRosario.getPrecintosBolsinesEnviados();
        assertTrue(precintos.length > 0);
        gestorRosario.tomarSeleccionBolsin(precintos[0]);

        String[][] opciones = gestorRosario.buscarOpcionesRecepcion();
        assertEquals("false", opciones[3][6], "Opción 4 debe venir deshabilitada para CM Regional");

        gestorRosario.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_REDIRIGIR_OTRA_AREA, new int[0]);
        assertEquals(0, gestorRosario.getOpcionSeleccionadaNumero());
    }

    @Test
    @DisplayName("Flujo Alternativo A1: Selección de precinto inexistente manejado sin errores")
    void testFlujoAlternativoA1_PrecintoInexistenteEnSeleccion() {
        gestor.tomarSeleccionBolsin(999999);
        assertEquals(-1, gestor.getPrecintoBolsinSeleccionado());
    }

    @Test
    @DisplayName("Obs. 1: Cancelar recepción en cualquier momento restablece estado transitorio")
    void testCancelacionEnCualquierMomento() {
        gestor.tomarSeleccionBolsin(7041);
        assertEquals(7041, gestor.getPrecintoBolsinSeleccionado());

        gestor.cancelarRecepcion();
        assertEquals(-1, gestor.getPrecintoBolsinSeleccionado());
        assertEquals(0, gestor.getOpcionSeleccionadaNumero());
        assertEquals(3, gestor.getPrecintosBolsinesEnviados().length);
    }

    @Test
    @DisplayName("Patrón Iterator: Bolsín con múltiples remitos recorre todos sus elementos")
    void testBolsinMultiplesRemitos() {
        ComisionMedica cmOrigen = new ComisionMedica(2, "CMJ N° 1 - Córdoba", "", "", "");
        ComisionMedica cmDestino = new ComisionMedica(1, "Comisión Médica Central (CMC) - CABA", "", "", "");
        Estado estEnviado = new Estado(Estado.NOMBRE_ENVIADO, "Enviado", Estado.AMBITO_BOLSIN);

        Remito r1 = new Remito(101, LocalDateTime.now(), cmOrigen, cmDestino);
        Remito r2 = new Remito(102, LocalDateTime.now(), cmOrigen, cmDestino);
        Bolsin bolsinMulti = new Bolsin(10, 777, LocalDateTime.now(), 2.0, cmOrigen, cmDestino,
                List.of(r1, r2), estEnviado, LocalDateTime.now(), null);

        assertEquals(2, bolsinMulti.getRemitos().size());
        assertNotNull(bolsinMulti.obtenerInformacionRemito());
    }

    @Test
    @DisplayName("Pantalla BCE: Selección y cancelación sincronizan correctamente sin entidades")
    void testSeleccionYCancelacionConPantalla() {
        PantallaRecepcionBolsin pantalla = new PantallaRecepcionBolsin(gestor);
        gestor.setPantalla(pantalla);
        pantalla.tomarOpcionRegistrarRecepcionBolsin();
        assertTrue(pantalla.isHabilitada());

        assertDoesNotThrow(() -> pantalla.tomarSeleccionBolsin(7041));
        assertEquals(7041, gestor.getPrecintoBolsinSeleccionado());
        assertEquals(7041, pantalla.getPrecintoSeleccionadoActual());

        pantalla.cancelar();
        assertEquals(-1, gestor.getPrecintoBolsinSeleccionado());
        assertEquals(-1, pantalla.getPrecintoSeleccionadoActual());

        pantalla.tomarSeleccionBolsin(8820);
        assertEquals(8820, gestor.getPrecintoBolsinSeleccionado());
        assertEquals(8820, pantalla.getPrecintoSeleccionadoActual());
    }

    @Test
    @DisplayName("Ciclo de vida multi-turno resetea referencias transitorias en finCU() y sincroniza Pantalla")
    void testCicloDeVidaMultiTurnoYSincronizacionPantalla() {
        PantallaRecepcionBolsin pantalla = new PantallaRecepcionBolsin(gestor);
        gestor.setPantalla(pantalla);
        pantalla.tomarOpcionRegistrarRecepcionBolsin();

        pantalla.tomarSeleccionBolsin(7041);
        assertEquals(7041, pantalla.getPrecintoSeleccionadoActual());

        pantalla.tomarSeleccionOpcion(GestorRecepcionBolsin.OPCION_CONTENIDO_IGUAL);
        assertEquals(1, gestor.getOpcionSeleccionadaNumero());

        pantalla.tomarConfirmacionSeleccion(true);
        assertEquals(-1, gestor.getPrecintoBolsinSeleccionado(), "finCU() limpia bolsinSeleccionado");
        assertEquals(0, gestor.getOpcionSeleccionadaNumero(), "finCU() limpia opcionSeleccionada");
        assertEquals(0, gestor.getNumerosDocsNoConformes().length, "finCU() limpia docs no conformes");

        // Turno 2: seleccionar el multi-remito 8820
        pantalla.tomarOpcionRegistrarRecepcionBolsin();
        assertEquals(2, gestor.getPrecintosBolsinesEnviados().length);
        pantalla.tomarSeleccionBolsin(8820);
        assertEquals(8820, gestor.getPrecintoBolsinSeleccionado());
        assertEquals(8820, pantalla.getPrecintoSeleccionadoActual());
    }
}
