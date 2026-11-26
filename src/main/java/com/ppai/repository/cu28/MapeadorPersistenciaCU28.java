package com.ppai.repository.cu28;

import com.ppai.domain.model.cu28.Bolsin;
import com.ppai.domain.model.cu28.CambioEstadoBolsin;
import com.ppai.domain.model.cu28.CambioEstadoDocumentacion;
import com.ppai.domain.model.cu28.ComisionMedica;
import com.ppai.domain.model.cu28.DetalleRemito;
import com.ppai.domain.model.cu28.Documentacion;
import com.ppai.domain.model.cu28.Empleado;
import com.ppai.domain.model.cu28.Estado;
import com.ppai.domain.model.cu28.Remito;
import com.ppai.domain.model.cu28.Sesion;
import com.ppai.domain.model.cu28.TipoDocumento;
import com.ppai.domain.model.cu28.Usuario;
import com.ppai.repository.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Mapper (Fabricación Pura) en JDBC para el CU 28.
 * Traduce bidireccionalmente entre filas relacionales H2 (con PKs naturales y compuestas)
 * y las 12 entidades puras del Dominio OO, manteniendo al Dominio 100% libre de JPA/SQL.
 */
public class MapeadorPersistenciaCU28 implements IRepositorioCU28 {

    @Override
    public boolean estaVacia() {
        try (Connection conn = ConexionBD.abrirConexion()) {
            return contarRegistros(conn, "bolsin") == 0 && contarRegistros(conn, "sesion") == 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Error verificando si la base de datos está vacía", e);
        }
    }

    @Override
    public Sesion obtenerSesionActiva() {
        String sql = """
                SELECT s.usuario_nombre, s.fecha_hora_inicio, s.fecha_hora_fin,
                       u.descripcion AS usuario_desc,
                       e.nombre AS emp_nombre,
                       cm.codigo, cm.nombre AS cm_nombre, cm.direccion, cm.email, cm.telefono
                FROM sesion s
                JOIN usuario u ON u.nombre = s.usuario_nombre
                JOIN empleado e ON e.nombre = u.empleado_nombre
                JOIN comision_medica cm ON cm.codigo = e.comision_medica_codigo
                WHERE s.fecha_hora_fin IS NULL
                ORDER BY s.fecha_hora_inicio DESC
                FETCH FIRST 1 ROWS ONLY
                """;
        try (Connection conn = ConexionBD.abrirConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            ComisionMedica cm = new ComisionMedica(
                    rs.getInt("codigo"),
                    rs.getString("cm_nombre"),
                    rs.getString("direccion"),
                    rs.getString("email"),
                    rs.getString("telefono")
            );
            Empleado emp = new Empleado(rs.getString("emp_nombre"), cm);
            Usuario usr = new Usuario(rs.getString("usuario_nombre"), rs.getString("usuario_desc"), emp);
            return new Sesion(usr, aLocalDateTime(rs.getTimestamp("fecha_hora_inicio")));
        } catch (SQLException e) {
            throw new IllegalStateException("Error recuperando sesión activa desde la BD", e);
        }
    }

    @Override
    public List<Estado> obtenerEstados() {
        try (Connection conn = ConexionBD.abrirConexion()) {
            return new ArrayList<>(cargarEstados(conn).values());
        } catch (SQLException e) {
            throw new IllegalStateException("Error consultando estados en la BD", e);
        }
    }

    @Override
    public List<ComisionMedica> obtenerComisionesMedicas() {
        try (Connection conn = ConexionBD.abrirConexion()) {
            return new ArrayList<>(cargarComisiones(conn).values());
        } catch (SQLException e) {
            throw new IllegalStateException("Error consultando comisiones médicas en la BD", e);
        }
    }

    @Override
    public List<Bolsin> obtenerTodosBolsines() {
        try (Connection conn = ConexionBD.abrirConexion()) {
            return cargarBolsinesCompletos(conn);
        } catch (SQLException e) {
            throw new IllegalStateException("Error cargando bolsines desde la BD", e);
        }
    }

