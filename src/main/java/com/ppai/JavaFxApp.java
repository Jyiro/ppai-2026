package com.ppai;

import com.ppai.boundary.cu28.PantallaRecepcionBolsin;
import com.ppai.controller.cu28.GestorRecepcionBolsin;
import com.ppai.domain.model.cu28.ComisionMedica;
import com.ppai.domain.model.cu28.Empleado;
import com.ppai.domain.model.cu28.Sesion;
import com.ppai.domain.model.cu28.Usuario;
import com.ppai.repository.JpaUtil;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Aplicación de Escritorio JavaFX — PPAI 2026 (Diseño de Sistemas de Información).
 * Inicializa el contexto de objetos, el Gestor de CU y la Pantalla Boundary.
 */
public class JavaFxApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║   PPAI 2026 — Sistema de Bolsines (JavaFX Moderno)       ║");
        System.out.println("║   Caso de Uso: CU 28 - Registrar Recepción de Bolsín     ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        // 1. Contexto de Sesión inicial (Usuario logueado con su Empleado y Comisión Médica)
        ComisionMedica cmDestino = new ComisionMedica(1L, "Comisión Médica Central (CMC) - CABA");
        Empleado empleado = new Empleado(100L, "Juan Pérez (Encargado Central)", cmDestino);
        Usuario usuario = new Usuario("jperez.cmc", empleado);
        Sesion sesionActual = new Sesion(usuario);

        System.out.println("[APP] Sesión iniciada como: " + usuario.getUsername()
                + " | CM: " + empleado.getCM().getNombre());

        // 2. Instanciación pura orientada a objetos de Gestor (Control) y Pantalla (Boundary)
        GestorRecepcionBolsin gestor = new GestorRecepcionBolsin(sesionActual);
        PantallaRecepcionBolsin pantalla = new PantallaRecepcionBolsin(gestor);
        gestor.setPantalla(pantalla);

        // 3. Configuración de Escena y Ventana
        Scene scene = new Scene(pantalla, 1120, 760);
        primaryStage.setTitle("PPAI 2026 — DSI | CU 28: Registrar Recepción de Bolsín");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(980);
        primaryStage.setMinHeight(680);
        primaryStage.show();

        // 4. DS-CU28 paso 1: Inicio formal del caso de uso
        pantalla.tomarOpcionRegistrarRecepcionBolsin();
    }

    @Override
    public void stop() {
        JpaUtil.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
