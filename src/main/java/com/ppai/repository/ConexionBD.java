package com.ppai.repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Administra conexiones JDBC hacia la base de datos H2 persistente en disco
 * e inicializa de forma idempotente el esquema SQL al primer uso.
 */
public final class ConexionBD {

    public static final String URL_POR_DEFECTO = "jdbc:h2:file:./data/ppaidb;AUTO_SERVER=TRUE";
    private static final String USUARIO = "sa";
    private static final String CONTRASENA = "";

    private static String urlActual = URL_POR_DEFECTO;
    private static boolean esquemaInicializado = false;

    private ConexionBD() {
    }

    public static synchronized void configurarUrl(String nuevaUrl) {
        urlActual = (nuevaUrl != null && !nuevaUrl.isBlank()) ? nuevaUrl : URL_POR_DEFECTO;
        esquemaInicializado = false;
    }

    public static synchronized void restaurarUrlPorDefecto() {
        urlActual = URL_POR_DEFECTO;
        esquemaInicializado = false;
    }

    public static synchronized String getUrlActual() {
        return urlActual;
    }

    public static synchronized Connection abrirConexion() throws SQLException {
        Connection conn = DriverManager.getConnection(urlActual, USUARIO, CONTRASENA);
        if (!esquemaInicializado) {
            ejecutarSchema(conn);
            esquemaInicializado = true;
        }
        return conn;
    }

    public static synchronized void cerrar() {
        esquemaInicializado = false;
    }

    private static void ejecutarSchema(Connection conn) throws SQLException {
        String sqlCompleto = leerRecursoSchema();
        String[] sentencias = sqlCompleto.split(";");
        try (Statement st = conn.createStatement()) {
            for (String raw : sentencias) {
                String ddl = raw.trim();
                if (!ddl.isEmpty()) {
                    st.execute(ddl);
                }
            }
        }
    }

    private static String leerRecursoSchema() throws SQLException {
        try (InputStream is = ConexionBD.class.getResourceAsStream("/schema.sql")) {
            if (is == null) {
                throw new SQLException("No se encontró /schema.sql en el classpath");
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    sb.append(linea).append('\n');
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new SQLException("Error leyendo /schema.sql", e);
        }
    }
}
