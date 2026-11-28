package com.ppai;

import com.ppai.controller.cu28.GestorRecepcionBolsin;
import com.ppai.domain.model.cu28.*;
import com.ppai.iterator.*;
import com.ppai.repository.cu28.IRepositorioCU28;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de pruebas unitarias, de integración y estructurales para el
 * Patrón Iterator (GoF) implementado artesanalmente para el CU 28.
 *
 * Valida:
 * 1. Cero uso de java.util.Iterator / java.lang.Iterable.
 * 2. Realización estricta de IAgregado e IIterador (elementoActual() sin actual()).
 * 3. Semántica exacta de IteradorBolsin sin look-ahead.
 * 4. Recorrido completo y retorno String[][] en Bolsin y Remito con múltiples remitos/detalles.
 */
public class IteratorPatternTest {

    // =========================================================================
    // 1. Pruebas de Reflexión y Restricción Estructural (Zero JDK Iterator)
    // =========================================================================

    @Test
    @DisplayName("R1 - Reflexión: Ninguna interfaz ni clase del patrón debe extender o implementar java.util.Iterator")
    void testNoJdkIteratorInterfacesUsed() {
        assertFalse(java.util.Iterator.class.isAssignableFrom(IIterador.class));
        assertFalse(java.util.Iterator.class.isAssignableFrom(IteradorBolsin.class));
        assertFalse(java.util.Iterator.class.isAssignableFrom(IteradorRemito.class));
        assertFalse(java.util.Iterator.class.isAssignableFrom(IteradorDetalleRemito.class));
    }

    @Test
    @DisplayName("R1 - Reflexión: Ningún agregado debe extender o implementar java.lang.Iterable")
    void testNoJdkIterableInterfacesUsed() {
        assertFalse(Iterable.class.isAssignableFrom(IAgregado.class));
        assertFalse(Iterable.class.isAssignableFrom(GestorRecepcionBolsin.class));
        assertFalse(Iterable.class.isAssignableFrom(Bolsin.class));
        assertFalse(Iterable.class.isAssignableFrom(Remito.class));
    }

    @Test
    @DisplayName("R1 - Estático: Verificación de código fuente sin importaciones de java.util.Iterator ni Iterable")
    void testNoJdkIteratorImportsInSource() throws IOException {
        Path iteratorPkg = Paths.get("src", "main", "java", "com", "ppai", "iterator");
        assertTrue(Files.exists(iteratorPkg));

        try (Stream<Path> paths = Files.walk(iteratorPkg)) {
            List<Path> javaFiles = paths.filter(p -> p.toString().endsWith(".java")).toList();
            assertFalse(javaFiles.isEmpty());

            for (Path javaFile : javaFiles) {
                String content = Files.readString(javaFile);
                assertFalse(content.contains("import java.util.Iterator;"));
                assertFalse(content.contains("import java.util.Iterable;"));
                assertFalse(content.contains("import java.lang.Iterable;"));
            }
        }
    }

    @Test
    @DisplayName("R1/R2 - Reflexión: GestorRecepcionBolsin, Bolsin y Remito deben implementar IAgregado")
    void testAgregadosImplementIAgregado() {
        assertTrue(IAgregado.class.isAssignableFrom(GestorRecepcionBolsin.class));
        assertTrue(IAgregado.class.isAssignableFrom(Bolsin.class));
        assertTrue(IAgregado.class.isAssignableFrom(Remito.class));
    }

    @Test
    @DisplayName("R1 - Reflexión: IteradorBolsin, IteradorRemito e IteradorDetalleRemito implementan IIterador")
    void testIteradoresImplementIIterador() {
        assertTrue(IIterador.class.isAssignableFrom(IteradorBolsin.class));
        assertTrue(IIterador.class.isAssignableFrom(IteradorRemito.class));
        assertTrue(IIterador.class.isAssignableFrom(IteradorDetalleRemito.class));
    }

    // =========================================================================
    // 2. Pruebas Unitarias de IteradorBolsin (Sin look-ahead, filtro String)
    // =========================================================================

