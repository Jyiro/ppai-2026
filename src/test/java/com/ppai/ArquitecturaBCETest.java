package com.ppai;

import com.ppai.boundary.cu28.DocumentacionRow;
import com.ppai.boundary.cu28.PantallaRecepcionBolsin;
import com.ppai.boundary.cu29.InterfazServidorCorreo;
import com.ppai.controller.cu28.GestorRecepcionBolsin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gate de Verificación Íntegra de Fase 4 (Bloqueante):
 * Verifica el cumplimiento estricto de la arquitectura BCE en toda la capa Boundary
 * y en la frontera pública del GestorRecepcionBolsin.
 */
public class ArquitecturaBCETest {

    private static final List<Class<?>> CLASES_BOUNDARY = List.of(
            PantallaRecepcionBolsin.class,
            DocumentacionRow.class,
            InterfazServidorCorreo.class
    );

    private static final List<String> ESTADOS_DOMINIO_PROHIBIDOS = List.of(
            "Recibido en CM Destino",
            "No recibida",
            "Recibida y rechazada",
            "Para redirigir",
            "Recibido y aceptado",
            "Recibida y aceptada",
            "Recibido y aceptado parcial",
            "En bolsín enviado"
    );

    @Test
    @DisplayName("BCE Estático: 0 imports de dominio/iteradores/repositorio/fechas en todo com.ppai.boundary.**")
    void testImportsPermitidosEnTodoBoundary() throws IOException {
        Path boundaryRoot = Paths.get("src", "main", "java", "com", "ppai", "boundary");
        assertTrue(Files.exists(boundaryRoot));

        Pattern importPattern = Pattern.compile("^\\s*import\\s+([^;]+);", Pattern.MULTILINE);
        List<Path> archivos;
        try (Stream<Path> walk = Files.walk(boundaryRoot)) {
            archivos = walk.filter(p -> p.toString().endsWith(".java")).toList();
        }
        assertFalse(archivos.isEmpty());

        for (Path archivo : archivos) {
            String source = Files.readString(archivo);
            Matcher matcher = importPattern.matcher(source);
            while (matcher.find()) {
                String imported = matcher.group(1).trim();
                boolean permitido = imported.startsWith("com.ppai.controller.")
                        || imported.startsWith("java.")
                        || imported.startsWith("javafx.");
                assertTrue(permitido,
                        archivo.getFileName() + " tiene un import prohibido por BCE: " + imported);

                assertFalse(imported.startsWith("com.ppai.domain."),
                        archivo.getFileName() + " no debe importar dominio: " + imported);
                assertFalse(imported.startsWith("com.ppai.iterator."),
                        archivo.getFileName() + " no debe importar iteradores: " + imported);
                assertFalse(imported.startsWith("com.ppai.repository."),
                        archivo.getFileName() + " no debe importar repositorio: " + imported);

                assertFalse(imported.equals("java.time.LocalDateTime")
                                || imported.equals("java.time.LocalDate")
                                || imported.equals("java.time.format.DateTimeFormatter"),
                        archivo.getFileName() + " no debe importar clases de fecha/formateo: " + imported);
            }
        }
    }

    @Test
    @DisplayName("BCE Reflexión: 0 campos, parámetros o retornos de dominio/iterador/repositorio en Boundary")
    void testReflexionCeroEntidadesEnBoundary() {
        for (Class<?> clazz : CLASES_BOUNDARY) {
            for (Field field : clazz.getDeclaredFields()) {
                assertTipoNoEsInfraODominio(clazz, "campo " + field.getName(), field.getType());
            }
            for (Method method : clazz.getDeclaredMethods()) {
                assertTipoNoEsInfraODominio(clazz, "retorno de " + method.getName(), method.getReturnType());
                for (Class<?> paramType : method.getParameterTypes()) {
                    assertTipoNoEsInfraODominio(clazz, "parámetro de " + method.getName(), paramType);
                }
            }
        }
    }

    @Test
    @DisplayName("BCE Estático: 0 literales de nombres de estados del dominio en PantallaRecepcionBolsin y DocumentacionRow")
    void testSinLiteralesDeEstadosEnPantallaNiRow() throws IOException {
        for (Path archivo : List.of(
                Paths.get("src", "main", "java", "com", "ppai", "boundary", "cu28", "PantallaRecepcionBolsin.java"),
                Paths.get("src", "main", "java", "com", "ppai", "boundary", "cu28", "DocumentacionRow.java")
        )) {
            String source = Files.readString(archivo);
            for (String literalProhibido : ESTADOS_DOMINIO_PROHIBIDOS) {
                assertFalse(source.contains("\"" + literalProhibido + "\""),
                        archivo.getFileName() + " no debe contener literal de estado: " + literalProhibido);
            }
            assertFalse(source.contains("LocalDateTime"),
                    archivo.getFileName() + " no debe usar LocalDateTime");
            assertFalse(source.contains("DateTimeFormatter"),
                    archivo.getFileName() + " no debe usar DateTimeFormatter");
        }
    }

    @Test
    @DisplayName("BCE Controlador: GestorRecepcionBolsin desacoplado de Mapeador/Correo y sin entidades en API pública")
    void testControladorFronteraPrimitivosEInversionDependencias() throws IOException {
        Path gestorPath = Paths.get("src", "main", "java", "com", "ppai", "controller", "cu28", "GestorRecepcionBolsin.java");
        String gestorSrc = Files.readString(gestorPath);

        assertFalse(gestorSrc.contains("MapeadorPersistenciaCU28"),
                "GestorRecepcionBolsin debe depender solo de IRepositorioCU28");
        assertFalse(gestorSrc.contains("InterfazServidorCorreo"),
                "GestorRecepcionBolsin debe delegar el correo en GestorNotificarRecepcionBolsin");

        for (Method method : GestorRecepcionBolsin.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            String retPkg = method.getReturnType().getName();
            assertFalse(retPkg.startsWith("com.ppai.domain."),
                    "Método público del Gestor expone entidad en retorno: " + method.getName());
            for (Class<?> paramType : method.getParameterTypes()) {
                assertFalse(paramType.getName().startsWith("com.ppai.domain."),
                        "Método público del Gestor recibe entidad en parámetro: " + method.getName());
            }
        }
    }

    private void assertTipoNoEsInfraODominio(Class<?> owner, String contexto, Class<?> type) {
        Class<?> base = type.isArray() ? type.getComponentType() : type;
        String name = base.getName();
        assertFalse(name.startsWith("com.ppai.domain."),
                owner.getSimpleName() + " (" + contexto + ") referencia dominio: " + name);
        assertFalse(name.startsWith("com.ppai.iterator."),
                owner.getSimpleName() + " (" + contexto + ") referencia iterator: " + name);
        assertFalse(name.startsWith("com.ppai.repository."),
                owner.getSimpleName() + " (" + contexto + ") referencia repository: " + name);
    }
}
