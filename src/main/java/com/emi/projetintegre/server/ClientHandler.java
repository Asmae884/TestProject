package com.emi.projetintegre.server;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.emi.projetintegre.models.PersonalDocument;

import java.util.Base64;

public class ClientHandler implements Runnable {
    private static final String UPLOAD_DIR = "/mnt/Shared_Folder/UserFiles";
    private static final int MAX_AUTH_ATTEMPTS = 3;
    private static final int BUFFER_SIZE = 4096;

    private final Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private Connection dbConnection;
    private int userId = -1;
    private String username;
    private boolean isAuthenticated = false;
    private int authAttempts = 0;
    private FileSecurity fileSecurity;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.fileSecurity = new FileSecurity();
    }

    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());

            connectToDatabase();
            if (dbConnection == null || dbConnection.isClosed()) {
                sendError("Database connection failed");
                return;
            }

            processClientCommands();

        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanupResources();
        }
    }

    private void connectToDatabase() {
        String url = "jdbc:mysql://localhost:3306/SecureCommDB";
        String dbUsername = "secureapp";
        String dbPassword = "THISisFUNNY&&5627";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            dbConnection = DriverManager.getConnection(url, dbUsername, dbPassword);
            System.out.println("Database connection established for client: " + socket.getInetAddress());
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found!");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }


    private void processClientCommands() throws IOException, ClassNotFoundException {
        while (!socket.isClosed()) {
            Object commandObj = input.readObject();
            System.out.println("Received command from client " + socket.getInetAddress() + ": " + commandObj);
            if (!(commandObj instanceof String)) {
                sendError("INVALID_COMMAND");
                continue;
            }

            String command = (String) commandObj;
            if (command.startsWith("GET_DOCUMENT_CONTENT:")) {
                if (!isAuthenticated) {
                    sendError("NOT_AUTHENTICATED");
                    continue;
                }
                String docIdStr = command.substring("GET_DOCUMENT_CONTENT:".length());
                try {
                    int docId = Integer.parseInt(docIdStr);
                    handleGetDocumentContent(docId);
                } catch (NumberFormatException e) {
                    sendError("INVALID_DOCUMENT_ID");
                }
                continue;
            } else if (command.startsWith("DECRYPT_FILE:")) {
                if (!isAuthenticated) {
                    sendError("NOT_AUTHENTICATED");
                    continue;
                }
                String docIdStr = command.substring("DECRYPT_FILE:".length());
                try {
                    int docId = Integer.parseInt(docIdStr);
                    handleDecryptFile(docId);
                } catch (NumberFormatException e) {
                    sendError("INVALID_DOCUMENT_ID");
                }
                continue;
            } else if (command.startsWith("ENCRYPT_FILE:")) {
                if (!isAuthenticated) {
                    sendError("NOT_AUTHENTICATED");
                    continue;
                }
                String docIdStr = command.substring("ENCRYPT_FILE:".length());
                try {
                    int docId = Integer.parseInt(docIdStr);
                    handleEncryptFile(docId);
                } catch (NumberFormatException e) {
                    sendError("INVALID_DOCUMENT_ID");
                }
                continue;
            } else if (command.startsWith("GET_ENCRYPTION_KEY:")) {
                if (!isAuthenticated) {
                    sendError("NOT_AUTHENTICATED");
                    continue;
                }
                String docIdStr = command.substring("GET_ENCRYPTION_KEY:".length());
                try {
                    int docId = Integer.parseInt(docIdStr);
                    handleGetEncryptionKey(docId);
                } catch (NumberFormatException e) {
                    sendError("INVALID_DOCUMENT_ID");
                }
                continue;
            } else if (command.startsWith("SHARE_DOCUMENT:")) {
                if (!isAuthenticated) {
                    sendError("NOT_AUTHENTICATED");
                    continue;
                }
                String[] parts = command.split(":");
                if (parts.length != 4) {
                    sendError("INVALID_SHARE_COMMAND");
                    continue;
                }
                try {
                    int docId = Integer.parseInt(parts[1]);
                    int userId = Integer.parseInt(parts[2]);
                    int accessId = Integer.parseInt(parts[3]);
                    handleShareDocument(docId, userId, accessId);
                } catch (NumberFormatException e) {
                    sendError("INVALID_SHARE_PARAMETERS");
                }
                continue;
            } else if (command.startsWith("GET_DOCUMENT_PERMISSIONS:")) {
                if (!isAuthenticated) {
                    sendError("NOT_AUTHENTICATED");
                    continue;
                }
                String docIdStr = command.substring("GET_DOCUMENT_PERMISSIONS:".length());
                try {
                    int docId = Integer.parseInt(docIdStr);
                    handleGetDocumentPermissions(docId);
                } catch (NumberFormatException e) {
                    sendError("INVALID_DOCUMENT_ID");
                }
                continue;
            } else if (command.startsWith("REMOVE_SHARE:")) {
                if (!isAuthenticated) {
                    sendError("NOT_AUTHENTICATED");
                    continue;
                }
                String[] parts = command.split(":");
                if (parts.length != 3) {
                    sendError("INVALID_REMOVE_SHARE_COMMAND");
                    continue;
                }
                try {
                    int docId = Integer.parseInt(parts[1]);
                    int userIdToRemove = Integer.parseInt(parts[2]);
                    handleRemoveShare(docId, userIdToRemove);
                } catch (NumberFormatException e) {
                    sendError("INVALID_REMOVE_SHARE_PARAMETERS");
                }
                continue;
            } else if (command.startsWith("LIST_USERS_SHARE:")) {
                if (!isAuthenticated) {
                    sendError("NOT_AUTHENTICATED");
                    continue;
                }
                String docIdStr = command.substring("LIST_USERS_SHARE:".length());
                try {
                    int docId = Integer.parseInt(docIdStr);
                    handleListUsersShare(docId);
                } catch (NumberFormatException e) {
                    sendError("INVALID_DOCUMENT_ID");
                }
                continue;
            } else if (command.startsWith("DELETE_DOCUMENT:")) {
                if (!isAuthenticated) {
                    sendError("NOT_AUTHENTICATED");
                    continue;
                }
                String docIdStr = command.substring("DELETE_DOCUMENT:".length());
                try {
                    int docId = Integer.parseInt(docIdStr);
                    handleDeleteDocument(docId);
                } catch (NumberFormatException e) {
                    sendError("INVALID_DOCUMENT_ID");
                }
                continue;
            } else if (command.equals("RESTORE_DOCUMENTS")) {
                if (!isAuthenticated) {
                    sendError("NOT_AUTHENTICATED");
                    continue;
                }
                try {
                    @SuppressWarnings("unchecked")
                    List<Integer> docIds = (List<Integer>) input.readObject();
                    handleRestoreDocuments(docIds);
                } catch (ClassNotFoundException e) {
                    sendError("INVALID_DATA: " + e.getMessage());
                }
                continue;
            } else if (command.equals("PERMANENT_DELETE_DOCUMENTS")) {
                if (!isAuthenticated) {
                    sendError("NOT_AUTHENTICATED");
                    continue;
                }
                try {
                    @SuppressWarnings("unchecked")
                    List<Integer> docIds = (List<Integer>) input.readObject();
                    handlePermanentDeleteDocuments(docIds);
                } catch (ClassNotFoundException e) {
                    sendError("INVALID_DATA: " + e.getMessage());
                }
                continue;
            } else if (command.startsWith("SAVE_DOCUMENT_CONTENT:")) {
                if (!isAuthenticated) {
                    sendError("NOT_AUTHENTICATED");
                    continue;
                }
                String docIdStr = command.substring("SAVE_DOCUMENT_CONTENT:".length());
                try {
                    int docId = Integer.parseInt(docIdStr);
                    handleSaveDocumentContent(docId);
                } catch (NumberFormatException e) {
                    sendError("INVALID_DOCUMENT_ID");
                }
                continue;
            } else if (command.startsWith("UPDATE_DOCUMENT_CONTENT:")) {
                if (!isAuthenticated) {
                    sendError("NOT_AUTHENTICATED");
                    continue;
                }
                String docIdStr = command.substring("UPDATE_DOCUMENT_CONTENT:".length());
                try {
                    int docId = Integer.parseInt(docIdStr);
                    handleUpdateDocumentContent(docId);
                } catch (NumberFormatException e) {
                    sendError("INVALID_DOCUMENT_ID");
                }
                continue;
            }

            switch (command) {
                case "AUTHENTICATE":
                    handleAuthenticate();
                    break;
                case "CHECK_DUPLICATE_FILE":
                    if (!isAuthenticated) {
                        sendError("NOT_AUTHENTICATED");
                        break;
                    }
                    handleCheckDuplicateFile();
                    break;
                case "UPLOAD_FILE":
                    if (!isAuthenticated) {
                        sendError("NOT_AUTHENTICATED");
                        break;
                    }
                    handleFileUpload();
                    break;
                case "UPLOAD_FILE_WITH_METADATA":
                    if (!isAuthenticated) {
                        sendError("NOT_AUTHENTICATED");
                        break;
                    }
                    handleFileUploadWithMetadata();
                    break;
                case "UPLOAD_ENCRYPTED_FILE_WITH_METADATA":
                    if (!isAuthenticated) {
                        sendError("NOT_AUTHENTICATED");
                        break;
                    }
                    handleEncryptedFileUploadWithMetadata();
                    break;
                case "LIST_DOCUMENTS":
                    if (!isAuthenticated) {
                        sendError("NOT_AUTHENTICATED");
                        break;
                    }
                    String searchTerm = input.readUTF();
                    if (searchTerm.isEmpty()) {
                        searchTerm = null;
                    }
                    handleListDocuments(searchTerm);
                    break;
                case "LIST_SHARED_WITH_ME_DOCUMENTS":
                    if (!isAuthenticated) {
                        sendError("NOT_AUTHENTICATED");
                        break;
                    }
                    String searchedTerm = input.readUTF();
                    if (searchedTerm.isEmpty()) {
                        searchedTerm = null;
                    }
                    handleListSharedWithMeDocuments(searchedTerm);
                    break;
                case "LIST_USERS":
                    if (!isAuthenticated) {
                        sendError("NOT_AUTHENTICATED");
                        break;
                    }
                    handleListUsers();
                    break;
                case "DOWNLOAD_DOCUMENT":
                    if (!isAuthenticated) {
                        sendError("NOT_AUTHENTICATED");
                        break;
                    }
                    handleDocumentDownload();
                    break;
                case "FILE_LOG":
                    if (!isAuthenticated) {
                        sendError("NOT_AUTHENTICATED");
                        break;
                    }
                    PersonalDocument document = (PersonalDocument) input.readObject();
                    Path userDir = getUserDirectory();
                    Path filePath = userDir.resolve(document.getFileName());
                    storeFileInDatabase(document, filePath);
                    break;
                case "LIST_DELETED_DOCUMENTS":
                    if (!isAuthenticated) {
                        sendError("NOT_AUTHENTICATED");
                        break;
                    }
                    String deletedSearchTerm = input.readUTF();
                    if (deletedSearchTerm.isEmpty()) {
                        deletedSearchTerm = null;
                    }
                    handleListDeletedDocuments(deletedSearchTerm);
                    break;
                case "DISCONNECT":
                    System.out.println("Client " + userId + " requested disconnect");
                    return;
                default:
                    sendError("UNKNOWN_COMMAND");
            }
        }
    }

    private void handleRemoveShare(int docId, int userIdToRemove) throws IOException {
        if (userId == -1) {
            sendError("INVALID_USER_ID");
            return;
        }

        if (dbConnection == null || isConnectionClosed()) {
            sendError("DATABASE_CONNECTION_ERROR");
            return;
        }

        try {
            // Validate document exists and belongs to the current user
            String docCheckSql = "SELECT COUNT(*) FROM PersonalDocuments WHERE docID = ? AND owner_id = ?";
            try (PreparedStatement docStmt = dbConnection.prepareStatement(docCheckSql)) {
                docStmt.setInt(1, docId);
                docStmt.setInt(2, userId);
                try (ResultSet rs = docStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        sendError("DOCUMENT_NOT_FOUND");
                        return;
                    }
                }
            }

            // Validate userIdToRemove exists in Users table
            String userCheckSql = "SELECT COUNT(*) FROM Users WHERE userID = ?";
            try (PreparedStatement userStmt = dbConnection.prepareStatement(userCheckSql)) {
                userStmt.setInt(1, userIdToRemove);
                try (ResultSet rs = userStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        sendError("INVALID_USER_ID");
                        return;
                    }
                }
            }

            // Delete the permission from UserPermissions table
            String sql = "DELETE FROM UserPermissions WHERE docID = ? AND userID = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, docId);
                stmt.setInt(2, userIdToRemove);
                int rowsAffected = stmt.executeUpdate();
                System.out.println("Removed share: docID=" + docId + ", userID=" + userIdToRemove + ", rows affected: " + rowsAffected);
                if (rowsAffected > 0) {
                    sendResponse("REMOVE_SUCCESS");
                } else {
                    sendError("NO_PERMISSION_FOUND");
                }
            }
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleSaveDocumentContent(int docId) throws IOException, ClassNotFoundException {
        if (!isAuthenticated) {
            sendError("NOT_AUTHENTICATED");
            return;
        }

        if (userId == -1) {
            sendError("INVALID_USER_ID");
            return;
        }

        if (dbConnection == null || isConnectionClosed()) {
            sendError("DATABASE_CONNECTION_ERROR");
            return;
        }

        // Read content from client
        Object contentObj = input.readObject();
        if (!(contentObj instanceof String)) {
            sendError("INVALID_CONTENT_TYPE");
            return;
        }
        String content = (String) contentObj;

        try {
            // Fetch document details
            String sql = "SELECT file_name, file_type, encryption_key, is_encrypted, owner_id, file_path " +
                         "FROM PersonalDocuments WHERE docID = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, docId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        sendError("DOCUMENT_NOT_FOUND");
                        return;
                    }

                    String fileName = rs.getString("file_name");
                    String fileType = rs.getString("file_type");
                    String encryptionKey = rs.getString("encryption_key");
                    boolean isEncrypted = rs.getInt("is_encrypted") == 1;
                    String filePath = rs.getString("file_path");
                    boolean hasWriteAccess = rs.getInt("owner_id") == userId;

                    // Check write permissions for non-owners
                    if (!hasWriteAccess) {
                        String permissionSql = "SELECT accessID FROM UserPermissions WHERE docID = ? AND userID = ?";
                        try (PreparedStatement permStmt = dbConnection.prepareStatement(permissionSql)) {
                            permStmt.setInt(1, docId);
                            permStmt.setInt(2, userId);
                            try (ResultSet permRs = permStmt.executeQuery()) {
                                if (permRs.next()) {
                                    int accessId = permRs.getInt("accessID");
                                    boolean[] flags = getPermissionFlags(accessId);
                                    hasWriteAccess = flags[1]; // Check write permission
                                }
                            }
                        }
                    }

                    if (!hasWriteAccess) {
                        sendError("NO_WRITE_PERMISSION");
                        return;
                    }

                    if (!fileType.equalsIgnoreCase("txt")) {
                        sendError("UNSUPPORTED_FILE_TYPE");
                        return;
                    }

                    Path path = Paths.get(UPLOAD_DIR, filePath != null ? filePath : getUsername(), fileName);
                    if (!Files.exists(path.getParent())) {
                        Files.createDirectories(path.getParent());
                    }

                    // Handle file writing
                    if (isEncrypted) {
                        if (encryptionKey == null || encryptionKey.isEmpty()) {
                            sendError("ENCRYPTION_KEY_NOT_FOUND");
                            return;
                        }
                        try {
                            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
                            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                                sendError("INVALID_KEY_LENGTH");
                                return;
                            }
                            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
                            fileSecurity.setKey(secretKey);

                            Path tempFile = Files.createTempFile(path.getParent(), "encrypted_", ".tmp");
                            try {
                                // Write plain text to temp file
                                Files.writeString(tempFile, content, StandardCharsets.UTF_8);
                                // Encrypt temp file to target path
                                fileSecurity.encryptFile(tempFile, path);
                            } finally {
                                Files.deleteIfExists(tempFile);
                            }
                        } catch (IllegalArgumentException | InvalidKeyException e) {
                            sendError("INVALID_ENCRYPTION_KEY: " + e.getMessage());
                            return;
                        } catch (Exception e) {
                            sendError("ENCRYPTION_ERROR: " + e.getMessage());
                            return;
                        }
                    } else {
                        Files.writeString(path, content, StandardCharsets.UTF_8);
                    }

                    // Update modify_date and file_size in database
                    String updateSql = "UPDATE PersonalDocuments SET modify_date = ?, file_size = ? WHERE docID = ?";
                    try (PreparedStatement updateStmt = dbConnection.prepareStatement(updateSql)) {
                        updateStmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                        updateStmt.setLong(2, content.getBytes(StandardCharsets.UTF_8).length);
                        updateStmt.setInt(3, docId);
                        updateStmt.executeUpdate();
                    }

                    sendResponse("SAVE_SUCCESS");
                    System.out.println("Successfully saved content for docID: " + docId);
                }
            }
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            sendError("FILE_SYSTEM_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleUpdateDocumentContent(int docId) throws IOException, ClassNotFoundException {
        if (!isAuthenticated) {
            sendError("NOT_AUTHENTICATED");
            return;
        }

        if (userId == -1) {
            sendError("INVALID_USER_ID");
            return;
        }

        if (dbConnection == null || isConnectionClosed()) {
            sendError("DATABASE_CONNECTION_ERROR");
            return;
        }

        // Read file type and content from client
        Object fileTypeObj = input.readObject();
        if (!(fileTypeObj instanceof String)) {
            sendError("INVALID_FILE_TYPE");
            return;
        }
        String fileType = ((String) fileTypeObj).toLowerCase();

        Object contentObj = input.readObject();
        if (!(contentObj instanceof byte[])) {
            sendError("INVALID_CONTENT_TYPE");
            return;
        }
        byte[] content = (byte[]) contentObj;

        try {
            // Validate file type
            if (!fileType.equals("txt") && !fileType.equals("png") && !fileType.equals("jpg") && !fileType.equals("jpeg")) {
                sendError("UNSUPPORTED_FILE_TYPE: " + fileType);
                return;
            }

            // Fetch document details
            String sql = "SELECT file_name, file_type, encryption_key, is_encrypted, owner_id, file_path " +
                         "FROM PersonalDocuments WHERE docID = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, docId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        sendError("DOCUMENT_NOT_FOUND");
                        return;
                    }

                    String fileName = rs.getString("file_name");
                    String storedFileType = rs.getString("file_type").toLowerCase();
                    String encryptionKey = rs.getString("encryption_key");
                    boolean isEncrypted = rs.getInt("is_encrypted") == 1;
                    String filePath = rs.getString("file_path");
                    boolean hasWriteAccess = rs.getInt("owner_id") == userId;

                    // Verify file type matches
                    if (!fileType.equals(storedFileType)) {
                        sendError("FILE_TYPE_MISMATCH: Expected " + storedFileType + ", got " + fileType);
                        return;
                    }

                    // Check write permissions for non-owners
                    if (!hasWriteAccess) {
                        String permissionSql = "SELECT accessID FROM UserPermissions WHERE docID = ? AND userID = ?";
                        try (PreparedStatement permStmt = dbConnection.prepareStatement(permissionSql)) {
                            permStmt.setInt(1, docId);
                            permStmt.setInt(2, userId);
                            try (ResultSet permRs = permStmt.executeQuery()) {
                                if (permRs.next()) {
                                    int accessId = permRs.getInt("accessID");
                                    boolean[] flags = getPermissionFlags(accessId);
                                    hasWriteAccess = flags[1]; // Check write permission
                                }
                            }
                        }
                    }

                    if (!hasWriteAccess) {
                        sendError("NO_WRITE_PERMISSION");
                        return;
                    }

                    Path path = Paths.get(UPLOAD_DIR, filePath != null ? filePath : getUsername(), fileName);
                    if (!Files.exists(path.getParent())) {
                        Files.createDirectories(path.getParent());
                    }

                    // Handle file writing
                    if (isEncrypted) {
                        if (encryptionKey == null || encryptionKey.isEmpty()) {
                            sendError("ENCRYPTION_KEY_NOT_FOUND");
                            return;
                        }
                        try {
                            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
                            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                                sendError("INVALID_KEY_LENGTH");
                                return;
                            }
                            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
                            fileSecurity.setKey(secretKey);

                            Path tempFile = Files.createTempFile(path.getParent(), "plain_", ".tmp");
                            try {
                                // Write plain content to temp file
                                Files.write(tempFile, content);
                                // Encrypt temp file to target path
                                fileSecurity.encryptFile(tempFile, path);
                            } finally {
                                Files.deleteIfExists(tempFile);
                            }
                        } catch (IllegalArgumentException | InvalidKeyException e) {
                            sendError("INVALID_ENCRYPTION_KEY: " + e.getMessage());
                            return;
                        } catch (Exception e) {
                            sendError("ENCRYPTION_ERROR: " + e.getMessage());
                            return;
                        }
                    } else {
                        Files.write(path, content);
                    }

                    // Update modify_date and file_size in database
                    String updateSql = "UPDATE PersonalDocuments SET modify_date = ?, file_size = ? WHERE docID = ?";
                    try (PreparedStatement updateStmt = dbConnection.prepareStatement(updateSql)) {
                        updateStmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                        updateStmt.setLong(2, content.length);
                        updateStmt.setInt(3, docId);
                        updateStmt.executeUpdate();
                    }

                    sendResponse("UPDATE_SUCCESS");
                    System.out.println("Successfully updated content for docID: " + docId);
                }
            }
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            sendError("FILE_SYSTEM_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleRestoreDocuments(List<Integer> docIDs) throws IOException {
        if (!isAuthenticated) {
            sendError("NOT_AUTHENTICATED");
            return;
        }

        if (userId == -1) {
            sendError("INVALID_USER_ID");
            return;
        }

        if (dbConnection == null || isConnectionClosed()) {
            sendError("DATABASE_CONNECTION_ERROR");
            return;
        }

        if (docIDs == null || docIDs.isEmpty()) {
            sendError("NO_DOCUMENTS_SELECTED");
            return;
        }

        try {
            dbConnection.setAutoCommit(false); // Start transaction
            int restoredCount = 0;

            for (Integer docId : docIDs) {
                // Validate document exists in TrashDocuments and belongs to the current user
                String docCheckSql = "SELECT file_name, file_hash, upload_date, modify_date, owner_id, file_type, file_size, encryption_key, is_encrypted, file_path " +
                                "FROM TrashDocuments WHERE docID = ? AND owner_id = ?";
                String fileName = null;
                String fileHash = null;
                Timestamp uploadDate = null;
                Timestamp modifyDate = null;
                String fileType = null;
                long fileSize = 0;
                String encryptionKey = null;
                int isEncrypted = 0;
                String filePath = null;

                try (PreparedStatement docStmt = dbConnection.prepareStatement(docCheckSql)) {
                    docStmt.setInt(1, docId);
                    docStmt.setInt(2, userId);
                    try (ResultSet rs = docStmt.executeQuery()) {
                        if (rs.next()) {
                            fileName = rs.getString("file_name");
                            fileHash = rs.getString("file_hash");
                            uploadDate = rs.getTimestamp("upload_date");
                            modifyDate = rs.getTimestamp("modify_date");
                            fileType = rs.getString("file_type");
                            fileSize = rs.getLong("file_size");
                            encryptionKey = rs.getString("encryption_key");
                            isEncrypted = rs.getInt("is_encrypted");
                            filePath = rs.getString("file_path");
                        } else {
                            continue; // Skip invalid or unauthorized documents
                        }
                    }
                }

                // Extract original path from Trash/<oldPath>
                if (filePath == null || !filePath.startsWith("Trash/")) {
                    continue; // Skip if filePath is invalid or doesn't start with Trash/
                }
                String originalPath = filePath.substring("Trash/".length()); // Remove Trash/ prefix

                // Move file back to original directory
                Path sourcePath = Paths.get(UPLOAD_DIR, filePath, fileName);
                Path targetPath = Paths.get(UPLOAD_DIR, originalPath, fileName);
                Path targetDir = Paths.get(UPLOAD_DIR, originalPath);
                Files.createDirectories(targetDir);

                if (!Files.exists(sourcePath)) {
                    continue; // Skip if file doesn't exist
                }

                try {
                    Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Moved file from " + sourcePath + " to " + targetPath);
                } catch (IOException e) {
                    dbConnection.rollback();
                    sendError("FILE_MOVE_ERROR: " + e.getMessage());
                    return;
                }

                // Move document back to PersonalDocuments
                String insertSql = "INSERT INTO PersonalDocuments (docID, file_name, file_hash, upload_date, modify_date, owner_id, file_type, file_size, encryption_key, is_encrypted, file_path) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement insertStmt = dbConnection.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, docId);
                    insertStmt.setString(2, fileName);
                    insertStmt.setString(3, fileHash);
                    insertStmt.setTimestamp(4, uploadDate);
                    insertStmt.setTimestamp(5, modifyDate);
                    insertStmt.setInt(6, userId);
                    insertStmt.setString(7, fileType);
                    insertStmt.setLong(8, fileSize);
                    insertStmt.setString(9, encryptionKey);
                    insertStmt.setInt(10, isEncrypted);
                    insertStmt.setString(11, originalPath); // Use original path without Trash/
                    insertStmt.executeUpdate();
                }

                // Delete from TrashDocuments
                String deleteSql = "DELETE FROM TrashDocuments WHERE docID = ? AND owner_id = ?";
                try (PreparedStatement deleteStmt = dbConnection.prepareStatement(deleteSql)) {
                    deleteStmt.setInt(1, docId);
                    deleteStmt.setInt(2, userId);
                    int rowsAffected = deleteStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        restoredCount++;
                    }
                }
            }

            dbConnection.commit();
            if (restoredCount > 0) {
                sendResponse("RESTORE_SUCCESS");
                System.out.println("Successfully restored " + restoredCount + " documents for userID: " + userId);
            } else {
                dbConnection.rollback();
                sendError("NO_VALID_DOCUMENTS_RESTORED");
            }
        } catch (SQLException e) {
            try {
                dbConnection.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Rollback failed: " + rollbackEx.getMessage());
            }
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            try {
                dbConnection.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Rollback failed: " + rollbackEx.getMessage());
            }
            sendError("FILE_SYSTEM_ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                dbConnection.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
            }
        }
    }

    private void handlePermanentDeleteDocuments(List<Integer> docIDs) throws IOException {
        if (!isAuthenticated) {
            sendError("NOT_AUTHENTICATED");
            return;
        }

        if (userId == -1) {
            sendError("INVALID_USER_ID");
            return;
        }

        if (dbConnection == null || isConnectionClosed()) {
            sendError("DATABASE_CONNECTION_ERROR");
            return;
        }

        if (docIDs == null || docIDs.isEmpty()) {
            sendError("NO_DOCUMENTS_SELECTED");
            return;
        }

        try {
            dbConnection.setAutoCommit(false); // Start transaction
            int deletedCount = 0;

            for (Integer docId : docIDs) {
                // Validate document exists in TrashDocuments and belongs to the current user
                String docCheckSql = "SELECT file_name, file_path FROM TrashDocuments WHERE docID = ? AND owner_id = ?";
                String fileName = null;
                String filePath = null;

                try (PreparedStatement docStmt = dbConnection.prepareStatement(docCheckSql)) {
                    docStmt.setInt(1, docId);
                    docStmt.setInt(2, userId);
                    try (ResultSet rs = docStmt.executeQuery()) {
                        if (rs.next()) {
                            fileName = rs.getString("file_name");
                            filePath = rs.getString("file_path");
                        } else {
                            continue; // Skip invalid or unauthorized documents
                        }
                    }
                }

                // Validate file path
                if (filePath == null || !filePath.startsWith("Trash/")) {
                    continue; // Skip if filePath is invalid or doesn't start with Trash/
                }

                // Delete file from filesystem
                Path fileToDelete = Paths.get(UPLOAD_DIR, filePath, fileName);
                if (Files.exists(fileToDelete)) {
                    try {
                        Files.delete(fileToDelete);
                        System.out.println("Deleted file: " + fileToDelete);
                    } catch (IOException e) {
                        dbConnection.rollback();
                        sendError("FILE_DELETE_ERROR: " + e.getMessage());
                        return;
                    }
                }

                // Delete from UserPermissions
                String deletePermissionsSql = "DELETE FROM UserPermissions WHERE docID = ?";
                try (PreparedStatement deletePermStmt = dbConnection.prepareStatement(deletePermissionsSql)) {
                    deletePermStmt.setInt(1, docId);
                    deletePermStmt.executeUpdate();
                }

                // Delete from TrashDocuments
                String deleteSql = "DELETE FROM TrashDocuments WHERE docID = ? AND owner_id = ?";
                try (PreparedStatement deleteStmt = dbConnection.prepareStatement(deleteSql)) {
                    deleteStmt.setInt(1, docId);
                    deleteStmt.setInt(2, userId);
                    int rowsAffected = deleteStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        deletedCount++;
                    }
                }
            }

            dbConnection.commit();
            if (deletedCount > 0) {
                sendResponse("PERMANENT_DELETE_SUCCESS");
                System.out.println("Successfully deleted " + deletedCount + " documents for userID: " + userId);
            } else {
                dbConnection.rollback();
                sendError("NO_VALID_DOCUMENTS_DELETED");
            }
        } catch (SQLException e) {
            try {
                dbConnection.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Rollback failed: " + rollbackEx.getMessage());
            }
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            try {
                dbConnection.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("Rollback failed: " + rollbackEx.getMessage());
            }
            sendError("FILE_SYSTEM_ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                dbConnection.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
            }
        }
    }

    private void handleShareDocument(int docId, int userIdToShare, int accessId) throws IOException {
        if (userId == -1) {
            sendError("INVALID_USER_ID");
            return;
        }

        if (dbConnection == null || isConnectionClosed()) {
            sendError("DATABASE_CONNECTION_ERROR");
            return;
        }

        try {
            // Validate document exists and belongs to the current user
            String docCheckSql = "SELECT COUNT(*) FROM PersonalDocuments WHERE docID = ? AND owner_id = ?";
            try (PreparedStatement docStmt = dbConnection.prepareStatement(docCheckSql)) {
                docStmt.setInt(1, docId);
                docStmt.setInt(2, userId);
                try (ResultSet rs = docStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        sendError("DOCUMENT_NOT_FOUND");
                        return;
                    }
                }
            }

            // Validate userIdToShare exists in Users table
            String userCheckSql = "SELECT COUNT(*) FROM Users WHERE userID = ?";
            try (PreparedStatement userStmt = dbConnection.prepareStatement(userCheckSql)) {
                userStmt.setInt(1, userIdToShare);
                try (ResultSet rs = userStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        sendError("INVALID_USER_ID");
                        return;
                    }
                }
            }

            // Validate accessId is between 1 and 7 (as defined in AccessLevel)
            if (accessId < 1 || accessId > 7) {
                sendError("INVALID_ACCESS_ID");
                return;
            }

            // Insert or update the UserPermissions table
            String sql = "INSERT INTO UserPermissions (docID, userID, accessID) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE accessID = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, docId);
                stmt.setInt(2, userIdToShare);
                stmt.setInt(3, accessId);
                stmt.setInt(4, accessId);
                int rowsAffected = stmt.executeUpdate();
                System.out.println("Shared document docID: " + docId + " with userID: " + userIdToShare +
                                    ", accessID: " + accessId + ", rows affected: " + rowsAffected);
                sendResponse("SHARE_SUCCESS");
            }
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGetDocumentPermissions(int docId) throws IOException {
        if (userId == -1) {
            sendError("INVALID_USER_ID");
            return;
        }

        if (dbConnection == null || isConnectionClosed()) {
            sendError("DATABASE_CONNECTION_ERROR");
            return;
        }

        try {
            // Validate document exists and belongs to the current user
            String docCheckSql = "SELECT COUNT(*) FROM PersonalDocuments WHERE docID = ? AND owner_id = ?";
            try (PreparedStatement docStmt = dbConnection.prepareStatement(docCheckSql)) {
                docStmt.setInt(1, docId);
                docStmt.setInt(2, userId);
                try (ResultSet rs = docStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        sendError("DOCUMENT_NOT_FOUND");
                        return;
                    }
                }
            }

            // Fetch permissions from UserPermissions, joining with Users to get login
            String sql = "SELECT up.userID, u.login, up.accessID " +
                        "FROM UserPermissions up " +
                        "JOIN Users u ON up.userID = u.userID " +
                        "WHERE up.docID = ?";
            List<List<String>> permissions = new ArrayList<>();
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, docId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int userId = rs.getInt("userID");
                        String login = rs.getString("login");
                        int accessId = rs.getInt("accessID");

                        // Compute permission flags based on accessId
                        boolean[] flags = getPermissionFlags(accessId);
                        boolean canRead = flags[0];
                        boolean canWrite = flags[1];
                        boolean canDownload = flags[2];
                        boolean canDelete = flags[3];

                        List<String> permission = new ArrayList<>();
                        permission.add(String.valueOf(userId));
                        permission.add(login != null ? login : "Unknown");
                        permission.add(String.valueOf(canRead));
                        permission.add(String.valueOf(canWrite));
                        permission.add(String.valueOf(canDownload));
                        permission.add(String.valueOf(canDelete));

                        permissions.add(permission);
                    }
                }
            }

            output.writeObject(permissions);
            output.flush();
            sendResponse("PERMISSIONS_LIST_SUCCESS");
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean[] getPermissionFlags(int accessId) {
        boolean canRead = false;
        boolean canWrite = false;
        boolean canDownload = false;
        boolean canDelete = false;

        switch (accessId) {
            case 7: // ALL_PERMISSIONS
                canRead = canWrite = canDownload = canDelete = true;
                break;
            case 6: // READ_WRITE_DELETE
                canRead = canWrite = canDelete = true;
                break;
            case 5: // READ_WRITE_DOWNLOAD
                canRead = canWrite = canDownload = true;
                break;
            case 4: // READ_DELETE
                canRead = canDelete = true;
                break;
            case 3: // READ_DOWNLOAD
                canRead = canDownload = true;
                break;
            case 2: // READ_WRITE
                canRead = canWrite = true;
                break;
            case 1: // READ_ONLY
                canRead = true;
                break;
            default:
                // Invalid accessId, default to no permissions
                break;
        }

        return new boolean[]{canRead, canWrite, canDownload, canDelete};
    }

    private void handleListUsers() throws IOException {
        if (!isAuthenticated) {
            sendError("NOT_AUTHENTICATED");
            return;
        }

        if (userId == -1) {
            sendError("INVALID_USER_ID");
            return;
        }

        if (dbConnection == null || isConnectionClosed()) {
            sendError("DATABASE_CONNECTION_ERROR");
            return;
        }

        try {
            String sql = "SELECT userID, login FROM Users";
            List<List<String>> users = new ArrayList<>();
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        List<String> user = new ArrayList<>();
                        int userID = rs.getInt("userID");
                        String login = rs.getString("login");

                        user.add(String.valueOf(userID));
                        user.add(login != null ? login : "Unknown");

                        users.add(user);
                    }
                }
            }

            output.writeObject(users);
            output.flush();
            sendResponse("USERS_LIST_SUCCESS");
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleListUsersShare(int docId) throws IOException {
        if (!isAuthenticated) {
            sendError("NOT_AUTHENTICATED");
            return;
        }

        if (userId == -1) {
            sendError("INVALID_USER_ID");
            return;
        }

        if (dbConnection == null || isConnectionClosed()) {
            sendError("DATABASE_CONNECTION_ERROR");
            return;
        }

        try {
            // Validate document exists and belongs to the current user
            String docCheckSql = "SELECT COUNT(*) FROM PersonalDocuments WHERE docID = ? AND owner_id = ?";
            try (PreparedStatement docStmt = dbConnection.prepareStatement(docCheckSql)) {
                docStmt.setInt(1, docId);
                docStmt.setInt(2, userId);
                try (ResultSet rs = docStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        sendError("DOCUMENT_NOT_FOUND");
                        return;
                    }
                }
            }

            // Fetch users who do NOT have permissions for the document, excluding the current user
            String sql = "SELECT u.userID, u.login FROM Users u " +
                        "WHERE u.userID != ? AND u.userID NOT IN (" +
                        "SELECT userID FROM UserPermissions WHERE docID = ?)";
            List<List<String>> users = new ArrayList<>();
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, userId); // Exclude current user
                stmt.setInt(2, docId);  // Exclude users with permissions for docId
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        List<String> user = new ArrayList<>();
                        int userID = rs.getInt("userID");
                        String login = rs.getString("login");

                        user.add(String.valueOf(userID));
                        user.add(login != null ? login : "Unknown");

                        users.add(user);
                    }
                }
            }

            output.writeObject(users);
            output.flush();
            sendResponse("USERS_SHARE_LIST_SUCCESS");
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleEncryptFile(int docId) throws IOException {
        try {
            String sql = "SELECT file_name, is_encrypted FROM PersonalDocuments WHERE docID = ? AND owner_id = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, docId);
                stmt.setInt(2, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String fileName = rs.getString("file_name");
                        int isEncrypted = rs.getInt("is_encrypted");

                        if (isEncrypted == 1) {
                            sendError("FILE_ALREADY_ENCRYPTED");
                            return;
                        }

                        Path userDir = getUserDirectory();
                        Path filePath = userDir.resolve(fileName);

                        if (!Files.exists(filePath)) {
                            sendError("FILE_NOT_FOUND");
                            return;
                        }

                        // Generate a new encryption key
                        SecretKey secretKey;
                        try {
                            secretKey = fileSecurity.generateKey();
                            fileSecurity.setKey(secretKey);
                        } catch (NoSuchAlgorithmException e) {
                            sendError("KEY_GENERATION_ERROR: " + e.getMessage());
                            return;
                        }

                        // Encode the key
                        String keyString = Base64.getEncoder().encodeToString(secretKey.getEncoded());
                        System.out.println("Generated encryption key for docID " + docId + ": [" + keyString + "]");

                        // Validate key
                        try {
                            byte[] keyBytes = Base64.getDecoder().decode(keyString);
                            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                                System.err.println("Invalid AES key length: " + keyBytes.length);
                                sendError("INVALID_KEY_LENGTH");
                                return;
                            }
                        } catch (IllegalArgumentException e) {
                            System.err.println("Base64 encoding error for generated key: " + e.getMessage());
                            sendError("KEY_GENERATION_ERROR: " + e.getMessage());
                            return;
                        }

                        // Encrypt the file
                        Path tempFile = Files.createTempFile(userDir, "encrypted_", ".tmp");
                        try {
                            fileSecurity.encryptFile(filePath, tempFile);
                            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);

                            // Store the encryption key and update is_encrypted
                            String updateSql = "UPDATE PersonalDocuments SET encryption_key = ?, is_encrypted = 1 WHERE docID = ? AND owner_id = ?";
                            try (PreparedStatement updateStmt = dbConnection.prepareStatement(updateSql)) {
                                updateStmt.setString(1, keyString);
                                updateStmt.setInt(2, docId);
                                updateStmt.setInt(3, userId);
                                int rows = updateStmt.executeUpdate();
                                if (rows == 0) {
                                    sendError("DATABASE_UPDATE_FAILED");
                                    return;
                                }
                            }

                            sendResponse("ENCRYPT_SUCCESS");
                        } catch (Exception e) {
                            sendError("ENCRYPTION_ERROR: " + e.getMessage());
                            throw e;
                        } finally {
                            Files.deleteIfExists(tempFile);
                        }
                    } else {
                        sendError("DOCUMENT_NOT_FOUND");
                    }
                }
            }
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            sendError("ENCRYPTION_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDecryptFile(int docId) throws IOException {
        try {
            String sql = "SELECT file_name, encryption_key, is_encrypted FROM PersonalDocuments WHERE docID = ? AND owner_id = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, docId);
                stmt.setInt(2, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String fileName = rs.getString("file_name");
                        String encryptionKey = rs.getString("encryption_key");
                        int isEncrypted = rs.getInt("is_encrypted");

                        if (isEncrypted == 0) {
                            sendError("FILE_NOT_ENCRYPTED");
                            return;
                        }

                        if (encryptionKey == null || encryptionKey.isEmpty()) {
                            sendError("ENCRYPTION_KEY_NOT_FOUND");
                            return;
                        }

                        Path userDir = getUserDirectory();
                        Path filePath = userDir.resolve(fileName);

                        if (!Files.exists(filePath)) {
                            sendError("FILE_NOT_FOUND");
                            return;
                        }

                        // Decode and validate the encryption key
                        byte[] keyBytes;
                        try {
                            keyBytes = Base64.getDecoder().decode(encryptionKey);
                            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                                System.err.println("Invalid AES key length for docID " + docId + ": " + keyBytes.length);
                                sendError("INVALID_KEY_LENGTH");
                                return;
                            }
                        } catch (IllegalArgumentException e) {
                            System.err.println("Base64 decoding error for key: [" + encryptionKey + "], error: " + e.getMessage());
                            sendError("INVALID_ENCRYPTION_KEY: " + e.getMessage());
                            return;
                        }

                        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
                        fileSecurity.setKey(secretKey);

                        // Decrypt the file
                        Path tempFile = Files.createTempFile(userDir, "decrypted_", ".tmp");
                        try {
                            fileSecurity.decryptFile(filePath, tempFile);
                            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);

                            // Update the database
                            String updateSql = "UPDATE PersonalDocuments SET encryption_key = NULL, is_encrypted = 0 WHERE docID = ? AND owner_id = ?";
                            try (PreparedStatement updateStmt = dbConnection.prepareStatement(updateSql)) {
                                updateStmt.setInt(1, docId);
                                updateStmt.setInt(2, userId);
                                int rowsAffected = updateStmt.executeUpdate();
                                if (rowsAffected > 0) {
                                    System.out.println("Successfully set encryption_key to NULL and is_encrypted to 0 for docID: " + docId);
                                } else {
                                    sendError("DATABASE_UPDATE_FAILED");
                                    return;
                                }
                            }

                            sendResponse("DECRYPT_SUCCESS");
                        } catch (InvalidKeyException | BadPaddingException e) {
                            sendError("DECRYPTION_ERROR: " + e.getMessage());
                            throw e;
                        } finally {
                            Files.deleteIfExists(tempFile);
                        }
                    } else {
                        sendError("DOCUMENT_NOT_FOUND");
                    }
                }
            }
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            sendError("DECRYPTION_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleGetDocumentContent(int docId) throws IOException {
        try {
            String sql = "SELECT pd.file_name, pd.file_type, pd.encryption_key, pd.is_encrypted, pd.owner_id, pd.file_path " +
                         "FROM PersonalDocuments pd " +
                         "LEFT JOIN UserPermissions up ON pd.docID = up.docID AND up.userID = ? " +
                         "WHERE pd.docID = ?";
            
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, docId);
                
                String fileName = null;
                String fileType = null;
                String encryptionKey = null;
                boolean isEncrypted = false;
                String filePath = null;
                boolean hasReadAccess = false;

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        fileName = rs.getString("file_name");
                        fileType = rs.getString("file_type");
                        encryptionKey = rs.getString("encryption_key");
                        isEncrypted = rs.getInt("is_encrypted") == 1;
                        filePath = rs.getString("file_path");
                        hasReadAccess = rs.getInt("owner_id") == userId;
                    } else {
                        sendError("DOCUMENT_NOT_FOUND");
                        return;
                    }
                }

                // Validate file type
                if (fileType != null) {
                    fileType = fileType.toLowerCase();
                    if (!fileType.equals("txt") && !fileType.equals("png") && !fileType.equals("jpg") && !fileType.equals("jpeg")) {
                        sendError("UNSUPPORTED_FILE_TYPE: " + fileType);
                        return;
                    }
                } else {
                    fileType = "unknown";
                }

                // Check read permissions for non-owners
                if (!hasReadAccess) {
                    String permissionSql = "SELECT accessID FROM UserPermissions WHERE docID = ? AND userID = ?";
                    try (PreparedStatement permStmt = dbConnection.prepareStatement(permissionSql)) {
                        permStmt.setInt(1, docId);
                        permStmt.setInt(2, userId);
                        try (ResultSet permRs = permStmt.executeQuery()) {
                            if (permRs.next()) {
                                int accessId = permRs.getInt("accessID");
                                boolean[] flags = getPermissionFlags(accessId);
                                hasReadAccess = flags[0]; // Check read permission
                            }
                        }
                    }
                }

                if (!hasReadAccess) {
                    sendError("NO_READ_PERMISSION");
                    return;
                }

                Path path = Paths.get(UPLOAD_DIR, filePath != null ? filePath : getUsername(), fileName);
                if (!Files.exists(path)) {
                    sendError("FILE_NOT_FOUND");
                    return;
                }

                // Check file size to prevent memory issues
                long fileSize = Files.size(path);
                if (fileSize > 100_000_000) { // Limit to 100MB
                    sendError("FILE_TOO_LARGE");
                    return;
                }

                byte[] fileContent;
                if (isEncrypted) {
                    if (encryptionKey == null || encryptionKey.isEmpty()) {
                        sendError("ENCRYPTION_KEY_NOT_FOUND");
                        return;
                    }
                    try {
                        System.out.println("Decrypting content for docID " + docId + " with key: [" + encryptionKey + "]");
                        byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
                        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                            System.err.println("Invalid AES key length for docID " + docId + ": " + keyBytes.length);
                            sendError("INVALID_KEY_LENGTH");
                            return;
                        }
                        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
                        fileSecurity.setKey(secretKey);

                        Path tempFile = Files.createTempFile(getUserDirectory(), "decrypted_", ".tmp");
                        try {
                            fileSecurity.decryptFile(path, tempFile);
                            fileContent = Files.readAllBytes(tempFile);
                        } finally {
                            Files.deleteIfExists(tempFile);
                        }
                    } catch (IllegalArgumentException | InvalidKeyException e) {
                        System.err.println("Base64 decoding error for key: [" + encryptionKey + "], error: " + e.getMessage());
                        sendError("INVALID_ENCRYPTION_KEY: " + e.getMessage());
                        return;
                    } catch (Exception e) {
                        sendError("DECRYPTION_ERROR: " + e.getMessage());
                        return;
                    }
                } else {
                    fileContent = Files.readAllBytes(path);
                }

                // Send file metadata and content
                output.writeObject(isEncrypted);
                output.writeObject(fileType);
                output.writeObject(fileContent);
                output.flush();
                sendResponse("CONTENT_SUCCESS");
            }
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            sendError("CONTENT_RETRIEVAL_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGetEncryptionKey(int docId) throws IOException {
        try {
            String sql = "SELECT encryption_key, is_encrypted, owner_id FROM PersonalDocuments WHERE docID = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, docId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        boolean isOwner = rs.getInt("owner_id") == userId;
                        boolean isEncrypted = rs.getInt("is_encrypted") == 1;
                        String encryptionKey = rs.getString("encryption_key");
                        System.out.println("Retrieved encryption key for docID " + docId + ": [" + encryptionKey + "]");

                        if (!isOwner) {
                            String permissionSql = "SELECT accessID FROM UserPermissions WHERE docID = ? AND userID = ?";
                            try (PreparedStatement permStmt = dbConnection.prepareStatement(permissionSql)) {
                                permStmt.setInt(1, docId);
                                permStmt.setInt(2, userId);
                                try (ResultSet permRs = permStmt.executeQuery()) {
                                    if (!permRs.next()) {
                                        sendError("NO_READ_PERMISSION");
                                        return;
                                    }
                                }
                            }
                        }

                        if (isEncrypted && encryptionKey != null && !encryptionKey.isEmpty()) {
                            // Validate Base64 format
                            if (!encryptionKey.matches("^[A-Za-z0-9+/=]+$")) {
                                System.err.println("Invalid Base64 key format in database for docID " + docId + ": [" + encryptionKey + "]");
                                sendError("INVALID_ENCRYPTION_KEY");
                                return;
                            }
                            // Validate key length after decoding
                            try {
                                byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
                                if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                                    System.err.println("Invalid AES key length for docID " + docId + ": " + keyBytes.length);
                                    sendError("INVALID_KEY_LENGTH");
                                    return;
                                }
                            } catch (IllegalArgumentException e) {
                                System.err.println("Base64 decoding error for key: [" + encryptionKey + "], error: " + e.getMessage());
                                sendError("INVALID_ENCRYPTION_KEY: " + e.getMessage());
                                return;
                            }
                            System.out.println("Sending encryption key for docID " + docId + ": [" + encryptionKey + "]");
                            output.writeObject("KEY:" + encryptionKey);
                            output.flush();
                        } else {
                            sendError("ENCRYPTION_KEY_NOT_FOUND");
                        }
                    } else {
                        sendError("DOCUMENT_NOT_FOUND");
                    }
                }
            }
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleAuthenticate() throws IOException, ClassNotFoundException {
        if (isAuthenticated) {
            sendError("ALREADY_AUTHENTICATED");
            return;
        }
        if (authAttempts >= MAX_AUTH_ATTEMPTS) {
            sendError("MAX_AUTH_ATTEMPTS_EXCEEDED");
            cleanupResources();
            return;
        }

        String[] credentials = (String[]) input.readObject();
        if (authenticate(credentials[0], credentials[1])) {
            isAuthenticated = true;
            authAttempts = 0;
            sendResponse("AUTH_SUCCESS");
        } else {
            authAttempts++;
            sendResponse("AUTH_FAIL");
        }
    }

    private boolean authenticate(String login, String password) {
        try (PreparedStatement stmt = dbConnection.prepareStatement(
                "SELECT userID, hashed_password, login FROM Users WHERE login = ?")) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("hashed_password");
                if (storedHash.equals(password)) {
                    userId = rs.getInt("userID");
                    username = rs.getString("login");
                    System.out.println("Authenticated user: " + username + " with userID: " + userId);
                    return true;
                } else {
                    System.out.println("Password mismatch for user: " + login);
                }
            } else {
                System.out.println("No user found with login: " + login);
            }
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private void handleCheckDuplicateFile() throws IOException {
        try {
            String fileName = input.readUTF();
            if (!isValidFilename(fileName)) {
                sendResponse("INVALID_FILENAME");
                return;
            }

            boolean isDuplicate = checkDuplicateFile(fileName);
            if (isDuplicate) {
                sendResponse("DUPLICATE_FILE");
            } else {
                sendResponse("FILE_NOT_FOUND");
            }
        } catch (Exception e) {
            sendError("CHECK_DUPLICATE_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean checkDuplicateFile(String fileName) {
        try {
            String sql = "SELECT COUNT(*) FROM PersonalDocuments WHERE owner_id = ? AND file_name = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, fileName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        if (count > 0) {
                            System.out.println("Duplicate file found in database: " + fileName + " for userID: " + userId);
                            return true;
                        }
                    }
                }
            }

            Path userDir = getUserDirectory();
            Path filePath = userDir.resolve(fileName);
            if (Files.exists(filePath)) {
                System.out.println("Duplicate file found in file system: " + filePath.toAbsolutePath());
                return true;
            }

            return false;
        } catch (SQLException | IOException e) {
            System.err.println("Error checking for duplicate file: " + e.getMessage());
            e.printStackTrace();
            return false; // Assume no duplicate on error to allow upload
        }
    }

    private void handleFileUpload() throws IOException {
        try {
            String fileName = input.readUTF();
            long fileSize = input.readLong();

            if (!isValidFilename(fileName)) {
                sendResponse("INVALID_FILENAME");
                return;
            }

            if (checkDuplicateFile(fileName)) {
                sendResponse("DUPLICATE_FILE");
                return;
            }

            if (!hasEnoughSpace(fileSize)) {
                sendResponse("INSUFFICIENT_SPACE");
                return;
            }

            Path userDir = getUserDirectory();
            Path filePath = userDir.resolve(fileName);

            try (OutputStream fileOut = Files.newOutputStream(filePath)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                long remaining = fileSize;

                while (remaining > 0) {
                    int bytesToRead = (int) Math.min(buffer.length, remaining);
                    int bytesRead = input.read(buffer, 0, bytesToRead);
                    if (bytesRead == -1) break;

                    fileOut.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }

                if (remaining == 0) {
                    sendResponse("UPLOAD_SUCCESS");
                } else {
                    Files.deleteIfExists(filePath);
                    sendResponse("UPLOAD_INCOMPLETE");
                }
            }
        } catch (Exception e) {
            sendError("UPLOAD_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleFileUploadWithMetadata() throws IOException {
        try {
            String fileName = input.readUTF();
            String fileType = input.readUTF();
            long fileSize = input.readLong();
            LocalDateTime uploadDate = (LocalDateTime) input.readObject();
            byte[] fileContent = (byte[]) input.readObject();

            if (!isValidFilename(fileName)) {
                sendResponse("INVALID_FILENAME");
                return;
            }

            if (checkDuplicateFile(fileName)) {
                sendResponse("DUPLICATE_FILE");
                return;
            }

            Path userDir = getUserDirectory();
            Path filePath = userDir.resolve(fileName);
            Files.write(filePath, fileContent);

            String fileHash = calculateFileHash(filePath);
            storeDocumentInDB(fileName, fileHash, uploadDate, fileType, fileSize, null, 0);

            sendResponse("UPLOAD_SUCCESS");
        } catch (Exception e) {
            sendError("UPLOAD_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleEncryptedFileUploadWithMetadata() throws IOException {
        try {
            String fileName = input.readUTF();
            String fileType = input.readUTF();
            long fileSize = input.readLong();
            LocalDateTime uploadDate = (LocalDateTime) input.readObject();
            String encryptionKey = input.readUTF();
            byte[] encryptedContent = (byte[]) input.readObject();

            if (!isValidFilename(fileName)) {
                sendResponse("INVALID_FILENAME");
                return;
            }

            if (checkDuplicateFile(fileName)) {
                sendResponse("DUPLICATE_FILE");
                return;
            }

            // Validate encryption key
            byte[] keyBytes;
            try {
                keyBytes = Base64.getDecoder().decode(encryptionKey);
                if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                    sendError("INVALID_KEY_LENGTH");
                    return;
                }
            } catch (IllegalArgumentException e) {
                sendError("INVALID_ENCRYPTION_KEY: " + e.getMessage());
                return;
            }

            // Verify encrypted content integrity (basic check)
            if (encryptedContent == null || encryptedContent.length == 0) {
                sendError("INVALID_ENCRYPTED_CONTENT");
                return;
            }

            Path userDir = getUserDirectory();
            Path filePath = userDir.resolve(fileName);
            Files.write(filePath, encryptedContent);

            String fileHash = calculateFileHash(filePath);
            storeDocumentInDB(fileName, fileHash, uploadDate, fileType, fileSize, encryptionKey, 1);

            sendResponse("UPLOAD_SUCCESS");
        } catch (Exception e) {
            sendError("UPLOAD_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void storeDocumentInDB(String fileName, String fileHash, LocalDateTime uploadDate,
                                    String fileType, long fileSize, String encryptionKey, int isEncrypted) throws SQLException {
        String sql = "INSERT INTO PersonalDocuments (file_name, file_hash, upload_date, modify_date, owner_id, file_type, file_size, encryption_key, is_encrypted) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, fileName);
            stmt.setString(2, fileHash);
            stmt.setTimestamp(3, uploadDate != null ? Timestamp.valueOf(uploadDate) : null);
            stmt.setTimestamp(4, uploadDate != null ? Timestamp.valueOf(uploadDate) : null);
            stmt.setInt(5, userId);
            stmt.setString(6, fileType);
            stmt.setLong(7, fileSize);
            stmt.setString(8, encryptionKey);
            stmt.setInt(9, isEncrypted);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Stored document '" + fileName + "' for userID: " + userId + ", rows affected: " + rowsAffected +
                                ", encrypted: " + (encryptionKey != null));
        }
    }

    private void storeFileInDatabase(PersonalDocument document, Path filePath) throws IOException {
        try {
            String fileHash = calculateFileHash(filePath);
            String sql = "INSERT INTO PersonalDocuments (file_name, file_hash, upload_date, owner_id, file_type, file_size, encryption_key) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, document.getFileName());
                stmt.setString(2, fileHash);
                LocalDateTime uploadDate = document.getUploadDate();
                LocalDateTime uploadDateTime = uploadDate != null ? uploadDate : LocalDateTime.now();
                stmt.setTimestamp(3, Timestamp.valueOf(uploadDateTime));
                stmt.setInt(4, userId);
                stmt.setString(5, document.getNumberType());
                stmt.setLong(6, document.getSize());
                stmt.setString(7, null);
                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            System.out.println("Document stored with ID: " + generatedKeys.getInt(1));
                        }
                    }
                    sendResponse("FILE_LOG_SUCCESS");
                } else {
                    sendResponse("FILE_LOG_FAILED");
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String calculateFileHash(Path filePath) throws IOException, NoSuchAlgorithmException {
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

    private void handleListDocuments(String searchTerm) throws IOException {
        if (!isAuthenticated) {
            sendError("NOT_AUTHENTICATED");
            return;
        }

        if (userId == -1) {
            sendError("INVALID_USER_ID");
            return;
        }

        if (dbConnection == null || isConnectionClosed()) {
            sendError("DATABASE_CONNECTION_ERROR");
            return;
        }

        try {
            String sql = "SELECT docID, file_name, file_type, upload_date, file_size FROM PersonalDocuments WHERE owner_id = ?";
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                sql += " AND UPPER(file_name) LIKE UPPER(?)";
            }

            List<List<String>> documents = new ArrayList<>();
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                    stmt.setString(2, "%" + searchTerm.trim() + "%");
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        List<String> document = new ArrayList<>();
                        int docID = rs.getInt("docID");
                        String fileName = rs.getString("file_name");
                        String fileType = rs.getString("file_type");
                        Timestamp uploadTimestamp = rs.getTimestamp("upload_date");
                        long fileSize = rs.getLong("file_size");

                        String uploadDateStr = uploadTimestamp != null
                                ? uploadTimestamp.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                        document.add(String.valueOf(docID));
                        document.add(fileName != null ? fileName : "Unknown");
                        document.add(fileType != null ? fileType : "Unknown");
                        document.add(uploadDateStr);
                        document.add(String.valueOf(fileSize));

                        documents.add(document);
                    }
                }
            }

            output.writeObject(documents);
            output.flush();
            sendResponse("LIST_SUCCESS");
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleListSharedWithMeDocuments(String searchTerm) throws IOException {
        if (!isAuthenticated) {
            sendError("NOT_AUTHENTICATED");
            return;
        }

        if (userId == -1) {
            sendError("INVALID_USER_ID");
            return;
        }

        if (dbConnection == null || isConnectionClosed()) {
            sendError("DATABASE_CONNECTION_ERROR");
            return;
        }

        try {
            String sql = "SELECT pd.docID, pd.file_name, pd.file_type, pd.upload_date, pd.file_size FROM PersonalDocuments AS pd JOIN UserPermissions up ON pd.docID = up.docID AND up.userID = ?";
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                sql += " AND UPPER(pd.file_name) LIKE UPPER(?)";
            }

            List<List<String>> documents = new ArrayList<>();
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                    stmt.setString(2, "%" + searchTerm.trim() + "%");
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        List<String> document = new ArrayList<>();
                        int docID = rs.getInt("docID");
                        String fileName = rs.getString("file_name");
                        String fileType = rs.getString("file_type");
                        Timestamp uploadTimestamp = rs.getTimestamp("upload_date");
                        long fileSize = rs.getLong("file_size");

                        String uploadDateStr = uploadTimestamp != null
                                ? uploadTimestamp.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                        document.add(String.valueOf(docID));
                        document.add(fileName != null ? fileName : "Unknown");
                        document.add(fileType != null ? fileType : "Unknown");
                        document.add(uploadDateStr);
                        document.add(String.valueOf(fileSize));

                        documents.add(document);
                    }
                }
            }

            output.writeObject(documents);
            output.flush();
            sendResponse("LIST_SHARED_WITH_ME_SUCCESS");
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    
    private void handleDeleteDocument(int docId) throws IOException {
        if (!isAuthenticated) {
            sendError("NOT_AUTHENTICATED");
            return;
        }

        if (userId == -1) {
            sendError("INVALID_USER_ID");
            return;
        }

        if (dbConnection == null || isConnectionClosed()) {
            sendError("DATABASE_CONNECTION_ERROR");
            return;
        }

        try {
            // Validate document exists and belongs to the current user
            String docCheckSql = "SELECT file_name, file_hash, upload_date, modify_date, owner_id, file_type, file_size, encryption_key, is_encrypted, file_path " +
                                "FROM PersonalDocuments WHERE docID = ? AND owner_id = ?";
            String fileName = null;
            String fileHash = null;
            Timestamp uploadDate = null;
            Timestamp modifyDate = null;
            String fileType = null;
            long fileSize = 0;
            String encryptionKey = null;
            int isEncrypted = 0;
            String filePath = null;

            try (PreparedStatement docStmt = dbConnection.prepareStatement(docCheckSql)) {
                docStmt.setInt(1, docId);
                docStmt.setInt(2, userId);
                try (ResultSet rs = docStmt.executeQuery()) {
                    if (rs.next()) {
                        fileName = rs.getString("file_name");
                        fileHash = rs.getString("file_hash");
                        uploadDate = rs.getTimestamp("upload_date");
                        modifyDate = rs.getTimestamp("modify_date");
                        fileType = rs.getString("file_type");
                        fileSize = rs.getLong("file_size");
                        encryptionKey = rs.getString("encryption_key");
                        isEncrypted = rs.getInt("is_encrypted");
                        filePath = rs.getString("file_path");
                    } else {
                        sendError("DOCUMENT_NOT_FOUND");
                        return;
                    }
                }
            }

            // Move file to Trash directory
            Path sourcePath = Paths.get(UPLOAD_DIR, filePath != null ? filePath : getUsername(), fileName);
            String newPath = "Trash/" + (filePath != null ? filePath : getUsername());
            Path trashDir = Paths.get(UPLOAD_DIR, newPath);
            Files.createDirectories(trashDir);
            Path targetPath = trashDir.resolve(fileName);

            if (!Files.exists(sourcePath)) {
                sendError("FILE_NOT_FOUND");
                return;
            }

            try {
                Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Moved file from " + sourcePath + " to " + targetPath);
            } catch (IOException e) {
                sendError("FILE_MOVE_ERROR: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            // Move document to TrashDocuments table
            String insertTrashSql = "INSERT INTO TrashDocuments (docID, file_name, file_hash, upload_date, modify_date, owner_id, file_type, file_size, encryption_key, is_encrypted, file_path) " +
                                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = dbConnection.prepareStatement(insertTrashSql)) {
                insertStmt.setInt(1, docId);
                insertStmt.setString(2, fileName);
                insertStmt.setString(3, fileHash);
                insertStmt.setTimestamp(4, uploadDate);
                insertStmt.setTimestamp(5, modifyDate);
                insertStmt.setInt(6, userId);
                insertStmt.setString(7, fileType);
                insertStmt.setLong(8, fileSize);
                insertStmt.setString(9, encryptionKey);
                insertStmt.setInt(10, isEncrypted);
                insertStmt.setString(11, newPath);
                int rowsAffected = insertStmt.executeUpdate();
                if (rowsAffected == 0) {
                    sendError("DATABASE_INSERT_TRASH_FAILED");
                    // Attempt to rollback file move
                    try {
                        Files.move(targetPath, sourcePath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        System.err.println("Failed to rollback file move: " + e.getMessage());
                    }
                    return;
                }
            }

            // Delete from PersonalDocuments
            String deleteSql = "DELETE FROM PersonalDocuments WHERE docID = ? AND owner_id = ?";
            try (PreparedStatement deleteStmt = dbConnection.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, docId);
                deleteStmt.setInt(2, userId);
                int rowsAffected = deleteStmt.executeUpdate();
                if (rowsAffected == 0) {
                    sendError("DATABASE_DELETE_FAILED");
                    // Attempt to rollback TrashDocuments insert
                    try {
                        String rollbackSql = "DELETE FROM TrashDocuments WHERE docID = ?";
                        try (PreparedStatement rollbackStmt = dbConnection.prepareStatement(rollbackSql)) {
                            rollbackStmt.setInt(1, docId);
                            rollbackStmt.executeUpdate();
                        }
                        Files.move(targetPath, sourcePath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception e) {
                        System.err.println("Failed to rollback TrashDocuments insert: " + e.getMessage());
                    }
                    return;
                }
            }

            sendResponse("DELETE_SUCCESS");
            System.out.println("Successfully deleted document docID: " + docId + " for userID: " + userId);
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            sendError("FILE_SYSTEM_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleListDeletedDocuments(String searchTerm) throws IOException {
        if (!isAuthenticated) {
            sendError("NOT_AUTHENTICATED");
            return;
        }

        if (userId == -1) {
            sendError("INVALID_USER_ID");
            return;
        }

        if (dbConnection == null || isConnectionClosed()) {
            sendError("DATABASE_CONNECTION_ERROR");
            return;
        }

        try {
            String sql = "SELECT docID, file_name, file_type, upload_date, file_size FROM TrashDocuments WHERE owner_id = ?";
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                sql += " AND UPPER(file_name) LIKE UPPER(?)";
            }

            List<List<String>> documents = new ArrayList<>();
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                    stmt.setString(2, "%" + searchTerm.trim() + "%");
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        List<String> document = new ArrayList<>();
                        int docID = rs.getInt("docID");
                        String fileName = rs.getString("file_name");
                        String fileType = rs.getString("file_type");
                        Timestamp uploadTimestamp = rs.getTimestamp("upload_date");
                        long fileSize = rs.getLong("file_size");

                        String uploadDateStr = uploadTimestamp != null
                                ? uploadTimestamp.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                        document.add(String.valueOf(docID));
                        document.add(fileName != null ? fileName : "Unknown");
                        document.add(fileType != null ? fileType : "Unknown");
                        document.add(uploadDateStr);
                        document.add(String.valueOf(fileSize));

                        documents.add(document);
                    }
                }
            }

            output.writeObject(documents);
            output.flush();
            sendResponse("LIST_DELETED_SUCCESS");
            System.out.println("Sent " + documents.size() + " deleted documents to client");
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDocumentDownload() throws IOException {
        try {
            int docId = (Integer) input.readObject();
            String sql = "SELECT file_name, file_path, owner_id FROM PersonalDocuments WHERE docID = ?";
            
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, docId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String fileName = rs.getString("file_name");
                        String filePath = rs.getString("file_path");
                        boolean hasDownloadAccess = rs.getInt("owner_id") == userId;

                        // Check download permissions for non-owners
                        if (!hasDownloadAccess) {
                            String permissionSql = "SELECT accessID FROM UserPermissions WHERE docID = ? AND userID = ?";
                            try (PreparedStatement permStmt = dbConnection.prepareStatement(permissionSql)) {
                                permStmt.setInt(1, docId);
                                permStmt.setInt(2, userId);
                                try (ResultSet permRs = permStmt.executeQuery()) {
                                    if (permRs.next()) {
                                        int accessId = permRs.getInt("accessID");
                                        boolean[] flags = getPermissionFlags(accessId);
                                        hasDownloadAccess = flags[2]; // Check download permission
                                    }
                                }
                            }
                        }

                        if (!hasDownloadAccess) {
                            sendError("NO_DOWNLOAD_PERMISSION");
                            return;
                        }

                        Path path = Paths.get(UPLOAD_DIR, filePath != null ? filePath : getUsername(), fileName);
                        if (!Files.exists(path)) {
                            sendError("FILE_NOT_FOUND");
                            return;
                        }

                        // Read and send the file as-is
                        byte[] fileContent = Files.readAllBytes(path);
                        
                        // Send just the filename and file content
                        List<Object> responseData = new ArrayList<>();
                        responseData.add(fileName);
                        responseData.add(fileContent);

                        output.writeObject(responseData);
                        output.flush();
                        sendResponse("DOWNLOAD_SUCCESS");
                    } else {
                        sendError("DOCUMENT_NOT_FOUND");
                    }
                }
            }
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            sendError("DOWNLOAD_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Path getUserDirectory() throws IOException {
        Path userDir = Paths.get(UPLOAD_DIR, getUsername());
        Files.createDirectories(userDir);
        System.out.println("User directory: " + userDir.toAbsolutePath());
        return userDir;
    }

    private String getUsername() {
        if (username != null && !username.isEmpty()) {
            return username;
        }
        try (PreparedStatement stmt = dbConnection.prepareStatement("SELECT login FROM Users WHERE userID = ?")) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                username = rs.getString("login");
                return username;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching username: " + e.getMessage());
        }
        return "unknown_user";
    }

    private boolean isValidFilename(String fileName) {
        if (fileName == null || fileName.isEmpty() || fileName.length() > 255) {
            return false;
        }

        Pattern illegalChars = Pattern.compile("[\\\\/:*?\"<>|]|[\\x00-\\x1F]");
        Pattern reservedNames = Pattern.compile("^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?$", Pattern.CASE_INSENSITIVE);
        boolean containsPathTraversal = fileName.contains("..") || fileName.contains("./") || fileName.contains(".\\");

        return !illegalChars.matcher(fileName).find() &&
                !reservedNames.matcher(fileName).matches() &&
                !containsPathTraversal &&
                !fileName.endsWith(".") &&
                !fileName.endsWith(" ");
    }

    private boolean hasEnoughSpace(long requiredSize) {
        File uploadDir = new File(UPLOAD_DIR);
        return uploadDir.getUsableSpace() > requiredSize * 1.1;
    }

    private boolean isConnectionClosed() {
        try {
            return dbConnection.isClosed();
        } catch (SQLException e) {
            System.err.println("Error checking database connection state: " + e.getMessage());
            return true;
        }
    }

    private void sendResponse(String response) throws IOException {
        System.out.println("Sending response to client " + socket.getInetAddress() + ": " + response);
        output.writeObject(response);
        output.flush();
    }

    private void sendError(String error) throws IOException {
        System.out.println("Sending error to client " + socket.getInetAddress() + ": " + error);
        sendResponse("ERROR: " + error);
    }

    private void cleanupResources() {
        try {
            if (output != null) output.close();
            if (input != null) input.close();
            if (socket != null && !socket.isClosed()) socket.close();
            if (dbConnection != null && !dbConnection.isClosed()) dbConnection.close();
            System.out.println("Client " + userId + " resources cleaned up");
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
}