    @Test
    @DisplayName("IteradorBolsin: Comportamiento con colección vacía y colección nula")
    void testIteradorBolsinColeccionVaciaYNula() {
        Object[] filtros = new Object[]{ "Comisión Médica Central (CMC) - CABA" };

        IteradorBolsin itVacio = new IteradorBolsin(new Bolsin[0], filtros);
        assertTrue(itVacio.haFinalizado());
        assertNull(itVacio.elementoActual());
        itVacio.primero();
        assertTrue(itVacio.haFinalizado());
        itVacio.siguiente();
        assertTrue(itVacio.haFinalizado());

        IteradorBolsin itNulo = new IteradorBolsin(null, filtros);
        assertTrue(itNulo.haFinalizado());
        assertNull(itNulo.elementoActual());
    }

    @Test
    @DisplayName("IteradorBolsin: Sin look-ahead — primero() en 0, elementoActual() retorna null cuando no cumple filtro")
    void testIteradorBolsinFiltradoSinLookAhead() {
        ComisionMedica cmCentral = new ComisionMedica(1, "Comisión Médica Central (CMC) - CABA",
                "Av. Roque Sáenz Peña 1100", "despacho.cmc@srt.gob.ar", "011-4321-1000");
        ComisionMedica cmCordoba = new ComisionMedica(10, "CMJ N° 1 - Córdoba Capital",
                "Av. Colón 450", "despacho.cordoba@srt.gob.ar", "0351-420-1100");
        ComisionMedica cmRosario = new ComisionMedica(2, "CMJ N° 7 - Rosario",
                "Bv. Oroño 850", "despacho.rosario@srt.gob.ar", "0341-440-2000");

        Estado estEnviado = new Estado(Estado.NOMBRE_ENVIADO, "Enviado", Estado.AMBITO_BOLSIN);
        Estado estRecibido = new Estado(Estado.NOMBRE_RECIBIDO_CM_DESTINO, "Recibido", Estado.AMBITO_BOLSIN);
        LocalDateTime ahora = LocalDateTime.now();

        Bolsin bol1 = new Bolsin(1, 1111, ahora, 2.0, cmCordoba, cmCentral, List.of(), estEnviado, ahora, null);
        Bolsin bol2 = new Bolsin(2, 2222, ahora, 2.0, cmCordoba, cmCentral, List.of(), estRecibido, ahora, null);
        Bolsin bol3 = new Bolsin(3, 3333, ahora, 2.0, cmCordoba, cmRosario, List.of(), estEnviado, ahora, null);
        Bolsin bol4 = new Bolsin(4, 4444, ahora, 2.0, cmRosario, cmCentral, List.of(), estEnviado, ahora, null);

        Bolsin[] array = new Bolsin[]{ bol1, bol2, bol3, bol4 };
        Object[] filtros = new Object[]{ cmCentral.getNombre() };

        IteradorBolsin it = new IteradorBolsin(array, filtros);
        it.primero();

        // Índice 0: cumple (ENVIADO + cmCentral)
        assertFalse(it.haFinalizado());
        Object elem0 = it.elementoActual();
        assertNotNull(elem0);
        assertEquals(1111, ((Bolsin) elem0).getNroPrecinto());

        // Índice 1: avanza secuencialmente (sin saltar) y elementoActual() retorna null por estado != ENVIADO
        it.siguiente();
        assertFalse(it.haFinalizado(), "siguiente() avanza 1 a 1 sin look-ahead");
        assertFalse(it.comprobarFiltro(filtros));
        assertNull(it.elementoActual(), "elementoActual() debe retornar null si comprobarFiltro() es false");

        // Índice 2: avanza secuencialmente y elementoActual() retorna null por CM destino distinta
        it.siguiente();
        assertFalse(it.haFinalizado(), "siguiente() avanza 1 a 1 sin look-ahead");
        assertFalse(it.comprobarFiltro(filtros));
        assertNull(it.elementoActual(), "elementoActual() debe retornar null si CM destino no coincide");

        // Índice 3: cumple (ENVIADO + cmCentral)
        it.siguiente();
        assertFalse(it.haFinalizado());
        Object elem3 = it.elementoActual();
        assertNotNull(elem3);
        assertEquals(4444, ((Bolsin) elem3).getNroPrecinto());

        // Índice 4: finalizado
        it.siguiente();
        assertTrue(it.haFinalizado());
        assertNull(it.elementoActual());
    }

