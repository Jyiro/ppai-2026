package com.ppai;

import com.ppai.boundary.PantallaEjemplo;
import com.ppai.controller.GestorEjemplo;
import com.ppai.repository.JpaUtil;

public class Main {

    public static void main(String[] args) {
        System.out.println("Inicializando Aplicación PPAI 2026...");

        try {
            // Instanciar Gestor y Pantalla (Arquitectura BCE/ECB)
            GestorEjemplo gestor = new GestorEjemplo();
            PantallaEjemplo pantalla = new PantallaEjemplo(gestor);

            // Iniciar flujo de la interfaz
            pantalla.mostrar();

        } finally {
            // Cerrar EntityManagerFactory
            JpaUtil.shutdown();
            System.out.println("Aplicación finalizada correctamente.");
        }
    }
}
