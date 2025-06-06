package com.emi.projetintegre.server;

import java.sql.*;
import java.security.*;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;
import java.io.*;

public class DatabaseManager {
    private Connection conn;
    private static final String PRIVATE_KEY_FILE = "PrivateKeys.txt";

    public DatabaseManager(String url, String user, String password) throws SQLException {
        if (url == null || user == null || password == null) {
            throw new IllegalArgumentException("Database connection parameters cannot be null");
        }
        conn = DriverManager.getConnection(url, user, password);
    }

    // Hash password using SHA-256
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = md.digest(password.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Generate RSA key pair and return both keys as Base64 strings
    private String[] generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        String publicKeyStr = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        String privateKeyStr = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        return new String[]{publicKeyStr, privateKeyStr};
    }

    // Write private key to file with userID identifier
    private void writePrivateKeyToFile(int userId, String privateKey) throws IOException {
        try (FileWriter fw = new FileWriter(PRIVATE_KEY_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println("UserID: " + userId);
            out.println("PrivateKey: " + privateKey);
            out.println("---");
        }
    }

    // Add user with RSA key pair
    public void addUser(String username, String password, int validityMonths, boolean isAdmin) throws Exception {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (validityMonths < 0) {
            throw new IllegalArgumentException("Validity months cannot be negative");
        }

        String hashedPassword = hashPassword(password);
        LocalDate creationDate = LocalDate.now();
        LocalDate validityDate = creationDate.plusMonths(validityMonths);
        String[] keys = generateRSAKeyPair();
        String publicKey = keys[0];
        String privateKey = keys[1];

        // Insert into Users table
        String userSql = "INSERT INTO Users (login, hashed_password, is_admin, creation_date, validity_date) " +
                        "VALUES (?, ?, ?, ?, ?)";
        int userId;

        try (PreparedStatement stmt = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setBoolean(3, isAdmin);
            stmt.setDate(4, Date.valueOf(creationDate));
            stmt.setDate(5, Date.valueOf(validityDate));

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    userId = rs.getInt(1);
                } else {
                    throw new SQLException("Failed to retrieve userID.");
                }
            }
        } catch (SQLException e) {
            throw new Exception("Error adding user '" + username + "': " + e.getMessage(), e);
        }

        // Insert public key into rsa_keys table
        String rsaSql = "INSERT INTO rsa_keys (ownerID, public_key) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(rsaSql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, publicKey);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new Exception("Error adding public key for userID " + userId + ": " + e.getMessage(), e);
        }

        // Write private key to file
        try {
            writePrivateKeyToFile(userId, privateKey);
        } catch (IOException e) {
            throw new Exception("Error writing private key for userID " + userId + ": " + e.getMessage(), e);
        }

        System.out.println("User '" + username + "' added with RSA key pair.");
    }

    // Change user password by ID
    public boolean changePassword(int userId, String newPassword) throws Exception {
        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("New password cannot be null or empty");
        }
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid userID");
        }

        String selectSql = "SELECT login FROM Users WHERE userID = ?";
        String updateSql = "UPDATE Users SET hashed_password = ? WHERE userID = ?";

        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setInt(1, userId);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("User with ID " + userId + " not found.");
                    return false;
                }
            }

            String hashedPassword = hashPassword(newPassword);
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, hashedPassword);
                updateStmt.setInt(2, userId);
                int rowsAffected = updateStmt.executeUpdate();

                if (rowsAffected > 0) {
                    System.out.println("Password updated successfully for userID: " + userId);
                    return true;
                } else {
                    System.out.println("Failed to update password for userID: " + userId);
                    return false;
                }
            }
        } catch (SQLException e) {
            throw new Exception("Error updating password for userID " + userId + ": " + e.getMessage(), e);
        }
    }

    // Populate rsa_keys table for existing users and write private keys to file
    public void populateRSAKeys() throws Exception {
        String selectSql = "SELECT userID FROM Users WHERE userID NOT IN (SELECT ownerID FROM rsa_keys)";
        String insertSql = "INSERT INTO rsa_keys (ownerID, public_key) VALUES (?, ?)";

        try (Statement selectStmt = conn.createStatement();
             ResultSet rs = selectStmt.executeQuery(selectSql)) {

            while (rs.next()) {
                int userId = rs.getInt("userID");
                String[] keys = generateRSAKeyPair();
                String publicKey = keys[0];
                String privateKey = keys[1];

                // Insert public key into rsa_keys
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, userId);
                    insertStmt.setString(2, publicKey);
                    insertStmt.executeUpdate();
                    System.out.println("Added RSA public key for userID: " + userId);
                }

                // Write private key to file
                try {
                    writePrivateKeyToFile(userId, privateKey);
                } catch (IOException e) {
                    throw new Exception("Error writing private key to file for userID " + userId + ": " + e.getMessage(), e);
                }
            }
            System.out.println("Finished populating RSA keys for existing users.");
        } catch (SQLException e) {
            throw new Exception("Error populating RSA keys: " + e.getMessage(), e);
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
        if (userID <= 0) {
            throw new IllegalArgumentException("Invalid userID");
        }

        String select = "SELECT login FROM Users WHERE userID = ?";
        String delete = "DELETE FROM Users WHERE userID = ?";

        try (PreparedStatement selStmt = conn.prepareStatement(select)) {
            selStmt.setInt(1, userID);
            try (ResultSet rs = selStmt.executeQuery()) {
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
            }
        } catch (SQLException e) {
            System.err.println("Error deleting user with ID " + userID + ": " + e.getMessage());
            return false;
        }
    }
}