    @Test
    @DisplayName("IteradorBolsin: Reinicio de puntero con primero() incluso si índice 0 no cumple filtro")
    void testIteradorBolsinReinicioPrimero() {
        ComisionMedica cmCentral = new ComisionMedica(1, "Comisión Médica Central (CMC) - CABA", "", "cmc@srt.gob.ar", "");
        ComisionMedica cmOrigen = new ComisionMedica(10, "CMJ N° 1 - Córdoba Capital", "", "cba@srt.gob.ar", "");
        Estado estEnviado = new Estado(Estado.NOMBRE_ENVIADO, "Enviado", Estado.AMBITO_BOLSIN);
        Estado estRecibido = new Estado(Estado.NOMBRE_RECIBIDO_CM_DESTINO, "Recibido", Estado.AMBITO_BOLSIN);
        LocalDateTime ahora = LocalDateTime.now();

        Bolsin bolInvalido = new Bolsin(1, 100, ahora, 1.0, cmOrigen, cmCentral, List.of(), estRecibido, ahora, null);
        Bolsin bolValido = new Bolsin(2, 200, ahora, 1.0, cmOrigen, cmCentral, List.of(), estEnviado, ahora, null);

        IteradorBolsin it = new IteradorBolsin(new Bolsin[]{ bolInvalido, bolValido }, new Object[]{ cmCentral.getNombre() });
        it.primero();
        assertNull(it.elementoActual());

        it.siguiente();
        assertEquals(200, ((Bolsin) it.elementoActual()).getNroPrecinto());

        it.siguiente();
        assertTrue(it.haFinalizado());

        // Reinicio con primero() vuelve a índice 0 (no salta al 1)
        it.primero();
        assertFalse(it.haFinalizado());
        assertNull(it.elementoActual());
        it.siguiente();
        assertEquals(200, ((Bolsin) it.elementoActual()).getNroPrecinto());
    }

    // =========================================================================
    // 3. Pruebas Unitarias de IteradorRemito e IteradorDetalleRemito
    // =========================================================================

    @Test
    @DisplayName("IteradorRemito: Recorrido secuencial completo y colección vacía/nula")
    void testIteradorRemitoRecorrido() {
        ComisionMedica cmOrig = new ComisionMedica(10, "Córdoba", "", "", "");
        ComisionMedica cmDest = new ComisionMedica(1, "Central", "", "", "");
        LocalDateTime ahora = LocalDateTime.now();

        Remito r1 = new Remito(1, ahora, cmOrig, cmDest);
        Remito r2 = new Remito(2, ahora, cmOrig, cmDest);
        Remito r3 = new Remito(3, ahora, cmOrig, cmDest);

        IteradorRemito it = new IteradorRemito(new Remito[]{ r1, r2, r3 });
        it.primero();

        assertFalse(it.haFinalizado());
        assertEquals(1, ((Remito) it.elementoActual()).getNumero());

        it.siguiente();
        assertFalse(it.haFinalizado());
        assertEquals(2, ((Remito) it.elementoActual()).getNumero());

        it.siguiente();
        assertFalse(it.haFinalizado());
        assertEquals(3, ((Remito) it.elementoActual()).getNumero());

        it.siguiente();
        assertTrue(it.haFinalizado());
        assertNull(it.elementoActual());

        it.primero();
        assertFalse(it.haFinalizado());
        assertEquals(1, ((Remito) it.elementoActual()).getNumero());

        IteradorRemito itVacio = new IteradorRemito(new Remito[0]);
        assertTrue(itVacio.haFinalizado());
        assertNull(itVacio.elementoActual());

        IteradorRemito itNulo = new IteradorRemito(null);
        assertTrue(itNulo.haFinalizado());
        assertNull(itNulo.elementoActual());
    }

