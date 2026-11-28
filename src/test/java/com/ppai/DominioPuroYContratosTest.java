package com.ppai;

import com.ppai.controller.cu28.GestorRecepcionBolsin;
import com.ppai.domain.model.cu28.*;
import com.ppai.iterator.IIterador;
import com.ppai.iterator.IteradorBolsin;
import com.ppai.iterator.IteradorDetalleRemito;
import com.ppai.iterator.IteradorRemito;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gate de Verificación Íntegra de Fase 1:
 * - Pureza OO de las 12 clases del dominio (0 JPA, 0 Hibernate, 0 JavaFX, 0 campos 'id', 0 ctors protected).
 * - Contratos estrictos por reflexión del patrón Iterator y agregados.
 * - Comportamiento limpio del dominio y soporte multi-remito (Opción 1, A3, A4, A5).
 */
public class DominioPuroYContratosTest {

    private static final List<Class<?>> DOCE_CLASES_DOMINIO = List.of(
            Bolsin.class,
            CambioEstadoBolsin.class,
            CambioEstadoDocumentacion.class,
            ComisionMedica.class,
            DetalleRemito.class,
            Documentacion.class,
            Empleado.class,
            Estado.class,
            Remito.class,
            Sesion.class,
            TipoDocumento.class,
            Usuario.class
    );

    // =========================================================================
    // 1. Pureza OO del Dominio (Cero JPA, Hibernate, JavaFX y Cero Long id)
    // =========================================================================

    @Test
    @DisplayName("Pureza Dominio (Estático): exactamente 12 clases E1+E2 y 0 imports de infraestructura")
    void testDominioSinImportsInfraestructura() throws IOException {
        Path domainDir = Paths.get("src", "main", "java", "com", "ppai", "domain", "model", "cu28");
        assertTrue(Files.exists(domainDir));

        List<String> archivosEnDominio;
        try (var stream = Files.list(domainDir)) {
            archivosEnDominio = stream.filter(p -> p.toString().endsWith(".java"))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        }
        assertEquals(12, archivosEnDominio.size(),
                "domain.model.cu28 debe contener exactamente las 12 clases del Diagrama de Clases");

        for (Class<?> clazz : DOCE_CLASES_DOMINIO) {
            Path srcFile = domainDir.resolve(clazz.getSimpleName() + ".java");
            assertTrue(Files.exists(srcFile), "Debe existir " + srcFile);
            String source = Files.readString(srcFile);

            assertFalse(source.contains("jakarta.persistence"),
                    clazz.getSimpleName() + " no debe importar ni referenciar jakarta.persistence");
            assertFalse(source.contains("org.hibernate"),
                    clazz.getSimpleName() + " no debe importar org.hibernate");
            assertFalse(source.contains("javafx."),
                    clazz.getSimpleName() + " no debe importar javafx");
        }
    }

