package com.ppai;

import com.ppai.domain.model.EjemploEntidad;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MainTest {

    @Test
    void testCreacionEntidadYEstadoInicial() {
        EjemploEntidad entidad = new EjemploEntidad("Prueba Unit Test");
        
        assertNotNull(entidad);
        assertEquals("Prueba Unit Test", entidad.getNombre());
        assertEquals("INICIAL", entidad.getEstadoActual().getNombre());
        assertFalse(entidad.getEstadoActual().esFinalizado());
    }

    @Test
    void testTransicionDeEstado() {
        EjemploEntidad entidad = new EjemploEntidad("Prueba Estado");
        entidad.finalizar();
        
        assertEquals("FINALIZADO", entidad.getEstadoActual().getNombre());
        assertTrue(entidad.getEstadoActual().esFinalizado());
        assertThrows(IllegalStateException.class, entidad::ejecutarOperacion);
    }
}