    @Test
    @DisplayName("IteradorDetalleRemito: Recorrido y obtención de asunto y tipo de documento")
    void testIteradorDetalleRemitoRecorrido() {
        TipoDocumento td1 = new TipoDocumento("Dictamen Médico", "Dictamen");
        TipoDocumento td2 = new TipoDocumento("Historia Clínica", "Historia");
        ComisionMedica cmOrig = new ComisionMedica(10, "Córdoba", "", "", "");
        Estado estEnviado = new Estado(Estado.NOMBRE_EN_BOLSIN_ENVIADO, "En bolsín enviado", Estado.AMBITO_DOCUMENTACION);
        LocalDateTime ahora = LocalDateTime.now();

        Documentacion doc1 = new Documentacion(10, "Expediente N° 5541/2026", ahora, td1, cmOrig, estEnviado, ahora, null);
        Documentacion doc2 = new Documentacion(20, "Estudio Traumatológico", ahora, td2, cmOrig, estEnviado, ahora, null);

        DetalleRemito det1 = new DetalleRemito(doc1, "Junta Médica");
        DetalleRemito det2 = new DetalleRemito(doc2, "Diagnóstico");

        IteradorDetalleRemito it = new IteradorDetalleRemito(new DetalleRemito[]{ det1, det2 });
        it.primero();

        assertFalse(it.haFinalizado());
        DetalleRemito actual1 = (DetalleRemito) it.elementoActual();
        assertEquals(doc1, actual1.getDocumentacion());
        assertArrayEquals(new String[]{ "Expediente N° 5541/2026", "Dictamen Médico" }, actual1.obtenerAsuntoYTipoDoc());

        it.siguiente();
        assertFalse(it.haFinalizado());
        DetalleRemito actual2 = (DetalleRemito) it.elementoActual();
        assertEquals(doc2, actual2.getDocumentacion());
        assertArrayEquals(new String[]{ "Estudio Traumatológico", "Historia Clínica" }, actual2.obtenerAsuntoYTipoDoc());

        it.siguiente();
        assertTrue(it.haFinalizado());
        assertNull(it.elementoActual());
    }

    // =========================================================================
    // 4. Pruebas de Integración en Memoria (sin Hibernate) con Gestor y Entidades
    // =========================================================================

    @Test
    @DisplayName("Integración: GestorRecepcionBolsin crea IteradorBolsin y filtra sin look-ahead")
    void testIntegracionGestorIteradorBolsin() {
        ComisionMedica cmCentral = new ComisionMedica(1, "Comisión Médica Central (CMC) - CABA",
                "Av. Roque Sáenz Peña 1100", "despacho.cmc@srt.gob.ar", "011-4321-1000");
        ComisionMedica cmCordoba = new ComisionMedica(10, "CMJ N° 1 - Córdoba Capital",
                "Av. Colón 450", "despacho.cordoba@srt.gob.ar", "0351-420-1100");
        ComisionMedica cmRosario = new ComisionMedica(2, "CMJ N° 7 - Rosario",
                "Bv. Oroño 850", "despacho.rosario@srt.gob.ar", "0341-440-2000");

        Empleado emp = new Empleado("Juan Pérez", cmCentral);
        Usuario user = new Usuario("jperez", "Encargado", emp);
        LocalDateTime ahora = LocalDateTime.now();
        Sesion sesion = new Sesion(user, ahora);

        Estado estEnviado = new Estado(Estado.NOMBRE_ENVIADO, "Enviado", Estado.AMBITO_BOLSIN);
        Estado estRecibido = new Estado(Estado.NOMBRE_RECIBIDO_CM_DESTINO, "Recibido", Estado.AMBITO_BOLSIN);

        Bolsin bValido = new Bolsin(1, 7041, ahora, 2.5, cmCordoba, cmCentral, List.of(), estEnviado, ahora, emp);
        Bolsin bYaRecibido = new Bolsin(2, 7042, ahora, 2.0, cmCordoba, cmCentral, List.of(), estRecibido, ahora, emp);
        Bolsin bOtraCM = new Bolsin(3, 7043, ahora, 1.5, cmCordoba, cmRosario, List.of(), estEnviado, ahora, emp);

        IRepositorioCU28 repoEnMemoria = new IRepositorioCU28() {
            @Override public boolean estaVacia() { return false; }
            @Override public Sesion obtenerSesionActiva() { return sesion; }
            @Override public List<Estado> obtenerEstados() { return List.of(estEnviado, estRecibido); }
            @Override public List<ComisionMedica> obtenerComisionesMedicas() { return List.of(cmCentral, cmCordoba, cmRosario); }
            @Override public List<Bolsin> obtenerTodosBolsines() { return List.of(bValido, bYaRecibido, bOtraCM); }
            @Override public Bolsin actualizarBolsin(Bolsin b, Empleado e) { return b; }
        };

        GestorRecepcionBolsin gestor = new GestorRecepcionBolsin(sesion, repoEnMemoria);
        gestor.buscarUsuario();

        IIterador it = gestor.crearIterador(new Bolsin[0]);
        assertNotNull(it);
        assertInstanceOf(IteradorBolsin.class, it);

        gestor.buscarBolsinEstadoEnviado();
        assertEquals(1, gestor.getPrecintosBolsinesEnviados().length);
        assertEquals(7041, gestor.getPrecintosBolsinesEnviados()[0]);
    }

