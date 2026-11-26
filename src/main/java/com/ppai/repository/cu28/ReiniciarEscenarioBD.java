package com.ppai.repository.cu28;

import com.ppai.domain.model.cu28.Estado;
import com.ppai.repository.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Herramienta independiente para reiniciar y sembrar el escenario de demostración en BD.
 * Nunca se invoca destructivamente en los arranques normales de la aplicación ni en finCU().
 */
public final class ReiniciarEscenarioBD {

    private ReiniciarEscenarioBD() {
    }

    public static void main(String[] args) {
        String perfil = (args != null && args.length > 0 && args[0] != null && !args[0].isBlank())
                ? args[0].trim().toLowerCase()
                : "cmc";
        reiniciar(perfil);
        System.out.println("[SEEDER] Base de datos reiniciada en '" + ConexionBD.getUrlActual()
                + "' con sesión activa para perfil: " + perfil);
    }

    /**
     * Limpia tablas operacionales, asegura catálogos e inserta el escenario completo:
     * 9 estados, 7 CMs, 4 tipos de documento, empleados/usuarios, 1 sesión activa y
     * 7 bolsines (incluyendo bolsín N° 2 / precinto 8820 con 2 remitos y múltiples documentaciones).
     */
    public static synchronized void reiniciar(String perfil) {
        String perfilNormalizado = (perfil != null && perfil.equalsIgnoreCase("rosario")) ? "rosario" : "cmc";
        LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        try (Connection conn = ConexionBD.abrirConexion()) {
            conn.setAutoCommit(false);
            try {
                limpiarTablasOperacionales(conn);
                sembrarEstados(conn);
                sembrarComisionesMedicas(conn);
                sembrarTiposDocumento(conn);
                sembrarEmpleadosUsuariosYSesion(conn, perfilNormalizado, ahora);
                sembrarBolsinesYRemitos(conn, ahora);
                conn.commit();
            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error al reiniciar escenario en BD", e);
        }
    }

    private static void limpiarTablasOperacionales(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM cambio_estado_bolsin");
            st.executeUpdate("DELETE FROM cambio_estado_documentacion");
            st.executeUpdate("DELETE FROM detalle_remito");
            st.executeUpdate("DELETE FROM remito");
            st.executeUpdate("DELETE FROM bolsin");
            st.executeUpdate("DELETE FROM documentacion");
            st.executeUpdate("DELETE FROM sesion");
            st.executeUpdate("DELETE FROM usuario");
            st.executeUpdate("DELETE FROM empleado");
        }
    }