    @Override
    public synchronized Bolsin actualizarBolsin(Bolsin bolsin, Empleado empleadoResponsable) {
        if (bolsin == null) {
            return null;
        }
        try (Connection conn = ConexionBD.abrirConexion()) {
            conn.setAutoCommit(false);
            try {
                if (empleadoResponsable != null) {
                    upsertEmpleadoYComision(conn, empleadoResponsable);
                }

                for (CambioEstadoBolsin cambio : bolsin.getCambiosDeEstado()) {
                    upsertCambioEstadoBolsin(conn, bolsin.getNumeroBolsin(), cambio);
                }

                for (Remito remito : bolsin.getRemitos()) {
                    actualizarEstadoRemito(conn, remito);
                    for (DetalleRemito det : remito.getDetalles()) {
                        Documentacion doc = det.getDocumentacion();
                        if (doc != null) {
                            for (CambioEstadoDocumentacion ced : doc.getCambiosDeEstado()) {
                                upsertCambioEstadoDocumentacion(conn, doc.getNumero(), ced);
                            }
                        }
                    }
                }

                conn.commit();
            } catch (SQLException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }

            return buscarBolsinEnConexion(conn, bolsin.getNumeroBolsin());
        } catch (SQLException e) {
            throw new IllegalStateException("Error persistiendo recepción del bolsín " + bolsin.getNumeroBolsin(), e);
        }
    }

    // =========================================================================
    // Hidratación Relacional -> Objetos Puros del Dominio
    // =========================================================================

    private List<Bolsin> cargarBolsinesCompletos(Connection conn) throws SQLException {
        Map<Integer, ComisionMedica> cms = cargarComisiones(conn);
        Map<String, TipoDocumento> tiposDoc = cargarTiposDocumento(conn);
        Map<String, Estado> estados = cargarEstados(conn);
        Map<String, Empleado> empleados = cargarEmpleados(conn, cms);

        Map<Integer, Documentacion> docs = cargarDocumentaciones(conn, tiposDoc, cms);
        cargarCambiosEstadoDocumentacion(conn, docs, estados, empleados);

        Map<Integer, Remito> remitosPorNumero = new LinkedHashMap<>();
        Map<Integer, List<Remito>> remitosPorBolsin = cargarRemitos(conn, cms, estados, remitosPorNumero);
        cargarDetallesRemito(conn, remitosPorNumero, docs);

        Map<Integer, Bolsin> bolsinesPorNumero = new LinkedHashMap<>();
        List<Bolsin> resultado = new ArrayList<>();
        String sqlBolsin = "SELECT numero_bolsin, numero_precinto, fecha, peso, cm_origen_codigo, cm_destino_codigo "
                + "FROM bolsin ORDER BY numero_bolsin";
        try (PreparedStatement ps = conn.prepareStatement(sqlBolsin);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int nroBolsin = rs.getInt("numero_bolsin");
                Bolsin b = new Bolsin(
                        nroBolsin,
                        rs.getInt("numero_precinto"),
                        aLocalDateTime(rs.getTimestamp("fecha")),
                        rs.getDouble("peso"),
                        cms.get(rs.getInt("cm_origen_codigo")),
                        cms.get(rs.getInt("cm_destino_codigo")),
                        remitosPorBolsin.getOrDefault(nroBolsin, List.of()),
                        null,
                        null,
                        null
                );
                bolsinesPorNumero.put(nroBolsin, b);
                resultado.add(b);
            }
        }