    @Test
    @DisplayName("Integración: Bolsin crea IteradorRemito y retorna String[][] multi-remito completo")
    void testIntegracionBolsinYRemitoIteradores() {
        ComisionMedica cmOrigen = new ComisionMedica(10, "CMJ N° 1 - Córdoba", "", "cba@srt.gob.ar", "");
        ComisionMedica cmDestino = new ComisionMedica(1, "Comisión Médica Central (CMC) - CABA", "", "cmc@srt.gob.ar", "");
        Estado estEnviado = new Estado(Estado.NOMBRE_ENVIADO, "Enviado", Estado.AMBITO_BOLSIN);
        Estado estDoc = new Estado(Estado.NOMBRE_EN_BOLSIN_ENVIADO, "En bolsín", Estado.AMBITO_DOCUMENTACION);
        TipoDocumento td1 = new TipoDocumento("Expediente", "Exp");
        TipoDocumento td2 = new TipoDocumento("Dictamen", "Dic");
        LocalDateTime ahora = LocalDateTime.now();

        Documentacion d1 = new Documentacion(1, "Asunto 1", ahora, td1, cmOrigen, estDoc, ahora, null);
        Documentacion d2 = new Documentacion(2, "Asunto 2", ahora, td2, cmOrigen, estDoc, ahora, null);

        Remito rem1 = new Remito(1001, ahora, cmOrigen, cmDestino);
        rem1.agregarDetalle(new DetalleRemito(d1, "Mesa de Entradas"));
        Remito rem2 = new Remito(1002, ahora, cmOrigen, cmDestino);
        rem2.agregarDetalle(new DetalleRemito(d2, "Junta Médica"));

        Bolsin bolsin = new Bolsin(50, 7041, ahora, 3.2, cmOrigen, cmDestino,
                List.of(rem1, rem2), estEnviado, ahora, null);

        assertEquals("CMJ N° 1 - Córdoba", bolsin.getNombreCMOrigen());
        assertEquals(7041, bolsin.getNroPrecinto());
        assertTrue(bolsin.sosEnviado());
        assertTrue(bolsin.esTuCMDestino("Comisión Médica Central (CMC) - CABA"));

        IIterador it = bolsin.crearIterador(new Remito[]{ rem1, rem2 });
        assertInstanceOf(IteradorRemito.class, it);

        String[][] info = bolsin.obtenerInformacionRemito();
        assertEquals(2, info.length);
        assertArrayEquals(new String[]{ "1001", "Asunto 1", "Expediente" }, info[0]);
        assertArrayEquals(new String[]{ "1002", "Asunto 2", "Dictamen" }, info[1]);
    }

    @Test
    @DisplayName("Integración: Remito crea IteradorDetalleRemito y busca documentación retornando String[][]")
    void testIntegracionRemitoYDetalleIteradores() {
        ComisionMedica cmOrig = new ComisionMedica(10, "CMJ N° 1 - Córdoba", "", "", "");
        ComisionMedica cmDest = new ComisionMedica(1, "CMC", "", "", "");
        LocalDateTime ahora = LocalDateTime.now();
        Remito remito = new Remito(9999, ahora, cmOrig, cmDest);

        TipoDocumento td = new TipoDocumento("Peritaje Contable", "Peritaje");
        Estado est = new Estado(Estado.NOMBRE_EN_BOLSIN_ENVIADO, "En bolsín", Estado.AMBITO_DOCUMENTACION);
        Documentacion doc = new Documentacion(99, "Peritaje de Prueba", ahora, td, cmOrig, est, ahora, null);
        DetalleRemito detalle = new DetalleRemito(doc, "Contable");
        remito.agregarDetalle(detalle);

        IIterador it = remito.crearIterador(new DetalleRemito[]{ detalle });
        assertInstanceOf(IteradorDetalleRemito.class, it);

        String[][] docs = remito.buscarDocumentacion();
        assertEquals(1, docs.length);
        assertArrayEquals(new String[]{ "Peritaje de Prueba", "Peritaje Contable" }, docs[0]);
    }
}
