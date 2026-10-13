package com.ppai;

import com.ppai.controller.cu28.GestorRecepcionBolsin;
import com.ppai.domain.model.cu28.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas Unitarias Automatizadas — CU 28: Registrar Recepción de Bolsín.
 * Valida el Flujo Básico y todos los Flujos Alternativos (A1, A2, A3, A4, A5, A6)
 * según las Reglas de Negocio del ERS v1.2 y la Máquina de Estados de Documentación.
 */
public class CU28Test {

    private GestorRecepcionBolsin gestor;
    private Sesion sesionCentral;

    @BeforeEach
    void setUp() {
        ComisionMedica cmCentral = new ComisionMedica(1L, "Comisión Médica Central (CMC) - CABA");
        Empleado empleado = new Empleado(100L, "Juan Pérez", cmCentral);
        Usuario usuario = new Usuario("jperez", empleado);
        sesionCentral = new Sesion(usuario);

        gestor = new GestorRecepcionBolsin(sesionCentral);
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

        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, bolsin.getRemito().getEstado().getNombre(),
                "El remito debe pasar a RECIBIDO_Y_ACEPTADO");

        for (DetalleRemito det : bolsin.getRemito().getDetalles()) {
            assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, det.getDocumentacion().getEstadoActual().getNombre(),
                    "Toda la documentación debe pasar a RECIBIDA_Y_ACEPTADA");
            assertNotNull(det.getDocumentacion().obtenerCambioActual().getEmpleado(),
                    "Debe registrarse el empleado responsable en el cambio de estado");
        }

        // Verificación de Notificación CU 29
        assertNotNull(gestor.getUltimaNotificacionEnviada(), "Debe haberse generado la notificación del CU 29");
        assertEquals(bolsin.getNumeroPrecinto(), gestor.getUltimaNotificacionEnviada().getNumeroPrecinto());
    }

    @Test
    @DisplayName("Flujo Alternativo A3 (Opción 2): No se recibe toda la documentación")
    void testFlujoAlternativoA3_FaltaDocumentacion() {
        List<Bolsin> bolsines = gestor.getBolsinesEnviados();
        Bolsin bolsin = bolsines.stream()
                .filter(b -> b.getRemito().getDetalles().size() > 1)
                .findFirst().orElseThrow();

        gestor.tomarSeleccionBolsin(bolsin);

        List<OpcionRecepcion> opciones = gestor.buscarOpcionesRecepcion();
        OpcionRecepcion opc2 = opciones.stream().filter(OpcionRecepcion::esFaltaDocumentacion).findFirst().orElseThrow();

        // Simulamos que el primer documento no llegó
        Documentacion docFaltante = bolsin.getRemito().getDetalles().get(0).getDocumentacion();
        Documentacion docPresente = bolsin.getRemito().getDetalles().get(1).getDocumentacion();

        gestor.tomarSeleccionOpcion(opc2, List.of(docFaltante));
        gestor.tomarConfirmacionSeleccion(true);

        // Verificaciones
        assertEquals(Estado.NOMBRE_RECIBIDO_CM_DESTINO, bolsin.getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL, bolsin.getRemito().getEstado().getNombre(),
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
                .filter(b -> b.getRemito().getDetalles().size() > 1)
                .findFirst().orElseThrow();

        gestor.tomarSeleccionBolsin(bolsin);

        List<OpcionRecepcion> opciones = gestor.buscarOpcionesRecepcion();
        OpcionRecepcion opc3 = opciones.stream().filter(OpcionRecepcion::esNoCorrespondeDestino).findFirst().orElseThrow();

        // Simulamos que el primer documento fue enviado por error
        Documentacion docErroneo = bolsin.getRemito().getDetalles().get(0).getDocumentacion();
        Documentacion docCorrecto = bolsin.getRemito().getDetalles().get(1).getDocumentacion();

        gestor.tomarSeleccionOpcion(opc3, List.of(docErroneo));
        gestor.tomarConfirmacionSeleccion(true);

        // Verificaciones
        assertEquals(Estado.NOMBRE_RECIBIDO_CM_DESTINO, bolsin.getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL, bolsin.getRemito().getEstado().getNombre());

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
        for (DetalleRemito det : bolsin.getRemito().getDetalles()) {
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
        ComisionMedica cmSinEnvios = new ComisionMedica(999L, "CMJ Inexistente");
        List<Bolsin> resOrigenInexistente = gestor.filtrarBolsines(null, cmSinEnvios);
        assertTrue(resOrigenInexistente.isEmpty(), "Filtro por CM sin envíos debe retornar vacío (A2)");
    }
}