        cargarCambiosEstadoBolsin(conn, bolsinesPorNumero, estados, empleados);
        return resultado;
    }

    private Bolsin buscarBolsinEnConexion(Connection conn, int numeroBolsin) throws SQLException {
        for (Bolsin b : cargarBolsinesCompletos(conn)) {
            if (b.getNumeroBolsin() == numeroBolsin) {
                return b;
            }
        }
        return null;
    }

    private Map<Integer, ComisionMedica> cargarComisiones(Connection conn) throws SQLException {
        Map<Integer, ComisionMedica> map = new LinkedHashMap<>();
        String sql = "SELECT codigo, nombre, direccion, email, telefono FROM comision_medica ORDER BY codigo";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ComisionMedica cm = new ComisionMedica(
                        rs.getInt("codigo"),
                        rs.getString("nombre"),
                        rs.getString("direccion"),
                        rs.getString("email"),
                        rs.getString("telefono")
                );
                map.put(cm.getCodigo(), cm);
            }
        }
        return map;
    }

    private Map<String, TipoDocumento> cargarTiposDocumento(Connection conn) throws SQLException {
        Map<String, TipoDocumento> map = new LinkedHashMap<>();
        String sql = "SELECT nombre, descripcion FROM tipo_documento ORDER BY nombre";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TipoDocumento td = new TipoDocumento(rs.getString("nombre"), rs.getString("descripcion"));
                map.put(td.getNombre(), td);
            }
        }
        return map;
    }

    private Map<String, Estado> cargarEstados(Connection conn) throws SQLException {
        Map<String, Estado> map = new LinkedHashMap<>();
        String sql = "SELECT nombre, descripcion, ambito FROM estado ORDER BY ambito, nombre";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Estado est = new Estado(
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        rs.getString("ambito")
                );
                map.put(claveEstado(est.getNombre(), est.getAmbito()), est);
            }
        }
        return map;
    }

    private Map<String, Empleado> cargarEmpleados(Connection conn, Map<Integer, ComisionMedica> cms) throws SQLException {
        Map<String, Empleado> map = new LinkedHashMap<>();
        String sql = "SELECT nombre, comision_medica_codigo FROM empleado ORDER BY nombre";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Empleado emp = new Empleado(rs.getString("nombre"), cms.get(rs.getInt("comision_medica_codigo")));
                map.put(emp.getNombre(), emp);
            }
        }
        return map;
    }

    private Map<Integer, Documentacion> cargarDocumentaciones(Connection conn,
                                                              Map<String, TipoDocumento> tiposDoc,
                                                              Map<Integer, ComisionMedica> cms) throws SQLException {
        Map<Integer, Documentacion> map = new LinkedHashMap<>();
        String sql = "SELECT numero, asunto, fecha_pase, tipo_documento_nombre, cm_origen_codigo FROM documentacion ORDER BY numero";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int num = rs.getInt("numero");
                Documentacion doc = new Documentacion(
                        num,
                        rs.getString("asunto"),
                        aLocalDateTime(rs.getTimestamp("fecha_pase")),
                        tiposDoc.get(rs.getString("tipo_documento_nombre")),
                        cms.get(rs.getInt("cm_origen_codigo")),
                        null,
                        null,
                        null
                );
                map.put(num, doc);
            }
        }
        return map;
    }

    private void cargarCambiosEstadoDocumentacion(Connection conn,
                                                  Map<Integer, Documentacion> docs,
                                                  Map<String, Estado> estados,
                                                  Map<String, Empleado> empleados) throws SQLException {
        String sql = "SELECT documentacion_numero, fecha_hora_inicio, fecha_hora_fin, estado_nombre, estado_ambito, empleado_nombre "
                + "FROM cambio_estado_documentacion ORDER BY documentacion_numero, fecha_hora_inicio ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Documentacion doc = docs.get(rs.getInt("documentacion_numero"));
                if (doc == null) {
                    continue;
                }
                Estado estado = estados.get(claveEstado(rs.getString("estado_nombre"), rs.getString("estado_ambito")));
                String empNombre = rs.getString("empleado_nombre");
                Empleado emp = empNombre != null ? empleados.get(empNombre) : null;
                CambioEstadoDocumentacion cambio = new CambioEstadoDocumentacion(
                        aLocalDateTime(rs.getTimestamp("fecha_hora_inicio")),
                        estado,
                        emp
                );
                cambio.setFechaHoraFin(aLocalDateTime(rs.getTimestamp("fecha_hora_fin")));
                doc.getCambiosDeEstado().add(cambio);
            }
        }
    }

    private Map<Integer, List<Remito>> cargarRemitos(Connection conn,
                                                       Map<Integer, ComisionMedica> cms,
                                                       Map<String, Estado> estados,
                                                       Map<Integer, Remito> remitosPorNumero) throws SQLException {
        Map<Integer, List<Remito>> porBolsin = new HashMap<>();
        String sql = "SELECT numero, fecha, bolsin_numero, cm_origen_codigo, cm_destino_codigo, estado_nombre, estado_ambito "
                + "FROM remito ORDER BY numero";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int numRemito = rs.getInt("numero");
                int numBolsin = rs.getInt("bolsin_numero");
                Remito r = new Remito(
                        numRemito,
                        aLocalDateTime(rs.getTimestamp("fecha")),
                        cms.get(rs.getInt("cm_origen_codigo")),
                        cms.get(rs.getInt("cm_destino_codigo"))
                );
                String estNombre = rs.getString("estado_nombre");
                String estAmbito = rs.getString("estado_ambito");
                if (estNombre != null && estAmbito != null) {
                    r.setEstado(estados.get(claveEstado(estNombre, estAmbito)));
                }
                remitosPorNumero.put(numRemito, r);
                porBolsin.computeIfAbsent(numBolsin, k -> new ArrayList<>()).add(r);
            }
        }
        return porBolsin;
    }

    private void cargarDetallesRemito(Connection conn,
                                      Map<Integer, Remito> remitosPorNumero,
                                      Map<Integer, Documentacion> docs) throws SQLException {
        String sql = "SELECT remito_numero, documentacion_numero, area_cmc_destino "
                + "FROM detalle_remito ORDER BY remito_numero, documentacion_numero";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Remito remito = remitosPorNumero.get(rs.getInt("remito_numero"));
                Documentacion doc = docs.get(rs.getInt("documentacion_numero"));
                if (remito != null && doc != null) {
                    remito.agregarDetalle(new DetalleRemito(doc, rs.getString("area_cmc_destino")));
                }
            }
        }
    }

    private void cargarCambiosEstadoBolsin(Connection conn,
                                           Map<Integer, Bolsin> bolsines,
                                           Map<String, Estado> estados,
                                           Map<String, Empleado> empleados) throws SQLException {
        String sql = "SELECT bolsin_numero, fecha_hora_inicio, fecha_hora_fin, estado_nombre, estado_ambito, empleado_nombre "
                + "FROM cambio_estado_bolsin ORDER BY bolsin_numero, fecha_hora_inicio ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Bolsin b = bolsines.get(rs.getInt("bolsin_numero"));
                if (b == null) {
                    continue;
                }
                Estado estado = estados.get(claveEstado(rs.getString("estado_nombre"), rs.getString("estado_ambito")));
                String empNombre = rs.getString("empleado_nombre");
                Empleado emp = empNombre != null ? empleados.get(empNombre) : null;
                CambioEstadoBolsin cambio = new CambioEstadoBolsin(
                        aLocalDateTime(rs.getTimestamp("fecha_hora_inicio")),
                        estado,
                        emp
                );
                cambio.setFechaHoraFin(aLocalDateTime(rs.getTimestamp("fecha_hora_fin")));
                b.getCambiosDeEstado().add(cambio);
            }
        }
    }

    // =========================================================================
    // Persistencia Transaccional Dominio Puro -> Tablas SQL
    // =========================================================================

    private void upsertEmpleadoYComision(Connection conn, Empleado emp) throws SQLException {
        ComisionMedica cm = emp.getCM();
        if (cm != null) {
            String sqlCm = "MERGE INTO comision_medica (codigo, nombre, direccion, email, telefono) KEY (codigo) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlCm)) {
                ps.setInt(1, cm.getCodigo());
                ps.setString(2, cm.getNombre());
                ps.setString(3, cm.getDireccion());
                ps.setString(4, cm.getEmail());
                ps.setString(5, cm.getTelefono());
                ps.executeUpdate();
            }
        }
        String sqlEmp = "MERGE INTO empleado (nombre, comision_medica_codigo) KEY (nombre) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlEmp)) {
            ps.setString(1, emp.getNombre());
            ps.setInt(2, cm != null ? cm.getCodigo() : 0);
            ps.executeUpdate();
        }
    }

    private void upsertCambioEstadoBolsin(Connection conn, int numeroBolsin, CambioEstadoBolsin cambio) throws SQLException {
        String sql = """
                MERGE INTO cambio_estado_bolsin
                    (bolsin_numero, fecha_hora_inicio, fecha_hora_fin, estado_nombre, estado_ambito, empleado_nombre)
                KEY (bolsin_numero, fecha_hora_inicio)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numeroBolsin);
            ps.setTimestamp(2, aTimestamp(cambio.getFechaHoraInicio()));
            setNullableTimestamp(ps, 3, cambio.getFechaHoraFin());
            ps.setString(4, cambio.getEstado().getNombre());
            ps.setString(5, cambio.getEstado().getAmbito());
            setNullableString(ps, 6, cambio.getEmpleado() != null ? cambio.getEmpleado().getNombre() : null);
            ps.executeUpdate();
        }
    }

    private void actualizarEstadoRemito(Connection conn, Remito remito) throws SQLException {
        String sql = "UPDATE remito SET estado_nombre = ?, estado_ambito = ? WHERE numero = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            Estado est = remito.getEstado();
            setNullableString(ps, 1, est != null ? est.getNombre() : null);
            setNullableString(ps, 2, est != null ? est.getAmbito() : null);
            ps.setInt(3, remito.getNumero());
            ps.executeUpdate();
        }
    }

    private void upsertCambioEstadoDocumentacion(Connection conn, int numeroDoc, CambioEstadoDocumentacion cambio) throws SQLException {
        String sql = """
                MERGE INTO cambio_estado_documentacion
                    (documentacion_numero, fecha_hora_inicio, fecha_hora_fin, estado_nombre, estado_ambito, empleado_nombre)
                KEY (documentacion_numero, fecha_hora_inicio)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numeroDoc);
            ps.setTimestamp(2, aTimestamp(cambio.getFechaHoraInicio()));
            setNullableTimestamp(ps, 3, cambio.getFechaHoraFin());
            ps.setString(4, cambio.getEstado().getNombre());
            ps.setString(5, cambio.getEstado().getAmbito());
            setNullableString(ps, 6, cambio.getEmpleado() != null ? cambio.getEmpleado().getNombre() : null);
            ps.executeUpdate();
        }
    }

    private long contarRegistros(Connection conn, String tabla) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM " + tabla);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private static String claveEstado(String nombre, String ambito) {
        return nombre + "|" + ambito;
    }

    private static Timestamp aTimestamp(LocalDateTime ldt) {
        return Timestamp.valueOf(ldt.truncatedTo(ChronoUnit.MICROS));
    }

    private static LocalDateTime aLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime().truncatedTo(ChronoUnit.MICROS) : null;
    }

    private static void setNullableTimestamp(PreparedStatement ps, int index, LocalDateTime ldt) throws SQLException {
        if (ldt != null) {
            ps.setTimestamp(index, aTimestamp(ldt));
        } else {
            ps.setNull(index, Types.TIMESTAMP);
        }
    }

    private static void setNullableString(PreparedStatement ps, int index, String val) throws SQLException {
        if (val != null) {
            ps.setString(index, val);
        } else {
            ps.setNull(index, Types.VARCHAR);
        }
    }
}
