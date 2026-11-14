package com.ppai;

import com.ppai.controller.cu28.GestorRecepcionBolsin;
import com.ppai.domain.model.cu28.*;
import com.ppai.iterator.*;
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
 * 1. Restricción estricta de NO utilización de java.util.Iterator ni java.util.Iterable.
 * 2. Realización de las interfaces IAgregado e IIterador por parte de las clases del modelo y control.
 * 3. Comportamiento unitario de IteradorBolsin, IteradorRemito e IteradorDetalleRemito.
 * 4. Integración fluida entre GestorRecepcionBolsin, Bolsin, Remito y sus respectivos iteradores.
 */
public class IteratorPatternTest {

    // =========================================================================
    // 1. Pruebas de Reflexión y Restricción Estructural (Zero JDK Iterator)
    // =========================================================================

    @Test
    @DisplayName("R1 - Reflexión: Ninguna interfaz ni clase del patrón debe extender o implementar java.util.Iterator")
    void testNoJdkIteratorInterfacesUsed() {
        assertFalse(java.util.Iterator.class.isAssignableFrom(IIterador.class),
                "IIterador NO debe extender de java.util.Iterator");
        assertFalse(java.util.Iterator.class.isAssignableFrom(IteradorBolsin.class),
                "IteradorBolsin NO debe implementar java.util.Iterator");
        assertFalse(java.util.Iterator.class.isAssignableFrom(IteradorRemito.class),
                "IteradorRemito NO debe implementar java.util.Iterator");
        assertFalse(java.util.Iterator.class.isAssignableFrom(IteradorDetalleRemito.class),
                "IteradorDetalleRemito NO debe implementar java.util.Iterator");
    }

    @Test
    @DisplayName("R1 - Reflexión: Ningún agregado debe extender o implementar java.lang.Iterable")
    void testNoJdkIterableInterfacesUsed() {
        assertFalse(Iterable.class.isAssignableFrom(IAgregado.class),
                "IAgregado NO debe extender de Iterable");
        assertFalse(Iterable.class.isAssignableFrom(GestorRecepcionBolsin.class),
                "GestorRecepcionBolsin NO debe implementar Iterable");
        assertFalse(Iterable.class.isAssignableFrom(Bolsin.class),
                "Bolsin NO debe implementar Iterable");
        assertFalse(Iterable.class.isAssignableFrom(Remito.class),
                "Remito NO debe implementar Iterable");
    }

    @Test
    @DisplayName("R1 - Estático: Verificación de código fuente sin importaciones de java.util.Iterator ni java.lang.Iterable")
    void testNoJdkIteratorImportsInSource() throws IOException {
        Path iteratorPkg = Paths.get("src", "main", "java", "com", "ppai", "iterator");
        assertTrue(Files.exists(iteratorPkg), "El paquete com.ppai.iterator debe existir físicamente");

        try (Stream<Path> paths = Files.walk(iteratorPkg)) {
            List<Path> javaFiles = paths.filter(p -> p.toString().endsWith(".java")).toList();
            assertFalse(javaFiles.isEmpty(), "Deben existir archivos Java en com.ppai.iterator");

            for (Path javaFile : javaFiles) {
                String content = Files.readString(javaFile);
                assertFalse(content.contains("import java.util.Iterator;"),
                        "El archivo " + javaFile.getFileName() + " no debe importar java.util.Iterator");
                assertFalse(content.contains("import java.util.Iterable;"),
                        "El archivo " + javaFile.getFileName() + " no debe importar java.util.Iterable");
                assertFalse(content.contains("import java.lang.Iterable;"),
                        "El archivo " + javaFile.getFileName() + " no debe importar java.lang.Iterable");
            }
        }
    }

    @Test
    @DisplayName("R1/R2 - Reflexión: GestorRecepcionBolsin, Bolsin y Remito deben implementar IAgregado")
    void testAgregadosImplementIAgregado() {
        assertTrue(IAgregado.class.isAssignableFrom(GestorRecepcionBolsin.class),
                "GestorRecepcionBolsin debe implementar IAgregado");
        assertTrue(IAgregado.class.isAssignableFrom(Bolsin.class),
                "Bolsin debe implementar IAgregado");
        assertTrue(IAgregado.class.isAssignableFrom(Remito.class),
                "Remito debe implementar IAgregado");
    }

