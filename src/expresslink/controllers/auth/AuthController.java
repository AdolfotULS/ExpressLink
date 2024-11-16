package expresslink.controllers.auth;

import expresslink.model.*;
import expresslink.model.enums.*;
import expresslink.utils.DatabaseConnection;
import java.sql.*;

public class AuthController {
    public Usuario inicioSesion(String email, String password) throws SQLException {
        String query = "SELECT id, nombre, email, telefono, rol FROM usuario WHERE email = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            stmt.setString(2, password); // En producción usar hash

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Usuario(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("email"),
                            rs.getString("telefono"),
                            TipoUsuario.valueOf(rs.getString("rol")));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new SQLException("Error durante el login", e);
        }
    }

    public boolean registrarUsuario(String nombre, String email, String password, String telefono, TipoUsuario rol)
            throws SQLException {
        Connection conn = null;
        PreparedStatement checkStmt = null;
        PreparedStatement insertStmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Iniciamos una transacción

            // Primero verificamos si el email existe
            String checkQuery = "SELECT COUNT(*) FROM usuario WHERE email = ?";
            checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, email);
            rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                throw new SQLException("El email ya está registrado");
            }

            // Si no existe, procedemos con el registro
            String insertQuery = "INSERT INTO usuario (nombre, email, password, telefono, rol) VALUES (?, ?, ?, ?, ?)";
            insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, nombre);
            insertStmt.setString(2, email);
            insertStmt.setString(3, password);
            insertStmt.setString(4, telefono);
            insertStmt.setString(5, rol.toString());

            int filasAfectadas = insertStmt.executeUpdate();

            conn.commit(); // Confirmamos la transacción
            return filasAfectadas > 0;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Si hay error, revertimos la transacción
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw new SQLException("Error durante el registro: " + e.getMessage(), e);
        } finally {
            // Cerramos todos los recursos en orden inverso
            if (rs != null)
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            if (checkStmt != null)
                try {
                    checkStmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            if (insertStmt != null)
                try {
                    insertStmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            if (conn != null)
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
        }
    }

    public boolean validarCredenciales(String email, String password) {
        try {
            return inicioSesion(email, password) != null;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void cerrarSesion() {
        DatabaseConnection.closeConnection();
    }
}