    @Test
    @DisplayName("Pureza Dominio (Reflexión): 0 anotaciones JPA, 0 campos 'id' y 0 ctors protected sin args")
    void testDominioSinAnotacionesCamposIdNiCtorsProtected() {
        for (Class<?> clazz : DOCE_CLASES_DOMINIO) {
            for (Annotation a : clazz.getAnnotations()) {
                assertFalse(a.annotationType().getName().startsWith("jakarta.persistence"),
                        clazz.getSimpleName() + " tiene anotación JPA en clase: " + a);
            }

            for (Field field : clazz.getDeclaredFields()) {
                assertNotEquals("id", field.getName(),
                        clazz.getSimpleName() + " no debe tener campo 'id' artificial de BD");
                for (Annotation a : field.getAnnotations()) {
                    assertFalse(a.annotationType().getName().startsWith("jakarta.persistence"),
                            clazz.getSimpleName() + "." + field.getName() + " tiene anotación JPA: " + a);
                }
            }

            for (Method method : clazz.getDeclaredMethods()) {
                for (Annotation a : method.getAnnotations()) {
                    assertFalse(a.annotationType().getName().startsWith("jakarta.persistence"),
                            clazz.getSimpleName() + "." + method.getName() + " tiene anotación JPA");
                }
            }

            for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
                boolean esProtectedSinArgs = Modifier.isProtected(ctor.getModifiers()) && ctor.getParameterCount() == 0;
                assertFalse(esProtectedSinArgs,
                        clazz.getSimpleName() + " no debe tener constructor protected sin parámetros para Hibernate");
            }
        }
    }

    // =========================================================================
    // 2. Contratos Estrictos de Patrón Iterator y Agregados
    // =========================================================================

    @Test
    @DisplayName("Contratos Iterator (Reflexión + Estático): firmas exactas sin actual(), getters ni sobrecargas")
    void testContratosEstrictosIteratorYAgregados() throws IOException {
        // IIterador: exactamente 4 métodos, sin actual()
        Set<String> metodosIIterador = Arrays.stream(IIterador.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("primero", "siguiente", "haFinalizado", "elementoActual"), metodosIIterador);

        // IteradorBolsin: 1 ctor (Object[], Object[]), 0 imports de ComisionMedica, 5 métodos públicos exactos
        String iteradorBolsinSrc = Files.readString(
                Paths.get("src", "main", "java", "com", "ppai", "iterator", "IteradorBolsin.java"));
        assertFalse(iteradorBolsinSrc.contains("ComisionMedica"),
                "IteradorBolsin no debe depender de ComisionMedica");

        assertEquals(1, IteradorBolsin.class.getDeclaredConstructors().length);
        assertArrayEquals(new Class<?>[]{ Object[].class, Object[].class },
                IteradorBolsin.class.getDeclaredConstructors()[0].getParameterTypes());

        Set<String> pubIterBolsin = Arrays.stream(IteradorBolsin.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("primero", "siguiente", "haFinalizado", "elementoActual", "comprobarFiltro"), pubIterBolsin);

        long comprobadores = Arrays.stream(IteradorBolsin.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("comprobarFiltro"))
                .count();
        assertEquals(1L, comprobadores, "Solo debe existir 1 comprobarFiltro(Object[])");

        // IteradorRemito e IteradorDetalleRemito: 1 ctor (Object[]), exactos 4 métodos públicos
        for (Class<?> iterClass : List.of(IteradorRemito.class, IteradorDetalleRemito.class)) {
            assertEquals(1, iterClass.getDeclaredConstructors().length);
            assertArrayEquals(new Class<?>[]{ Object[].class },
                    iterClass.getDeclaredConstructors()[0].getParameterTypes());

            Set<String> pubMethods = Arrays.stream(iterClass.getDeclaredMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers()))
                    .map(Method::getName)
                    .collect(Collectors.toSet());
            assertEquals(Set.of("primero", "siguiente", "haFinalizado", "elementoActual"), pubMethods);
        }

        // Los 3 agregados: exactamente 1 crearIterador(Object[])
        for (Class<?> agrClass : List.of(GestorRecepcionBolsin.class, Bolsin.class, Remito.class)) {
            List<Method> creadores = Arrays.stream(agrClass.getDeclaredMethods())
                    .filter(m -> m.getName().equals("crearIterador"))
                    .toList();
            assertEquals(1, creadores.size(), agrClass.getSimpleName() + " debe declarar exactamente 1 crearIterador");
            assertArrayEquals(new Class<?>[]{ Object[].class }, creadores.get(0).getParameterTypes());
        }
    }

    // =========================================================================
    // 3. Contratos Limpios del Dominio y Reglas Multi-Remito
    // =========================================================================

    @Test
    @DisplayName("Contratos Dominio: int limpios, sin getters/sobrecargas duplicadas e identidad natural")
    void testContratosLimpiosDominio() throws NoSuchMethodException {
        assertEquals(int.class, Bolsin.class.getDeclaredMethod("getNroPrecinto").getReturnType());
        assertEquals(int.class, Remito.class.getDeclaredMethod("getNumero").getReturnType());
        assertEquals(String[][].class, Bolsin.class.getDeclaredMethod("obtenerInformacionRemito").getReturnType());
        assertEquals(String[][].class, Remito.class.getDeclaredMethod("buscarDocumentacion").getReturnType());

        Set<String> metodosBolsin = Arrays.stream(Bolsin.class.getDeclaredMethods())
                .map(Method::getName).collect(Collectors.toSet());
        assertFalse(metodosBolsin.contains("getNumeroPrecinto"));
        assertFalse(metodosBolsin.contains("getRemito"));
        assertEquals(1L, Arrays.stream(Bolsin.class.getDeclaredMethods()).filter(m -> m.getName().equals("marcarRecibido")).count());
        assertEquals(1L, Arrays.stream(Bolsin.class.getDeclaredMethods()).filter(m -> m.getName().equals("crearNuevoCambioDeEstado")).count());

        Set<String> metodosRemito = Arrays.stream(Remito.class.getDeclaredMethods())
                .map(Method::getName).collect(Collectors.toSet());
        assertFalse(metodosRemito.contains("marcarRecibidoYAceptado"));
        assertFalse(metodosRemito.contains("marcarRecibidoYAceptadoParcial"));

        Set<String> metodosDetalle = Arrays.stream(DetalleRemito.class.getDeclaredMethods())
                .map(Method::getName).collect(Collectors.toSet());
        assertFalse(metodosDetalle.contains("getDocumentacionAsociada"));

        // Identidad natural de Bolsin por numeroBolsin (precinto cambia al reabrir)
        ComisionMedica cm1 = new ComisionMedica(1, "Comisión Médica Central (CMC) - CABA",
                "Av. Roque Sáenz Peña 1100", "despacho.cmc@srt.gob.ar", "011-4321-1000");
        ComisionMedica cm2 = new ComisionMedica(10, "CMJ N° 1 - Córdoba Capital",
                "Av. Colón 450", "despacho.cordoba@srt.gob.ar", "0351-420-1100");
        Estado estEnv = new Estado(Estado.NOMBRE_ENVIADO, "Enviado", Estado.AMBITO_BOLSIN);
        LocalDateTime ahora = LocalDateTime.now();

        Bolsin bA = new Bolsin(10, 7001, ahora, 2.0, cm2, cm1, List.of(), estEnv, ahora, null);
        Bolsin bBReabierto = new Bolsin(10, 7099, ahora, 2.0, cm2, cm1, List.of(), estEnv, ahora, null);
        Bolsin bDistinto = new Bolsin(11, 7001, ahora, 2.0, cm2, cm1, List.of(), estEnv, ahora, null);
        assertEquals(bA, bBReabierto, "Dos instancias del mismo numeroBolsin representan la misma identidad");
        assertNotEquals(bA, bDistinto);

        // ComisionMedica: coincidencia exacta, esCentral() y email propio
        assertTrue(cm1.esTuCMDestino("comisión médica central (cmc) - caba"));
        assertFalse(cm1.esTuCMDestino("Central"), "No debe aceptar substrings parciales");
        assertFalse(cm1.esTuCMDestino("CABA"));
        assertTrue(cm1.esCentral());
        assertFalse(cm2.esCentral());
        assertEquals("despacho.cmc@srt.gob.ar", cm1.getEmail());

        // Sesion: vigencia y cadena Bloque 1
        Empleado emp = new Empleado("Juan Pérez", cm1);
        Usuario usr = new Usuario("jperez", "Encargado", emp);
        Sesion sesion = new Sesion(usr, ahora);
        assertTrue(sesion.estaActiva());
        assertEquals(cm1, sesion.getCMEmpleado());
        assertEquals(emp, sesion.getEmpleadoLogueado());
        sesion.setFechaHoraFin(ahora.plusHours(2));
        assertFalse(sesion.estaActiva());
    }

    @Test
    @DisplayName("Dominio Multi-Remito: obtenerInformacionRemito() y opciones 1, A3, A4 y A5 sobre 2 remitos x 2 docs")
    void testDominioMultiRemitoCompleto() {
        ComisionMedica cmOrig = new ComisionMedica(10, "CMJ N° 1 - Córdoba Capital", "", "cba@srt.gob.ar", "");
        ComisionMedica cmDest = new ComisionMedica(1, "Comisión Médica Central (CMC) - CABA", "", "cmc@srt.gob.ar", "");
        Empleado emp = new Empleado("Juan Pérez", cmDest);
        LocalDateTime t0 = LocalDateTime.now().minusDays(2);
        LocalDateTime tRec = LocalDateTime.now();

        Estado estBolEnv = new Estado(Estado.NOMBRE_ENVIADO, "Enviado", Estado.AMBITO_BOLSIN);
        Estado estBolRec = new Estado(Estado.NOMBRE_RECIBIDO_CM_DESTINO, "Recibido en CM Destino", Estado.AMBITO_BOLSIN);
        Estado estRemAcep = new Estado(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, "Recibido y aceptado", Estado.AMBITO_REMITO);
        Estado estRemParc = new Estado(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL, "Recibido parcial", Estado.AMBITO_REMITO);
        Estado estDocEnv = new Estado(Estado.NOMBRE_EN_BOLSIN_ENVIADO, "En bolsín enviado", Estado.AMBITO_DOCUMENTACION);
        Estado estDocAcep = new Estado(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, "Recibida y aceptada", Estado.AMBITO_DOCUMENTACION);
        Estado estDocNoRec = new Estado(Estado.NOMBRE_NO_RECIBIDA, "No recibida", Estado.AMBITO_DOCUMENTACION);
        Estado estDocRech = new Estado(Estado.NOMBRE_RECIBIDA_Y_RECHAZADA, "Recibida y rechazada", Estado.AMBITO_DOCUMENTACION);
        Estado estDocRedir = new Estado(Estado.NOMBRE_PARA_REDIRIGIR, "Para redirigir", Estado.AMBITO_DOCUMENTACION);

        TipoDocumento tdExp = new TipoDocumento("Expediente", "Exp");
        TipoDocumento tdDic = new TipoDocumento("Dictamen", "Dic");

        // --- Escenario A3: falta doc 101 en Remito 1, Remito 2 viene completo ---
        Bolsin bA3 = crearBolsinConDosRemitos(5001, 8001, cmOrig, cmDest, estBolEnv, estDocEnv, tdExp, tdDic, t0, emp);
        String[][] infoA3 = bA3.obtenerInformacionRemito();
        assertEquals(4, infoA3.length);
        assertEquals("501", infoA3[0][0]);
        assertEquals("501", infoA3[1][0]);
        assertEquals("502", infoA3[2][0]);
        assertEquals("502", infoA3[3][0]);

        bA3.marcarRecibido(tRec, estBolRec, emp);
        bA3.registrarFaltaDocumentacion(tRec, estRemAcep, estRemParc, estDocAcep, estDocNoRec, Set.of(101), emp);

        assertEquals(Estado.NOMBRE_RECIBIDO_CM_DESTINO, bA3.getEstadoActual().getNombre());
        assertEquals(2, bA3.getCambiosDeEstado().size());
        assertNotNull(bA3.getCambiosDeEstado().get(0).getFechaHoraFin());
        assertEquals(emp, bA3.obtenerCambioActual().getEmpleado());

        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL, bA3.getRemitos().get(0).getEstado().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, bA3.getRemitos().get(1).getEstado().getNombre());
        assertEquals(Estado.NOMBRE_NO_RECIBIDA, bA3.getRemitos().get(0).getDetalles().get(0).getDocumentacion().getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, bA3.getRemitos().get(0).getDetalles().get(1).getDocumentacion().getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, bA3.getRemitos().get(1).getDetalles().get(0).getDocumentacion().getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, bA3.getRemitos().get(1).getDetalles().get(1).getDocumentacion().getEstadoActual().getNombre());

        // --- Escenario A4: error en doc 104 de Remito 2, Remito 1 completo ---
        Bolsin bA4 = crearBolsinConDosRemitos(5002, 8002, cmOrig, cmDest, estBolEnv, estDocEnv, tdExp, tdDic, t0, emp);
        bA4.marcarRecibido(tRec, estBolRec, emp);
        bA4.registrarDestinoErroneo(tRec, estRemAcep, estRemParc, estDocAcep, estDocRech, Set.of(104), emp);

        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, bA4.getRemitos().get(0).getEstado().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL, bA4.getRemitos().get(1).getEstado().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, bA4.getRemitos().get(1).getDetalles().get(0).getDocumentacion().getEstadoActual().getNombre());
        assertEquals(Estado.NOMBRE_RECIBIDA_Y_RECHAZADA, bA4.getRemitos().get(1).getDetalles().get(1).getDocumentacion().getEstadoActual().getNombre());

        // --- Opción 1: ambos remitos y 4 docs aceptados ---
        Bolsin bOp1 = crearBolsinConDosRemitos(5003, 8003, cmOrig, cmDest, estBolEnv, estDocEnv, tdExp, tdDic, t0, emp);
        bOp1.marcarRecibido(tRec, estBolRec, emp);
        bOp1.actualizarRemitoYDocumentacion(tRec, estRemAcep, estDocAcep, emp);
        for (Remito r : bOp1.getRemitos()) {
            assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, r.getEstado().getNombre());
            for (DetalleRemito det : r.getDetalles()) {
                assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, det.getDocumentacion().getEstadoActual().getNombre());
                assertEquals(emp, det.getDocumentacion().obtenerCambioActual().getEmpleado());
                assertNotNull(det.getDocumentacion().getCambiosDeEstado().get(0).getFechaHoraFin());
            }
        }

        // --- Escenario A5: ambos remitos aceptados y 4 docs PARA_REDIRIGIR ---
        Bolsin bA5 = crearBolsinConDosRemitos(5004, 8004, cmOrig, cmDest, estBolEnv, estDocEnv, tdExp, tdDic, t0, emp);
        bA5.marcarRecibido(tRec, estBolRec, emp);
        bA5.marcarParaRedirigir(tRec, estRemAcep, estDocRedir, emp);
        for (Remito r : bA5.getRemitos()) {
            assertEquals(Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, r.getEstado().getNombre());
            for (DetalleRemito det : r.getDetalles()) {
                assertEquals(Estado.NOMBRE_PARA_REDIRIGIR, det.getDocumentacion().getEstadoActual().getNombre());
            }
        }
    }

    private Bolsin crearBolsinConDosRemitos(int nroBolsin, int nroPrecinto,
                                           ComisionMedica cmOrig, ComisionMedica cmDest,
                                           Estado estBolEnv, Estado estDocEnv,
                                           TipoDocumento tdExp, TipoDocumento tdDic,
                                           LocalDateTime t0, Empleado emp) {
        Documentacion d1 = new Documentacion(101, "Doc 101", t0, tdExp, cmOrig, estDocEnv, t0, emp);
        Documentacion d2 = new Documentacion(102, "Doc 102", t0, tdDic, cmOrig, estDocEnv, t0, emp);
        Documentacion d3 = new Documentacion(103, "Doc 103", t0, tdExp, cmOrig, estDocEnv, t0, emp);
        Documentacion d4 = new Documentacion(104, "Doc 104", t0, tdDic, cmOrig, estDocEnv, t0, emp);

        Remito r1 = new Remito(501, t0, cmOrig, cmDest);
        r1.agregarDetalle(new DetalleRemito(d1, "Mesa Entradas"));
        r1.agregarDetalle(new DetalleRemito(d2, "Junta Médica"));

        Remito r2 = new Remito(502, t0, cmOrig, cmDest);
        r2.agregarDetalle(new DetalleRemito(d3, "Legales"));
        r2.agregarDetalle(new DetalleRemito(d4, "Sala B"));

        return new Bolsin(nroBolsin, nroPrecinto, t0, 3.0, cmOrig, cmDest,
                List.of(r1, r2), estBolEnv, t0, emp);
    }
}