    @Test
    @DisplayName("R1 - Reflexión: IteradorBolsin, IteradorRemito e IteradorDetalleRemito deben implementar IIterador")
    void testIteradoresImplementIIterador() {
        assertTrue(IIterador.class.isAssignableFrom(IteradorBolsin.class),
                "IteradorBolsin debe implementar IIterador");
        assertTrue(IIterador.class.isAssignableFrom(IteradorRemito.class),
                "IteradorRemito debe implementar IIterador");
        assertTrue(IIterador.class.isAssignableFrom(IteradorDetalleRemito.class),
                "IteradorDetalleRemito debe implementar IIterador");
    }

    // =========================================================================
    // 2. Pruebas Unitarias de IteradorBolsin
    // =========================================================================

    @Test
    @DisplayName("IteradorBolsin: Comportamiento con colección vacía")
    void testIteradorBolsinColeccionVacia() {
        ComisionMedica cmCentral = new ComisionMedica(1L, "Comisión Médica Central (CMC) - CABA");
        Object[] filtros = new Object[]{ cmCentral };

        IteradorBolsin it = new IteradorBolsin(new Bolsin[0], filtros);
        assertTrue(it.haFinalizado(), "Debe finalizar inmediatamente si la colección está vacía");
        assertNull(it.actual(), "actual() debe retornar null para colección vacía");
        assertNull(it.elementoActual(), "elementoActual() debe retornar null");

        it.primero();
        assertTrue(it.haFinalizado(), "Sigue finalizado tras invocar primero()");
        it.siguiente();
        assertTrue(it.haFinalizado(), "Sigue finalizado tras invocar siguiente()");
    }

    @Test
    @DisplayName("IteradorBolsin: Filtrado correcto por estado ENVIADO y CM Destino")
    void testIteradorBolsinFiltradoCorrecto() {
        ComisionMedica cmCentral = new ComisionMedica(1L, "Comisión Médica Central (CMC) - CABA");
        ComisionMedica cmCordoba = new ComisionMedica(2L, "CMJ N° 1 - Córdoba");
        ComisionMedica cmRosario = new ComisionMedica(3L, "CMJ N° 7 - Rosario");

        Estado estEnviado = new Estado(Estado.NOMBRE_ENVIADO, Estado.AMBITO_BOLSIN);
        Estado estRecibido = new Estado(Estado.NOMBRE_RECIBIDO_CM_DESTINO, Estado.AMBITO_BOLSIN);

        LocalDateTime ahora = LocalDateTime.now();

        // 1. Bolsin que cumple: ENVIADO hacia cmCentral
        Bolsin bol1 = new Bolsin("PREC-1111", cmCordoba, cmCentral, null, estEnviado, ahora);

        // 2. Bolsin que NO cumple: RECIBIDO hacia cmCentral
        Bolsin bol2 = new Bolsin("PREC-2222", cmCordoba, cmCentral, null, estRecibido, ahora);

        // 3. Bolsin que NO cumple: ENVIADO hacia cmRosario (no es cmCentral)
        Bolsin bol3 = new Bolsin("PREC-3333", cmCordoba, cmRosario, null, estEnviado, ahora);

        // 4. Bolsin que cumple: ENVIADO hacia cmCentral
        Bolsin bol4 = new Bolsin("PREC-4444", cmRosario, cmCentral, null, estEnviado, ahora);

        Bolsin[] array = new Bolsin[]{ bol1, bol2, bol3, bol4 };
        Object[] filtros = new Object[]{ cmCentral };

        IteradorBolsin it = new IteradorBolsin(array, filtros);
        it.primero();

        assertFalse(it.haFinalizado(), "El iterador no debe haber finalizado");
        assertEquals("PREC-1111", ((Bolsin) it.actual()).getNumeroPrecinto(), "Primer elemento filtrado debe ser PREC-1111");

        it.siguiente();
        assertFalse(it.haFinalizado(), "Debe existir un segundo elemento que cumple el filtro");
        assertEquals("PREC-4444", ((Bolsin) it.actual()).getNumeroPrecinto(), "Segundo elemento filtrado debe ser PREC-4444");

        it.siguiente();
        assertTrue(it.haFinalizado(), "Ya no deben quedar elementos válidos");
        assertNull(it.actual(), "actual() debe retornar null al finalizar");
    }

