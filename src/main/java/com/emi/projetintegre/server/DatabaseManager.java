package com.emi.projetintegre.server;

import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.UUID;

public class DatabaseManager {
    private Connection conn;

    public DatabaseManager(String url, String user, String password) throws SQLException {
        conn = DriverManager.getConnection(url, user, password);
    }

    // Hash password using SHA-256
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = md.digest(password.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Generate and insert user
    public String addUser(String username, String password, int validityMonths, boolean isAdmin) throws Exception {
        String secretKey = UUID.randomUUID().toString();
        String hashedPassword = hashPassword(password);
        LocalDate creationDate = LocalDate.now();
        LocalDate validityDate = creationDate.plusMonths(validityMonths);

        String sql = "INSERT INTO Users (login, hashed_password, is_admin, creation_date, validity_date, secret_key) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setBoolean(3, isAdmin);
            stmt.setDate(4, Date.valueOf(creationDate));
            stmt.setDate(5, Date.valueOf(validityDate));
            stmt.setString(6, secretKey);

            stmt.executeUpdate();
            System.out.println("User '" + username + "' added.");
            return secretKey;
        } catch (SQLException e) {
            throw new Exception("Error adding user: " + e.getMessage());
        }
    }

    // Display all users
    public void showUsers() {
        String sql = "SELECT userID, login, creation_date, validity_date FROM Users";
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("== Users ==");
            while (rs.next()) {
                System.out.printf("ID: %d | Login: %s | Created: %s | Valid Until: %s%n",
                        rs.getInt("userID"),
                        rs.getString("login"),
                        rs.getDate("creation_date"),
                        rs.getDate("validity_date"));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching users: " + e.getMessage());
        }
    }

    // Delete user by ID
    public boolean deleteUser(int userID) {
        String select = "SELECT login FROM Users WHERE userID = ?";
        String delete = "DELETE FROM Users WHERE userID = ?";

        try (PreparedStatement selStmt = conn.prepareStatement(select)) {
            selStmt.setInt(1, userID);
            ResultSet rs = selStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("User with ID " + userID + " not found.");
                return false;
            }

            String username = rs.getString("login");

            try (PreparedStatement delStmt = conn.prepareStatement(delete)) {
                delStmt.setInt(1, userID);
                delStmt.executeUpdate();
                System.out.println("User deleted: ID " + userID + " (" + username + ")");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            return false;
        }
    }
}