    private static void sembrarEstados(Connection conn) throws SQLException {
        String sql = "MERGE INTO estado (nombre, ambito, descripcion) KEY (nombre, ambito) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            mergeEstado(ps, Estado.NOMBRE_ENVIADO, Estado.AMBITO_BOLSIN, "Bolsín enviado hacia CM destino");
            mergeEstado(ps, Estado.NOMBRE_RECIBIDO_CM_DESTINO, Estado.AMBITO_BOLSIN, "Bolsín recibido en CM destino");
            mergeEstado(ps, Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, Estado.AMBITO_REMITO, "Remito recibido y aceptado");
            mergeEstado(ps, Estado.NOMBRE_RECIBIDO_Y_ACEPTADO_PARCIAL, Estado.AMBITO_REMITO, "Remito recibido con observaciones");
            mergeEstado(ps, Estado.NOMBRE_EN_BOLSIN_ENVIADO, Estado.AMBITO_DOCUMENTACION, "Documentación en bolsín enviado");
            mergeEstado(ps, Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, Estado.AMBITO_DOCUMENTACION, "Documentación recibida y aceptada");
            mergeEstado(ps, Estado.NOMBRE_NO_RECIBIDA, Estado.AMBITO_DOCUMENTACION, "Documentación faltante (no recibida)");
            mergeEstado(ps, Estado.NOMBRE_RECIBIDA_Y_RECHAZADA, Estado.AMBITO_DOCUMENTACION, "Documentación rechazada por destino erróneo");
            mergeEstado(ps, Estado.NOMBRE_PARA_REDIRIGIR, Estado.AMBITO_DOCUMENTACION, "Documentación para redirigir internamente en CMC");
        }
    }

    private static void mergeEstado(PreparedStatement ps, String nombre, String ambito, String desc) throws SQLException {
        ps.setString(1, nombre);
        ps.setString(2, ambito);
        ps.setString(3, desc);
        ps.executeUpdate();
    }

    private static void sembrarComisionesMedicas(Connection conn) throws SQLException {
        String sql = "MERGE INTO comision_medica (codigo, nombre, direccion, email, telefono) KEY (codigo) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            mergeCM(ps, 1, "Comisión Médica Central (CMC) - CABA", "Av. Roque Sáenz Peña 1100", "despacho.cmc@srt.gob.ar", "011-4321-1000");
            mergeCM(ps, 2, "CMJ N° 7 - Rosario", "Bv. Oroño 850", "despacho.rosario@srt.gob.ar", "0341-440-2000");
            mergeCM(ps, 10, "CMJ N° 1 - Córdoba Capital", "Av. Colón 450", "despacho.cordoba@srt.gob.ar", "0351-420-1100");
            mergeCM(ps, 11, "CMJ N° 4 - Mendoza", "San Martín 720", "despacho.mendoza@srt.gob.ar", "0261-425-3300");
            mergeCM(ps, 12, "CMJ N° 11 - La Plata", "Calle 7 N° 940", "despacho.laplata@srt.gob.ar", "0221-482-4400");
            mergeCM(ps, 14, "CMJ N° 5 - Santa Fe Capital", "25 de Mayo 1800", "despacho.santafe@srt.gob.ar", "0342-455-6600");
            mergeCM(ps, 99, "CMJ N° 99 - Salta", "Belgrano 610", "despacho.salta@srt.gob.ar", "0387-431-9900");
        }
    }

    private static void mergeCM(PreparedStatement ps, int codigo, String nombre, String dir, String email, String tel) throws SQLException {
        ps.setInt(1, codigo);
        ps.setString(2, nombre);
        ps.setString(3, dir);
        ps.setString(4, email);
        ps.setString(5, tel);
        ps.executeUpdate();
    }

    private static void sembrarTiposDocumento(Connection conn) throws SQLException {
        String sql = "MERGE INTO tipo_documento (nombre, descripcion) KEY (nombre) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            mergeTipo(ps, "Expediente de Accidente Laboral", "Expediente administrativo ART");
            mergeTipo(ps, "Dictamen Médico Legal", "Resolución médica pericial");
            mergeTipo(ps, "Estudio Clínico / Resonancia", "Estudio complementario de diagnóstico");
            mergeTipo(ps, "Carta Documento Notificada", "Notificación fehaciente");
        }
    }

    private static void mergeTipo(PreparedStatement ps, String nombre, String desc) throws SQLException {
        ps.setString(1, nombre);
        ps.setString(2, desc);
        ps.executeUpdate();
    }

    private static void sembrarEmpleadosUsuariosYSesion(Connection conn, String perfil, LocalDateTime ahora) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO empleado (nombre, comision_medica_codigo) VALUES (?, ?)")) {
            ps.setString(1, "Juan Pérez (Encargado Central)");
            ps.setInt(2, 1);
            ps.executeUpdate();

            ps.setString(1, "Mariana López (Encargada Rosario)");
            ps.setInt(2, 2);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO usuario (nombre, descripcion, empleado_nombre) VALUES (?, ?, ?)")) {
            ps.setString(1, "jperez.cmc");
            ps.setString(2, "Operador de Bolsines - CMC");
            ps.setString(3, "Juan Pérez (Encargado Central)");
            ps.executeUpdate();

            ps.setString(1, "mlopez.rosario");
            ps.setString(2, "Operadora de Bolsines - CMJ Rosario");
            ps.setString(3, "Mariana López (Encargada Rosario)");
            ps.executeUpdate();
        }

        String usuarioActivo = "rosario".equals(perfil) ? "mlopez.rosario" : "jperez.cmc";
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sesion (usuario_nombre, fecha_hora_inicio, fecha_hora_fin) VALUES (?, ?, NULL)")) {
            ps.setString(1, usuarioActivo);
            ps.setTimestamp(2, Timestamp.valueOf(ahora));
            ps.executeUpdate();
        }
    }

    private static void sembrarBolsinesYRemitos(Connection conn, LocalDateTime ahora) throws SQLException {
        LocalDateTime f3d = ahora.minusDays(3);
        LocalDateTime f2d = ahora.minusDays(2);
        LocalDateTime f1d = ahora.minusDays(1);
        LocalDateTime f18h = ahora.minusHours(18);
        LocalDateTime f5d = ahora.minusDays(5);

        // Bolsín 1 (precinto 7041): Córdoba -> CMC (ENVIADO), 1 remito (458), 2 docs
        insertarBolsin(conn, 1, 7041, f3d, 2.4, 10, 1, Estado.NOMBRE_ENVIADO);
        insertarRemito(conn, 458, f3d, 1, 10, 1, null, null);
        insertarDocYDetalle(conn, 101, "EXP-2026-904: Trámite incapacidad laboral Gómez", f3d,
                "Expediente de Accidente Laboral", 10, 458, "Mesa de Entradas", f3d);
        insertarDocYDetalle(conn, 102, "DIC-2026-312: Dictamen traumatológico pericial", f3d,
                "Dictamen Médico Legal", 10, 458, "Junta Médica", f3d);

        // Bolsín 2 (precinto 8820): Mendoza -> CMC (ENVIADO), MULTI-REMITO (892 + 893), 4 docs
        insertarBolsin(conn, 2, 8820, f2d, 3.1, 11, 1, Estado.NOMBRE_ENVIADO);
        insertarRemito(conn, 892, f2d, 2, 11, 1, null, null);
        insertarDocYDetalle(conn, 201, "EXP-2026-1188: Evaluación secuelas respiratorias", f2d,
                "Expediente de Accidente Laboral", 11, 892, "Área Neumología", f2d);
        insertarDocYDetalle(conn, 202, "EST-2026-049: Tomografía computada de tórax", f2d,
                "Estudio Clínico / Resonancia", 11, 892, "Área Diagnóstico", f2d);
        insertarRemito(conn, 893, f2d, 2, 11, 1, null, null);
        insertarDocYDetalle(conn, 203, "CD-2026-881: Intimación empleador ART preventiva", f2d,
                "Carta Documento Notificada", 11, 893, "Legales", f2d);
        insertarDocYDetalle(conn, 204, "DIC-2026-505: Junta médica interdisciplinaria", f2d,
                "Dictamen Médico Legal", 11, 893, "Junta Médica", f2d);

        // Bolsín 3 (precinto 9150): La Plata -> CMC (ENVIADO), 1 remito (1204), 1 doc
        insertarBolsin(conn, 3, 9150, f18h, 1.8, 12, 1, Estado.NOMBRE_ENVIADO);
        insertarRemito(conn, 1204, f18h, 3, 12, 1, null, null);
        insertarDocYDetalle(conn, 301, "EXP-2026-3401: Apelación Dictamen CMC Sala B", f18h,
                "Expediente de Accidente Laboral", 12, 1204, "Sala B - Apelaciones", f18h);

        // Bolsín 4 (precinto 6015): Santa Fe -> Rosario (ENVIADO), 1 remito (2110), 2 docs
        insertarBolsin(conn, 4, 6015, f2d, 2.0, 14, 2, Estado.NOMBRE_ENVIADO);
        insertarRemito(conn, 2110, f2d, 4, 14, 2, null, null);
        insertarDocYDetalle(conn, 401, "EXP-2026-4102: Derivación interjurisdiccional Santa Fe", f2d,
                "Expediente de Accidente Laboral", 14, 2110, "Mesa de Entradas", f2d);
        insertarDocYDetalle(conn, 402, "EST-2026-198: Electromiograma miembro superior", f2d,
                "Estudio Clínico / Resonancia", 14, 2110, "Consultorios", f2d);

        // Bolsín 5 (precinto 6088): CMC -> Rosario (ENVIADO), 1 remito (2145), 3 docs
        insertarBolsin(conn, 5, 6088, f1d, 2.7, 1, 2, Estado.NOMBRE_ENVIADO);
        insertarRemito(conn, 2145, f1d, 5, 1, 2, null, null);
        insertarDocYDetalle(conn, 501, "DIC-2026-710: Devolución expediente apelado CMC", f1d,
                "Dictamen Médico Legal", 1, 2145, "Mesa de Entradas", f1d);
        insertarDocYDetalle(conn, 502, "EXP-2026-2905: Reapertura trámite incapacidad", f1d,
                "Expediente de Accidente Laboral", 1, 2145, "Mesa de Entradas", f1d);
        insertarDocYDetalle(conn, 503, "CD-2026-912: Notificación fehaciente al trabajador", f1d,
                "Carta Documento Notificada", 1, 2145, "Notificaciones", f1d);

        // Bolsín 6 (precinto 9999): Rosario -> Salta (ENVIADO, control negativo CM destino)
        insertarBolsin(conn, 6, 9999, f1d, 1.2, 2, 99, Estado.NOMBRE_ENVIADO);
        insertarRemito(conn, 9999, f1d, 6, 2, 99, null, null);
        insertarDocYDetalle(conn, 900, "Trámite de otra provincia", f1d,
                "Expediente de Accidente Laboral", 2, 9999, "Mesa de Entradas", f1d);

        // Bolsín 7 (precinto 5500): Córdoba -> CMC (RECIBIDO_EN_CM_DESTINO, control negativo estado)
        insertarBolsin(conn, 7, 5500, f5d, 1.5, 10, 1, Estado.NOMBRE_RECIBIDO_CM_DESTINO);
        insertarRemito(conn, 112, f5d, 7, 10, 1, Estado.NOMBRE_RECIBIDO_Y_ACEPTADO, Estado.AMBITO_REMITO);
        insertarDocYDetalle(conn, 950, "DIC-2026-099: Bolsín ya recepcionado anteriormente", f5d,
                "Dictamen Médico Legal", 10, 112, "Archivo", f5d);
    }

    private static void insertarBolsin(Connection conn, int nroBolsin, int nroPrecinto, LocalDateTime fecha,
                                       double peso, int cmOrig, int cmDest, String estadoNombre) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO bolsin (numero_bolsin, numero_precinto, fecha, peso, cm_origen_codigo, cm_destino_codigo) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, nroBolsin);
            ps.setInt(2, nroPrecinto);
            ps.setTimestamp(3, Timestamp.valueOf(fecha));
            ps.setDouble(4, peso);
            ps.setInt(5, cmOrig);
            ps.setInt(6, cmDest);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO cambio_estado_bolsin (bolsin_numero, fecha_hora_inicio, fecha_hora_fin, estado_nombre, estado_ambito, empleado_nombre) "
                        + "VALUES (?, ?, NULL, ?, ?, NULL)")) {
            ps.setInt(1, nroBolsin);
            ps.setTimestamp(2, Timestamp.valueOf(fecha));
            ps.setString(3, estadoNombre);
            ps.setString(4, Estado.AMBITO_BOLSIN);
            ps.executeUpdate();
        }
    }

    private static void insertarRemito(Connection conn, int nroRemito, LocalDateTime fecha, int nroBolsin,
                                       int cmOrig, int cmDest, String estadoNombre, String estadoAmbito) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO remito (numero, fecha, bolsin_numero, cm_origen_codigo, cm_destino_codigo, estado_nombre, estado_ambito) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, nroRemito);
            ps.setTimestamp(2, Timestamp.valueOf(fecha));
            ps.setInt(3, nroBolsin);
            ps.setInt(4, cmOrig);
            ps.setInt(5, cmDest);
            ps.setString(6, estadoNombre);
            ps.setString(7, estadoAmbito);
            ps.executeUpdate();
        }
    }

    private static void insertarDocYDetalle(Connection conn, int nroDoc, String asunto, LocalDateTime fechaPase,
                                            String tipoNombre, int cmOrig, int nroRemito, String areaCMC,
                                            LocalDateTime fechaEstado) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO documentacion (numero, asunto, fecha_pase, tipo_documento_nombre, cm_origen_codigo) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            ps.setInt(1, nroDoc);
            ps.setString(2, asunto);
            ps.setTimestamp(3, Timestamp.valueOf(fechaPase));
            ps.setString(4, tipoNombre);
            ps.setInt(5, cmOrig);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO detalle_remito (remito_numero, documentacion_numero, area_cmc_destino) VALUES (?, ?, ?)")) {
            ps.setInt(1, nroRemito);
            ps.setInt(2, nroDoc);
            ps.setString(3, areaCMC);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO cambio_estado_documentacion (documentacion_numero, fecha_hora_inicio, fecha_hora_fin, estado_nombre, estado_ambito, empleado_nombre) "
                        + "VALUES (?, ?, NULL, ?, ?, NULL)")) {
            ps.setInt(1, nroDoc);
            ps.setTimestamp(2, Timestamp.valueOf(fechaEstado));
            ps.setString(3, Estado.NOMBRE_EN_BOLSIN_ENVIADO);
            ps.setString(4, Estado.AMBITO_DOCUMENTACION);
            ps.executeUpdate();
        }
    }
}
