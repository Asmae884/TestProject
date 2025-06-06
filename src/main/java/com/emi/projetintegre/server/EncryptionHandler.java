package com.emi.projetintegre.server;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Base64;

public class EncryptionHandler {
    private final Connection dbConnection;
    private final ObjectOutputStream output;
    private final FileSecurity fileSecurity;
    private final int userId;
    private final UserHandler userHandler;

    public EncryptionHandler(Connection dbConnection, ObjectOutputStream output, FileSecurity fileSecurity, int userId, UserHandler userHandler) {
        this.dbConnection = dbConnection;
        this.output = output;
        this.fileSecurity = fileSecurity;
        this.userId = userId;
        this.userHandler = userHandler;
    }

    public void handleEncryptFile(int docId) throws IOException {
        try {
            String sql = "SELECT file_name, encryption_key FROM PersonalDocuments WHERE docID = ? AND owner_id = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, docId);
                stmt.setInt(2, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String fileName = rs.getString("file_name");
                        String encryptionKey = rs.getString("encryption_key");
                        if (encryptionKey != null && !encryptionKey.isEmpty()) {
                            sendError("FILE_ALREADY_ENCRYPTED");
                            return;
                        }
                        Path userDir = userHandler.getUserDirectory();
                        Path filePath = userDir.resolve(fileName);
                        if (!Files.exists(filePath)) {
                            sendError("FILE_NOT_FOUND");
                            return;
                        }
                        SecretKey secretKey = fileSecurity.hasKey() ? fileSecurity.getCurrentKey() : fileSecurity.generateKey();
                        Path tempFile = Files.createTempFile(userDir, "encrypted_", ".tmp");
                        try {
                            fileSecurity.encryptFile(filePath, tempFile);
                            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
                            String keyString = fileSecurity.getKeyAsString();
                            String updateSql = "UPDATE PersonalDocuments SET encryption_key = ? WHERE docID = ? AND owner_id = ?";
                            try (PreparedStatement updateStmt = dbConnection.prepareStatement(updateSql)) {
                                updateStmt.setString(1, keyString);
                                updateStmt.setInt(2, docId);
                                updateStmt.setInt(3, userId);
                                updateStmt.executeUpdate();
                            }
                            sendResponse("ENCRYPT_SUCCESS");
                        } catch (Exception e) {
                            Files.deleteIfExists(tempFile);
                            sendError("ENCRYPTION_ERROR: " + e.getMessage());
                        }
                    } else {
                        sendError("DOCUMENT_NOT_FOUND");
                    }
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
        }
    }

    public void handleDecryptFile(int docId) throws IOException {
        try {
            String sql = "SELECT file_name, encryption_key FROM PersonalDocuments WHERE docID = ? AND owner_id = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, docId);
                stmt.setInt(2, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String fileName = rs.getString("file_name");
                        String encryptionKey = rs.getString("encryption_key");
                        if (encryptionKey == null || encryptionKey.isEmpty()) {
                            sendError("FILE_NOT_ENCRYPTED");
                            return;
                        }
                        Path userDir = userHandler.getUserDirectory();
                        Path filePath = userDir.resolve(fileName);
                        if (!Files.exists(filePath)) {
                            sendError("FILE_NOT_FOUND");
                            return;
                        }
                        byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
                        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
                        setFileSecurityKey(secretKey);
                        Path tempFile = Files.createTempFile(userDir, "decrypted_", ".tmp");
                        try {
                            fileSecurity.decryptFile(filePath, tempFile);
                            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
                            String updateSql = "UPDATE PersonalDocuments SET encryption_key = NULL WHERE docID = ? AND owner_id = ?";
                            try (PreparedStatement updateStmt = dbConnection.prepareStatement(updateSql)) {
                                updateStmt.setInt(1, docId);
                                updateStmt.setInt(2, userId);
                                updateStmt.executeUpdate();
                            }
                            sendResponse("DECRYPT_SUCCESS");
                        } catch (Exception e) {
                            Files.deleteIfExists(tempFile);
                            sendError("DECRYPTION_ERROR: " + e.getMessage());
                        }
                    } else {
                        sendError("DOCUMENT_NOT_FOUND");
                    }
                }
            }
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
        }
    }

    private void setFileSecurityKey(SecretKey key) {
        try {
            java.lang.reflect.Field field = FileSecurity.class.getDeclaredField("currentKey");
            field.setAccessible(true);
            field.set(fileSecurity, key);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("Error setting FileSecurity key: " + e.getMessage());
        }
    }

    public String calculateFileHash(Path filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(filePath);
        byte[] hashBytes = digest.digest(fileBytes);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private void sendResponse(String response) throws IOException {
        output.writeObject(response);
        output.flush();
    }

    private void sendError(String error) throws IOException {
        sendResponse("ERROR: " + error);
    }
}