    @Test
    @DisplayName("IteradorBolsin: Reinicio de puntero con primero()")
    void testIteradorBolsinReinicioPrimero() {
        ComisionMedica cmCentral = new ComisionMedica(1L, "Comisión Médica Central (CMC) - CABA");
        ComisionMedica cmOrigen = new ComisionMedica(2L, "CMJ N° 1 - Córdoba");
        Estado estEnviado = new Estado(Estado.NOMBRE_ENVIADO, Estado.AMBITO_BOLSIN);

        Bolsin bol1 = new Bolsin("PREC-100", cmOrigen, cmCentral, null, estEnviado, LocalDateTime.now());
        Bolsin bol2 = new Bolsin("PREC-200", cmOrigen, cmCentral, null, estEnviado, LocalDateTime.now());

        IteradorBolsin it = new IteradorBolsin(new Bolsin[]{ bol1, bol2 }, new Object[]{ cmCentral });

        it.primero();
        assertEquals("PREC-100", ((Bolsin) it.actual()).getNumeroPrecinto());
        it.siguiente();
        assertEquals("PREC-200", ((Bolsin) it.actual()).getNumeroPrecinto());
        it.siguiente();
        assertTrue(it.haFinalizado());

        // Reinicio con primero()
        it.primero();
        assertFalse(it.haFinalizado());
        assertEquals("PREC-100", ((Bolsin) it.actual()).getNumeroPrecinto(), "Debe volver al primer elemento válido");
    }

    // =========================================================================
    // 3. Pruebas Unitarias de IteradorRemito
    // =========================================================================

    @Test
    @DisplayName("IteradorRemito: Recorrido secuencial completo")
    void testIteradorRemitoRecorrido() {
        Remito r1 = new Remito("REM-001");
        Remito r2 = new Remito("REM-002");
        Remito r3 = new Remito("REM-003");

        IteradorRemito it = new IteradorRemito(new Remito[]{ r1, r2, r3 });
        it.primero();

        assertFalse(it.haFinalizado());
        assertEquals("REM-001", ((Remito) it.actual()).getNumero());

        it.siguiente();
        assertFalse(it.haFinalizado());
        assertEquals("REM-002", ((Remito) it.actual()).getNumero());

        it.siguiente();
        assertFalse(it.haFinalizado());
        assertEquals("REM-003", ((Remito) it.actual()).getNumero());

        it.siguiente();
        assertTrue(it.haFinalizado());
        assertNull(it.actual());

        // Reinicio
        it.primero();
        assertFalse(it.haFinalizado());
        assertEquals("REM-001", ((Remito) it.actual()).getNumero());
    }

    @Test
    @DisplayName("IteradorRemito: Colección vacía")
    void testIteradorRemitoColeccionVacia() {
        IteradorRemito it = new IteradorRemito(new Remito[0]);
        assertTrue(it.haFinalizado());
        assertNull(it.actual());
    }

    // =========================================================================
    // 4. Pruebas Unitarias de IteradorDetalleRemito
    // =========================================================================

    @Test
    @DisplayName("IteradorDetalleRemito: Recorrido y obtención de asunto y tipo de documento")
    void testIteradorDetalleRemitoRecorrido() {
        TipoDocumento td1 = new TipoDocumento(1L, "Dictamen Médico");
        TipoDocumento td2 = new TipoDocumento(2L, "Historia Clínica");

        Estado estEnviado = new Estado(Estado.NOMBRE_EN_BOLSIN_ENVIADO, Estado.AMBITO_DOCUMENTACION);
        LocalDateTime ahora = LocalDateTime.now();

        Documentacion doc1 = new Documentacion(10L, "Expediente N° 5541/2026", td1, estEnviado, ahora);
        Documentacion doc2 = new Documentacion(20L, "Estudio Traumatológico", td2, estEnviado, ahora);

        DetalleRemito det1 = new DetalleRemito(doc1);
        DetalleRemito det2 = new DetalleRemito(doc2);

        IteradorDetalleRemito it = new IteradorDetalleRemito(new DetalleRemito[]{ det1, det2 });
        it.primero();

        assertFalse(it.haFinalizado());
        DetalleRemito actual1 = (DetalleRemito) it.actual();
        assertEquals(doc1, actual1.getDocumentacion());
        String[] info1 = actual1.obtenerAsuntoYTipoDoc();
        assertEquals("Expediente N° 5541/2026", info1[0]);
        assertEquals("Dictamen Médico", info1[1]);

        it.siguiente();
        assertFalse(it.haFinalizado());
        DetalleRemito actual2 = (DetalleRemito) it.actual();
        assertEquals(doc2, actual2.getDocumentacion());
        String[] info2 = actual2.obtenerAsuntoYTipoDoc();
        assertEquals("Estudio Traumatológico", info2[0]);
        assertEquals("Historia Clínica", info2[1]);

        it.siguiente();
        assertTrue(it.haFinalizado());
        assertNull(it.actual());
    }

