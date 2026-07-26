package com.ppai.boundary;

import com.ppai.controller.GestorEjemplo;
import com.ppai.domain.model.EjemploEntidad;

import java.util.List;
import java.util.Scanner;

/**
 * Boundary / Pantalla de Interfaz (PPAI / ECB Pattern).
 * En la arquitectura OO representa la vista/interfaz de usuario.
 */
public class PantallaEjemplo {

    private final GestorEjemplo gestor;

    public PantallaEjemplo(GestorEjemplo gestor) {
        this.gestor = gestor;
    }

    /**
     * Muestra la interfaz e inicia el flujo del usuario.
     */
    public void mostrar() {
        System.out.println("=================================================");
        System.out.println("   PPAI 2026 - PANTALLA DE DEMOSTRACIÓN (BOUNDARY) ");
        System.out.println("=================================================");

        // Inicia el caso de uso notificando al gestor
        gestor.opcionRegistrarEjemplo();

        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese nombre para la nueva entidad de prueba: ");
        String nombre = scanner.nextLine();

        if (nombre == null || nombre.isBlank()) {
            nombre = "Elemento Demo PPAI";
        }

        // Delegar al Gestor la creación
        gestor.crearNuevaEntidad(nombre);

        // Mostrar resultados
        mostrarEntidadesExistentes();

        // Probar transición de estado
        System.out.println("\nEjecutando regla de negocio y cambio de estado...");
        gestor.finalizarEntidad();
    }

    public void mostrarEntidadesExistentes() {
        List<EjemploEntidad> lista = gestor.buscarEntidades();
        System.out.println("\n--- Entidades Registradas en H2 Database ---");
        for (EjemploEntidad e : lista) {
            System.out.println("ID: " + e.getId() + " | Nombre: " + e.getNombre() + " | Fecha: " + e.getFechaCreacion());
        }
        System.out.println("---------------------------------------------");
    }
}
