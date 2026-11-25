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

import java.time.LocalDateTime;

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
        ComisionMedica cmDestino = new ComisionMedica(1, "Comisión Médica Central (CMC) - CABA",
                "Av. Roque Sáenz Peña 1100", "despacho.cmc@srt.gob.ar", "011-4321-1000");
        Empleado empleado = new Empleado("Juan Pérez (Encargado Central)", cmDestino);
        Usuario usuario = new Usuario("jperez.cmc", "Operador Central", empleado);
        Sesion sesionActual = new Sesion(usuario, LocalDateTime.now());

        System.out.println("[APP] Sesión iniciada como: " + usuario.getNombre()
                + " | CM: " + empleado.getCM().getNombre());

        // 2. Instanciación pura orientada a objetos de Gestor (Control) y Pantalla (Boundary)
        GestorRecepcionBolsin gestor = new GestorRecepcionBolsin(sesionActual);
        PantallaRecepcionBolsin pantalla = new PantallaRecepcionBolsin(gestor);
        gestor.setPantalla(pantalla);

        // 3. Configuración de Escena y Ventana (Adaptativa a la resolución de pantalla)
        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        double initWidth = Math.min(1200, screenBounds.getWidth() * 0.94);
        double initHeight = Math.min(780, screenBounds.getHeight() * 0.92);

        Scene scene = new Scene(pantalla, initWidth, initHeight);
        primaryStage.setTitle("PPAI 2026 — DSI | CU 28: Registrar Recepción de Bolsín");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(820);
        primaryStage.setMinHeight(560);
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