    // =========================================================================
    // 5. Pruebas de Integración con Entidades y Gestor del CU 28
    // =========================================================================

    @Test
    @DisplayName("Integración: GestorRecepcionBolsin crea y utiliza IteradorBolsin")
    void testIntegracionGestorIteradorBolsin() {
        ComisionMedica cmCentral = new ComisionMedica(1L, "Comisión Médica Central (CMC) - CABA");
        Empleado emp = new Empleado(100L, "Juan Pérez", cmCentral);
        Usuario user = new Usuario("jperez", emp);
        Sesion sesion = new Sesion(user);

        GestorRecepcionBolsin gestor = new GestorRecepcionBolsin(sesion);

        // Crear iterador vía interfaz IAgregado
        IIterador it = gestor.crearIterador(new Bolsin[0]);
        assertNotNull(it, "crearIterador() debe retornar una instancia válida");
        assertInstanceOf(IteradorBolsin.class, it, "Debe ser de tipo IteradorBolsin");

        // Ejecutar búsqueda y verificar filtrado en gestor
        gestor.buscarUsuario();
        gestor.buscarBolsinEstadoEnviado();
        assertNotNull(gestor.getBolsinesEnviados(), "La lista de bolsines enviados debe estar inicializada");
        assertFalse(gestor.getBolsinesEnviados().isEmpty(), "Debe encontrar los bolsines mock en estado ENVIADO hacia CMC");
    }

    @Test
    @DisplayName("Integración: Bolsin crea IteradorRemito y obtiene información de remitos")
    void testIntegracionBolsinYRemitoIteradores() {
        ComisionMedica cmOrigen = new ComisionMedica(2L, "CMJ N° 1 - Córdoba");
        ComisionMedica cmDestino = new ComisionMedica(1L, "Comisión Médica Central (CMC) - CABA");
        Estado estEnviado = new Estado(Estado.NOMBRE_ENVIADO, Estado.AMBITO_BOLSIN);

        Remito remito = new Remito("REM-7041-01");
        Bolsin bolsin = new Bolsin("PREC-7041", cmOrigen, cmDestino, remito, estEnviado, LocalDateTime.now());

        // Verificación de métodos nuevos de diseño
        assertEquals("CMJ N° 1 - Córdoba", bolsin.getNombreCMOrigen());
        assertEquals(7041, bolsin.getNroPrecinto());
        assertTrue(bolsin.sosEnviado());
        assertTrue(bolsin.esTuCMDestino("Comisión Médica Central (CMC) - CABA"));

        // Verificación de fábrica del iterador
        IIterador it = bolsin.crearIterador(new Remito[]{ remito });
        assertInstanceOf(IteradorRemito.class, it);

        // Recorrido vía obtenerInformacionRemito()
        Remito obtenido = bolsin.obtenerInformacionRemito();
        assertNotNull(obtenido);
        assertEquals("REM-7041-01", obtenido.getNumero());
    }

    @Test
    @DisplayName("Integración: Remito crea IteradorDetalleRemito y busca documentación")
    void testIntegracionRemitoYDetalleIteradores() {
        Remito remito = new Remito("REM-9999");
        TipoDocumento td = new TipoDocumento(3L, "Peritaje Contable");
        Estado est = new Estado(Estado.NOMBRE_EN_BOLSIN_ENVIADO, Estado.AMBITO_DOCUMENTACION);
        Documentacion doc = new Documentacion(99L, "Peritaje de Prueba", td, est, LocalDateTime.now());
        DetalleRemito detalle = new DetalleRemito(doc);

        remito.agregarDetalle(detalle);

        // Verificación de fábrica del iterador
        IIterador it = remito.crearIterador(new DetalleRemito[]{ detalle });
        assertInstanceOf(IteradorDetalleRemito.class, it);

        // Búsqueda de documentación usando el iterador
        List<DetalleRemito> detalles = remito.buscarDocumentacion();
        assertEquals(1, detalles.size());
        assertEquals("Peritaje de Prueba", detalles.get(0).getDocumentacion().getAsunto());
        String[] info = detalles.get(0).obtenerAsuntoYTipoDoc();
        assertEquals("Peritaje de Prueba", info[0]);
        assertEquals("Peritaje Contable", info[1]);
    }
}
