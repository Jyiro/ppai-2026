package com.ppai;

import com.ppai.boundary.cu28.PantallaRecepcionBolsin;
import com.ppai.controller.cu28.GestorRecepcionBolsin;
import com.ppai.domain.model.cu28.*;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas Unitarias Automatizadas — CU 28: Registrar Recepción de Bolsín.
 * Valida el Flujo Básico y todos los Flujos Alternativos (A1, A2, A3, A4, A5, A6)
 * según las Reglas de Negocio del ERS v1.2 y la Máquina de Estados de Documentación.
 */
public class CU28Test {

    @TempDir
    Path tempDir;

    private GestorRecepcionBolsin gestor;
    private Sesion sesionCentral;

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
        IRepositorioCU28 repo = new MapeadorPersistenciaCU28();
        sesionCentral = repo.obtenerSesionActiva();

        gestor = new GestorRecepcionBolsin(sesionCentral, repo);
        gestor.tomarOpcionRegistrarRecepcionBolsin();
    }

    @Test
    @DisplayName("Flujo Básico (Opción 1): Contenido igual al registrado")
    void testFlujoBasicoOpcion1() {
        List<Bolsin> bolsines = gestor.getBolsinesEnviados();
        assertFalse(bolsines.isEmpty(), "Debe encontrar bolsines enviados");

        Bolsin bolsin = bolsines.get(0);
        gestor.tomarSeleccionBolsin(bolsin);

        List<OpcionRecepcion> opciones = gestor.buscarOpcionesRecepcion();
        OpcionRecepcion opc1 = opciones.stream().filter(OpcionRecepcion::esContenidoIgual).findFirst().orElseThrow();

        // No hay docs no conformes en el flujo básico
        gestor.tomarSeleccionOpcion(opc1, Collections.emptyList());

        // Confirmar recepción
        gestor.tomarConfirmacionSeleccion(true);

        // Verificaciones de Estados
        assertEquals(Estado.NOMBRE_RECIBIDO_CM_DESTINO, bolsin.getEstadoActual().getNombre(),
                "El bolsín debe pasar a RECIBIDO_EN_CM_DESTINO");

        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, bolsin.getRemitos().get(0).getEstado().getNombre(),
                "El remito debe pasar a RECIBIDO_Y_ACEPTADO");

        for (DetalleRemito det : bolsin.getRemitos().get(0).getDetalles()) {
            assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, det.getDocumentacion().getEstadoActual().getNombre(),
                    "Toda la documentación debe pasar a RECIBIDA_Y_ACEPTADA");
            assertNotNull(det.getDocumentacion().obtenerCambioActual().getEmpleado(),
                    "Debe registrarse el empleado responsable en el cambio de estado");
        }

        // Verificación de Notificación CU 29
        assertNotNull(gestor.getUltimaNotificacionEnviada(), "Debe haberse generado la notificación del CU 29");
        assertEquals(String.valueOf(bolsin.getNroPrecinto()), gestor.getUltimaNotificacionEnviada().getNumeroPrecinto());
    }

    @Test
    @DisplayName("Flujo Alternativo A3 (Opción 2): No se recibe toda la documentación")
    void testFlujoAlternativoA3_FaltaDocumentacion() {
        List<Bolsin> bolsines = gestor.getBolsinesEnviados();
        Bolsin bolsin = bolsines.stream()
                .filter(b -> !b.getRemitos().isEmpty() && b.getRemitos().get(0).getDetalles().size() > 1)
                .findFirst().orElseThrow();

        gestor.tomarSeleccionBolsin(bolsin);

        List<OpcionRecepcion> opciones = gestor.buscarOpcionesRecepcion();
        OpcionRecepcion opc2 = opciones.stream().filter(OpcionRecepcion::esFaltaDocumentacion).findFirst().orElseThrow();

        // Simulamos que el primer documento no llegó
        Documentacion docFaltante = bolsin.getRemitos().get(0).getDetalles().get(0).getDocumentacion();
        Documentacion docPresente = bolsin.getRemitos().get(0).getDetalles().get(1).getDocumentacion();

        gestor.tomarSeleccionOpcion(opc2, List.of(docFaltante));
        gestor.tomarConfirmacionSeleccion(true);

        // Verificaciones
        assertEquals(Estado.NOMBRE_RECIBIDO_CM_DESTINO, bolsin.getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL, bolsin.getRemitos().get(0).getEstado().getNombre(),
                "En A3 el remito pasa a RECIBIDO_Y_ACEPTADO_PARCIAL");

        assertEquals(Estado.NOMBRE_NO_RECIBIDA, docFaltante.getEstadoActual().getNombre(),
                "El documento faltante debe pasar a NO_RECIBIDA");

        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, docPresente.getEstadoActual().getNombre(),
                "El documento presente debe pasar a RECIBIDA_Y_ACEPTADA");
    }

    @Test
    @DisplayName("Flujo Alternativo A4 (Opción 3): Existe documentación que no corresponde al destino")
    void testFlujoAlternativoA4_DocumentacionErronea() {
        List<Bolsin> bolsines = gestor.getBolsinesEnviados();
        Bolsin bolsin = bolsines.stream()
                .filter(b -> !b.getRemitos().isEmpty() && b.getRemitos().get(0).getDetalles().size() > 1)
                .findFirst().orElseThrow();

        gestor.tomarSeleccionBolsin(bolsin);

        List<OpcionRecepcion> opciones = gestor.buscarOpcionesRecepcion();
        OpcionRecepcion opc3 = opciones.stream().filter(OpcionRecepcion::esNoCorrespondeDestino).findFirst().orElseThrow();

        // Simulamos que el primer documento fue enviado por error
        Documentacion docErroneo = bolsin.getRemitos().get(0).getDetalles().get(0).getDocumentacion();
        Documentacion docCorrecto = bolsin.getRemitos().get(0).getDetalles().get(1).getDocumentacion();

        gestor.tomarSeleccionOpcion(opc3, List.of(docErroneo));
        gestor.tomarConfirmacionSeleccion(true);

        // Verificaciones
        assertEquals(Estado.NOMBRE_RECIBIDO_CM_DESTINO, bolsin.getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL, bolsin.getRemitos().get(0).getEstado().getNombre());

        assertEquals(Estado.NOMBRE_RECIBIDA_Y_RECHAZADA, docErroneo.getEstadoActual().getNombre(),
                "El documento erróneo debe pasar a RECIBIDA_Y_RECHAZADA");

        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, docCorrecto.getEstadoActual().getNombre(),
                "El documento correcto debe pasar a RECIBIDA_Y_ACEPTADA");
    }

    @Test
    @DisplayName("Flujo Alternativo A5 (Opción 4): Redirigir a otra área (Destino CMC)")
    void testFlujoAlternativoA5_RedirigirCMC() {
        List<Bolsin> bolsines = gestor.getBolsinesEnviados();
        Bolsin bolsin = bolsines.get(0);
        gestor.tomarSeleccionBolsin(bolsin);

        List<OpcionRecepcion> opciones = gestor.buscarOpcionesRecepcion();
        OpcionRecepcion opc4 = opciones.stream().filter(OpcionRecepcion::esRedirigirAOtraArea).findFirst().orElseThrow();

        gestor.tomarSeleccionOpcion(opc4, Collections.emptyList());
        gestor.tomarConfirmacionSeleccion(true);

        // Verificaciones
        assertEquals(Estado.NOMBRE_RECIBIDO_CM_DESTINO, bolsin.getEstadoActual().getNombre());
        for (DetalleRemito det : bolsin.getRemitos().get(0).getDetalles()) {
            assertEquals(Estado.NOMBRE_PARA_REDIRIGIR, det.getDocumentacion().getEstadoActual().getNombre(),
                    "En A5 las documentaciones deben pasar a PARA_REDIRIGIR");
        }
    }

    @Test
    @DisplayName("Flujo Alternativo A6: No confirmar mantiene el bolsín enviado")
    void testFlujoAlternativoA6_NoConfirmar() {
        List<Bolsin> bolsines = gestor.getBolsinesEnviados();
        Bolsin bolsin = bolsines.get(0);
        gestor.tomarSeleccionBolsin(bolsin);

        OpcionRecepcion opc1 = gestor.buscarOpcionesRecepcion().stream()
                .filter(OpcionRecepcion::esContenidoIgual)
                .findFirst()
                .orElseThrow();

        gestor.tomarSeleccionOpcion(opc1, Collections.emptyList());
        gestor.tomarConfirmacionSeleccion(false);

        assertEquals(Estado.NOMBRE_ENVIADO, bolsin.getEstadoActual().getNombre(),
                "Si el EB no confirma, el bolsín debe seguir ENVIADO");
        assertNull(gestor.getUltimaNotificacionEnviada(),
                "Si el EB no confirma, no debe generarse notificación CU 29");
    }

    @Test
    @DisplayName("Flujos Alternativos A1 y A2: Filtros de precinto y CM origen")
    void testFiltrosA1yA2() {
        // A1: Precinto inexistente
        List<Bolsin> resPrecintoInexistente = gestor.filtrarBolsines("PREC-INEXISTENTE-999", null);
        assertTrue(resPrecintoInexistente.isEmpty(), "Filtro por precinto inexistente debe retornar vacío (A1)");

        // Precinto existente
        List<Bolsin> resPrecintoValido = gestor.filtrarBolsines("7041", null);
        assertFalse(resPrecintoValido.isEmpty(), "Filtro por precinto existente debe encontrar bolsín");

        // A2: CM origen que no tiene envíos
        ComisionMedica cmSinEnvios = new ComisionMedica(999, "CMJ Inexistente", "", "", "");
        List<Bolsin> resOrigenInexistente = gestor.filtrarBolsines(null, cmSinEnvios);
        assertTrue(resOrigenInexistente.isEmpty(), "Filtro por CM sin envíos debe retornar vacío (A2)");
    }

    @Test
    @DisplayName("Flujo Alternativo A5 (Negativo): Opción 4 solo permitida en CMC, no aplica en CM Regional")
    void testFlujoAlternativoA5_NoAplicaEnCMNoCentral() {
        ReiniciarEscenarioBD.reiniciar("rosario");
        IRepositorioCU28 repoRosario = new MapeadorPersistenciaCU28();
        GestorRecepcionBolsin gestorRosario = new GestorRecepcionBolsin(repoRosario.obtenerSesionActiva(), repoRosario);
        gestorRosario.tomarOpcionRegistrarRecepcionBolsin();

        List<Bolsin> bolsines = gestorRosario.getBolsinesEnviados();
        assertFalse(bolsines.isEmpty(), "Debe haber bolsines para Rosario");
        Bolsin bolsin = bolsines.get(0);
        gestorRosario.tomarSeleccionBolsin(bolsin);

        List<OpcionRecepcion> opciones = gestorRosario.buscarOpcionesRecepcion();
        OpcionRecepcion opc4 = opciones.stream().filter(OpcionRecepcion::esRedirigirAOtraArea).findFirst().orElseThrow();
        assertTrue(opc4.isSoloCMC(), "Opción 4 debe indicar que solo aplica en CMC");

        // Intentar seleccionar opción 4 en CM Regional
        gestorRosario.tomarSeleccionOpcion(opc4, Collections.emptyList());
        assertNull(gestorRosario.getOpcionSeleccionada(), "En CM no central, la opción 4 no debe quedar seleccionada");
    }

    @Test
    @DisplayName("Flujo Alternativo A1: Selección de precinto inexistente manejado sin errores")
    void testFlujoAlternativoA1_PrecintoInexistenteEnSeleccion() {
        gestor.tomarSeleccionOpcion("PREC-INEXISTENTE-999");
        assertNull(gestor.getBolsinSeleccionado(), "No debe seleccionarse bolsín ante precinto inexistente");
    }

    @Test
    @DisplayName("Obs. 1: Cancelar recepción en cualquier momento restablece el estado")
    void testCancelacionEnCualquierMomento() {
        List<Bolsin> bolsines = gestor.getBolsinesEnviados();
        Bolsin bolsin = bolsines.get(0);
        gestor.tomarSeleccionBolsin(bolsin);
        assertNotNull(gestor.getBolsinSeleccionado());

        gestor.cancelarRecepcion();
        assertNull(gestor.getBolsinSeleccionado(), "El bolsín seleccionado debe ser null tras cancelar");
        assertNull(gestor.getRemitoSeleccionado(), "El remito seleccionado debe ser null tras cancelar");
        assertNull(gestor.getOpcionSeleccionada(), "La opción seleccionada debe ser null tras cancelar");
        assertFalse(gestor.getBolsinesEnviados().isEmpty(), "La lista de bolsines enviados debe seguir disponible");
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
    @DisplayName("Regresión Bug 1: Cancelación de recepción tras seleccionar opción con lista inmutable no lanza UnsupportedOperationException")
    void testCancelacionConListaInmutable() {
        List<Bolsin> bolsines = gestor.getBolsinesEnviados();
        Bolsin bolsin = bolsines.stream()
                .filter(b -> !b.getRemitos().isEmpty() && b.getRemitos().get(0).getDetalles().size() > 1)
                .findFirst().orElseThrow();

        gestor.tomarSeleccionBolsin(bolsin);

        List<OpcionRecepcion> opciones = gestor.buscarOpcionesRecepcion();
        OpcionRecepcion opc2 = opciones.stream().filter(OpcionRecepcion::esFaltaDocumentacion).findFirst().orElseThrow();

        Documentacion docFaltante = bolsin.getRemitos().get(0).getDetalles().get(0).getDocumentacion();

        // Pasar una lista inmutable (List.of)
        gestor.tomarSeleccionOpcion(opc2, List.of(docFaltante));

        // Cancelar recepción: debe limpiar y no arrojar UnsupportedOperationException
        assertDoesNotThrow(() -> gestor.cancelarRecepcion(),
                "cancelarRecepcion() debe soportar listas inmodificables sin lanzar excepción");

        assertNull(gestor.getBolsinSeleccionado(), "El bolsín seleccionado debe ser null tras cancelar");
        assertNull(gestor.getRemitoSeleccionado(), "El remito seleccionado debe ser null tras cancelar");
        assertNull(gestor.getOpcionSeleccionada(), "La opción seleccionada debe ser null tras cancelar");
        assertTrue(gestor.getDocumentacionesNoConformes().isEmpty(), "La lista de documentaciones no conformes debe reiniciarse vacía");
    }

    @Test
    @DisplayName("Regresión Bug 2: Selección de bolsín por precinto con PantallaRecepcionBolsin no lanza NullPointerException")
    void testSeleccionBolsinPorPrecintoConPantalla() {
        PantallaRecepcionBolsin pantalla = new PantallaRecepcionBolsin(gestor);
        gestor.setPantalla(pantalla);
        gestor.tomarOpcionRegistrarRecepcionBolsin();

        // Selección por precinto string lookup (flujo A1 / búsqueda de precinto)
        assertDoesNotThrow(() -> gestor.tomarSeleccionOpcion("7041"),
                "Seleccionar por precinto con pantalla asociada no debe lanzar NullPointerException");

        assertNotNull(gestor.getBolsinSeleccionado(), "El bolsín 7041 debe haber sido seleccionado en el gestor");
        assertEquals(7041, gestor.getBolsinSeleccionado().getNroPrecinto());
        assertEquals("7041", pantalla.tomarSeleccionBolsin(), "La pantalla debe sincronizar el bolsín seleccionado");
    }

    @Test
    @DisplayName("Regresión Challenger 1_3: Cancelación y posterior selección por precinto de otro bolsín sincroniza Pantalla")
    void testCancelacionYSiguienteSeleccionPrecintoConPantalla() {
        PantallaRecepcionBolsin pantalla = new PantallaRecepcionBolsin(gestor);
        gestor.setPantalla(pantalla);
        gestor.tomarOpcionRegistrarRecepcionBolsin();

        // Paso 1: Seleccionar bolsín 1 por precinto
        gestor.tomarSeleccionOpcion("7041");
        assertNotNull(gestor.getBolsinSeleccionado());
        assertEquals(7041, gestor.getBolsinSeleccionado().getNroPrecinto());
        assertEquals("7041", pantalla.tomarSeleccionBolsin());

        // Paso 2: Cancelar recepción
        gestor.cancelarRecepcion();
        assertNull(gestor.getBolsinSeleccionado());

        // Paso 3: Seleccionar un bolsín diferente por precinto
        gestor.tomarSeleccionOpcion("8820");
        assertNotNull(gestor.getBolsinSeleccionado());
        assertEquals(8820, gestor.getBolsinSeleccionado().getNroPrecinto());
        assertEquals("8820", pantalla.tomarSeleccionBolsin(),
                "La pantalla debe actualizar su selección al nuevo bolsín tras la cancelación");
    }

    @Test
    @DisplayName("Regresión Challenger 1_4: Ciclo de vida multi-turno resetea referencias transitorias al finalizar (finCU)")
    void testCicloDeVidaMultiTurnoReseteaReferenciasTransitorias() {
        // Turno 1: Ejecutar recepción completa hasta finCU()
        List<Bolsin> bolsines = gestor.getBolsinesEnviados();
        assertFalse(bolsines.isEmpty(), "Debe haber bolsines para procesar el Turno 1");
        Bolsin bolsinTurno1 = bolsines.get(0);

        gestor.tomarSeleccionBolsin(bolsinTurno1);
        assertNotNull(gestor.getBolsinSeleccionado());
        assertNotNull(gestor.getRemitoSeleccionado());

        OpcionRecepcion opc1 = gestor.buscarOpcionesRecepcion().stream()
                .filter(OpcionRecepcion::esContenidoIgual)
                .findFirst()
                .orElseThrow();
        gestor.tomarSeleccionOpcion(opc1, Collections.emptyList());
        assertNotNull(gestor.getOpcionSeleccionada());

        // Confirmar: ejecuta actualizarEstadoBolsin(), notificarRecepcion(), y finCU()
        gestor.tomarConfirmacionSeleccion(true);

        // Aserciones de finCU()
        assertNull(gestor.getBolsinSeleccionado(), "bolsinSeleccionado debe ser null tras finCU()");
        assertNull(gestor.getRemitoSeleccionado(), "remitoSeleccionado debe ser null tras finCU()");
        assertNull(gestor.getOpcionSeleccionada(), "opcionSeleccionada debe ser null tras finCU()");
        assertTrue(gestor.getDocumentacionesNoConformes().isEmpty(), "documentacionesNoConformes debe estar vacía tras finCU()");

        // Turno 2: Iniciar nuevo turno defensivamente
        gestor.tomarOpcionRegistrarRecepcionBolsin();
        assertNull(gestor.getBolsinSeleccionado(), "bolsinSeleccionado debe seguir null al iniciar Turno 2");
        assertNull(gestor.getRemitoSeleccionado(), "remitoSeleccionado debe seguir null al iniciar Turno 2");
        assertNull(gestor.getOpcionSeleccionada(), "opcionSeleccionada debe seguir null al iniciar Turno 2");
        assertTrue(gestor.getDocumentacionesNoConformes().isEmpty(), "documentacionesNoConformes debe seguir vacía al iniciar Turno 2");
    }

    @Test
    @DisplayName("Regresión Challenger 1_4: Cambio a escenario Regional (Rosario) rechaza Opción 4 y asegura opcionSeleccionada nula")
    void testCambioEscenarioRosarioRechazaOpcion4YSinEstadoResidual() {
        // En CMC seleccionamos Opción 4 en Turno 1
        List<Bolsin> bolsinesCMC = gestor.getBolsinesEnviados();
        gestor.tomarSeleccionBolsin(bolsinesCMC.get(0));
        OpcionRecepcion opc4 = gestor.buscarOpcionesRecepcion().stream()
                .filter(OpcionRecepcion::esRedirigirAOtraArea)
                .findFirst()
                .orElseThrow();
        gestor.tomarSeleccionOpcion(opc4, Collections.emptyList());
        assertEquals(opc4, gestor.getOpcionSeleccionada(), "Opción 4 debe permitirse en CMC");

        // Cambiar a Escenario 2: CM Regional Rosario
        gestor.cargarEscenario(2);
        assertNull(gestor.getOpcionSeleccionada(), "Tras cargar escenario Rosario, opcionSeleccionada debe resetearse");

        // Seleccionar un bolsín de Rosario
        List<Bolsin> bolsinesRosario = gestor.getBolsinesEnviados();
        assertFalse(bolsinesRosario.isEmpty(), "Debe haber bolsines para Rosario");
        gestor.tomarSeleccionBolsin(bolsinesRosario.get(0));

        // Intentar seleccionar Opción 4 en Rosario
        gestor.tomarSeleccionOpcion(opc4, Collections.emptyList());
        assertNull(gestor.getOpcionSeleccionada(),
                "La opción 4 rechazada en CM Regional debe dejar opcionSeleccionada en null sin retener estado residual");
    }

    @Test
    @DisplayName("Regresión Challenger 1_4: Pantalla sincroniza selección entre múltiples turnos tras finCU")
    void testPantallaSincronizaEntreMultiplesTurnosTrasFinCU() {
        PantallaRecepcionBolsin pantalla = new PantallaRecepcionBolsin(gestor);
        gestor.setPantalla(pantalla);
        gestor.tomarOpcionRegistrarRecepcionBolsin();

        // Turno 1: Seleccionar 7041, Opción 1, Confirmar
        gestor.tomarSeleccionOpcion("7041");
        assertEquals("7041", pantalla.tomarSeleccionBolsin());

        OpcionRecepcion opc1 = gestor.buscarOpcionesRecepcion().stream()
                .filter(OpcionRecepcion::esContenidoIgual)
                .findFirst()
                .orElseThrow();
        gestor.tomarSeleccionOpcion(opc1, Collections.emptyList());
        gestor.tomarConfirmacionSeleccion(true);

        // Turno 2: Iniciar nuevo turno desde pantalla
        pantalla.tomarOpcionRegistrarRecepcionBolsin();
        gestor.tomarSeleccionOpcion("8820");

        assertEquals(8820, gestor.getBolsinSeleccionado().getNroPrecinto());
        assertEquals("8820", pantalla.tomarSeleccionBolsin(),
                "En el Turno 2 la pantalla debe reflejar 8820 y no retener 7041 del Turno 1");
    }
}
