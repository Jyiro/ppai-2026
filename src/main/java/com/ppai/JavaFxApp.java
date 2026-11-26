package com.ppai;

import com.ppai.boundary.cu28.PantallaRecepcionBolsin;
import com.ppai.controller.cu28.GestorRecepcionBolsin;
import com.ppai.domain.model.cu28.Sesion;
import com.ppai.repository.ConexionBD;
import com.ppai.repository.cu28.IRepositorioCU28;
import com.ppai.repository.cu28.MapeadorPersistenciaCU28;
import com.ppai.repository.cu28.ReiniciarEscenarioBD;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Aplicación de Escritorio JavaFX — PPAI 2026 (Diseño de Sistemas de Información).
 * Lee la sesión activa desde la base de datos persistente e inicializa el Gestor y la Pantalla.
 */
public class JavaFxApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║   PPAI 2026 — Sistema de Bolsines (JavaFX Moderno)       ║");
        System.out.println("║   Caso de Uso: CU 28 - Registrar Recepción de Bolsín     ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        // 1. Repositorio Data Mapper sobre H2 persistente en disco (siembra inicial solo si BD vacía)
        IRepositorioCU28 repositorio = new MapeadorPersistenciaCU28();
        if (repositorio.estaVacia()) {
            ReiniciarEscenarioBD.reiniciar("cmc");
        }

        Sesion sesionActual = repositorio.obtenerSesionActiva();
        if (sesionActual == null) {
            throw new IllegalStateException(
                    "No existe ninguna sesión activa en la base de datos. "
                            + "Ejecute scripts/reiniciar-escenario.ps1 para inicializar un usuario activo.");
        }

        System.out.println("[APP] Sesión activa leída de BD: " + sesionActual.getUsuario().getNombre()
                + " | CM: " + sesionActual.getCMEmpleado().getNombre());

        // 2. Instanciación de Gestor (Control) y Pantalla (Boundary)
        GestorRecepcionBolsin gestor = new GestorRecepcionBolsin(sesionActual, repositorio);
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
        ConexionBD.cerrar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
