package com.emi.projetintegre.server;

import java.io.*;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.emi.projetintegre.models.PersonalDocument;

import java.nio.charset.StandardCharsets;

public class DocumentHandler {
    private static final int BUFFER_SIZE = 4096;

    private final Connection dbConnection;
    private final ObjectOutputStream output;
    private final ObjectInputStream input;
    private final int userId;
    private final UserHandler userHandler;
    private final EncryptionHandler encryptionHandler;

    public DocumentHandler(Connection dbConnection, ObjectOutputStream output, ObjectInputStream input,
                          int userId, UserHandler userHandler, EncryptionHandler encryptionHandler) {
        this.dbConnection = dbConnection;
        this.output = output;
        this.input = input;
        this.userId = userId;
        this.userHandler = userHandler;
        this.encryptionHandler = encryptionHandler;
    }

    public void handleGetDocumentContent(int docId) throws IOException {
        try {
            String sql = "SELECT file_name, file_type, encryption_key FROM PersonalDocuments WHERE docID = ? AND owner_id = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, docId);
                stmt.setInt(2, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String fileName = rs.getString("file_name");
                        String fileType = rs.getString("file_type");
                        String encryptionKey = rs.getString("encryption_key");
                        if (!fileType.equalsIgnoreCase("txt")) {
                            sendError("UNSUPPORTED_FILE_TYPE");
                            return;
                        }
                        Path userDir = userHandler.getUserDirectory();
                        Path filePath = userDir.resolve(fileName);
                        if (!Files.exists(filePath)) {
                            sendError("FILE_NOT_FOUND");
                            return;
                        }
                        if (encryptionKey != null && !encryptionKey.isEmpty()) {
                            sendError("ENCRYPTED_FILE_NOT_SUPPORTED");
                            return;
                        }
                        String fileContent = Files.readString(filePath, StandardCharsets.UTF_8);
                        output.writeObject(fileContent);
                        output.flush();
                        sendResponse("CONTENT_SUCCESS");
                    } else {
                        sendError("DOCUMENT_NOT_FOUND");
                    }
                }
            }
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
        }
    }

    public void handleCheckDuplicateFile() throws IOException {
        String fileName = input.readUTF();
        if (!isValidFilename(fileName)) {
            sendResponse("INVALID_FILENAME");
            return;
        }
        if (checkDuplicateFile(fileName)) {
            sendResponse("DUPLICATE_FILE");
        } else {
            sendResponse("FILE_NOT_FOUND");
        }
    }

    private boolean checkDuplicateFile(String fileName) {
        try {
            String sql = "SELECT COUNT(*) FROM PersonalDocuments WHERE owner_id = ? AND file_name = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, fileName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return true;
                    }
                }
            }
            Path userDir = userHandler.getUserDirectory();
            Path filePath = userDir.resolve(fileName);
            return Files.exists(filePath);
        } catch (SQLException | IOException e) {
            return false;
        }
    }

    public void handleFileUpload() throws IOException {
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
        if (!userHandler.hasEnoughSpace(fileSize)) {
            sendResponse("INSUFFICIENT_SPACE");
            return;
        }
        Path userDir = userHandler.getUserDirectory();
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
        } catch (Exception e) {
            sendError("UPLOAD_ERROR: " + e.getMessage());
        }
    }

    public void handleFileUploadWithMetadata() throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
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
        Path userDir = userHandler.getUserDirectory();
        Path filePath = userDir.resolve(fileName);
        Files.write(filePath, fileContent);
        String fileHash = encryptionHandler.calculateFileHash(filePath);
        storeDocumentInDB(fileName, fileHash, uploadDate, fileType, fileSize, null);
        sendResponse("UPLOAD_SUCCESS");
    }

    public void handleEncryptedFileUploadWithMetadata() throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
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
        Path userDir = userHandler.getUserDirectory();
        Path filePath = userDir.resolve(fileName);
        Files.write(filePath, encryptedContent);
        String fileHash = encryptionHandler.calculateFileHash(filePath);
        storeDocumentInDB(fileName, fileHash, uploadDate, fileType, fileSize, encryptionKey);
        sendResponse("UPLOAD_SUCCESS");
    }

    private void storeDocumentInDB(String fileName, String fileHash, LocalDateTime uploadDate,
                                  String fileType, long fileSize, String encryptionKey) throws IOException {
        try {
            String sql = "INSERT INTO PersonalDocuments (file_name, file_hash, upload_date, modify_date, owner_id, file_type, file_size, encryption_key) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, fileName);
                stmt.setString(2, fileHash);
                stmt.setTimestamp(3, uploadDate != null ? Timestamp.valueOf(uploadDate) : null);
                stmt.setTimestamp(4, uploadDate != null ? Timestamp.valueOf(uploadDate) : null);
                stmt.setInt(5, userId);
                stmt.setString(6, fileType);
                stmt.setLong(7, fileSize);
                stmt.setString(8, encryptionKey);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
        }
    }

    public void storeFileInDatabase(PersonalDocument document, Path filePath) throws IOException {
        try {
            String fileHash = encryptionHandler.calculateFileHash(filePath);
            String sql = "INSERT INTO PersonalDocuments (file_name, file_hash, upload_date, owner_id, file_type, file_size, encryption_key) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, document.getFileName());
                stmt.setString(2, fileHash);
                LocalDateTime uploadDate = document.getUploadDate() != null ? document.getUploadDate() : LocalDateTime.now();
                stmt.setTimestamp(3, Timestamp.valueOf(uploadDate));
                stmt.setInt(4, userId);
                stmt.setString(5, document.getNumberType());
                stmt.setLong(6, document.getSize());
                stmt.setString(7, null);
                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    sendResponse("FILE_LOG_SUCCESS");
                } else {
                    sendResponse("FILE_LOG_FAILED");
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
        }
    }

    public void handleListDocuments(String searchTerm) throws IOException {
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
                        document.add(String.valueOf(rs.getInt("docID")));
                        document.add(rs.getString("file_name") != null ? rs.getString("file_name") : "Unknown");
                        document.add(rs.getString("file_type") != null ? rs.getString("file_type") : "Unknown");
                        Timestamp uploadTimestamp = rs.getTimestamp("upload_date");
                        String uploadDateStr = uploadTimestamp != null
                                ? uploadTimestamp.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        document.add(uploadDateStr);
                        document.add(String.valueOf(rs.getLong("file_size")));
                        documents.add(document);
                    }
                }
            }
            output.writeObject(documents);
            output.flush();
            sendResponse("LIST_SUCCESS");
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
        }
    }

    public void handleDocumentDownload() throws IOException {
        try {
            int docID = (Integer) input.readObject();
            String sql = "SELECT file_name, file_type, file_size, encryption_key FROM PersonalDocuments WHERE docID = ? AND owner_id = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, docID);
                stmt.setInt(2, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String fileName = rs.getString("file_name");
                        String encryptionKey = rs.getString("encryption_key");
                        Path userDir = userHandler.getUserDirectory();
                        Path filePath = userDir.resolve(fileName);
                        if (!Files.exists(filePath)) {
                            sendError("FILE_NOT_FOUND");
                            return;
                        }
                        byte[] fileContent = Files.readAllBytes(filePath);
                        List<Object> responseData = new ArrayList<>();
                        responseData.add(fileName);
                        responseData.add(encryptionKey != null ? encryptionKey : "");
                        responseData.add(fileContent);
                        output.writeObject(responseData);
                        output.flush();
                        sendResponse("DOWNLOAD_SUCCESS");
                    } else {
                        sendError("DOCUMENT_NOT_FOUND");
                    }
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
        }
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

    private boolean isConnectionClosed() {
        try {
            return dbConnection.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }

    private void sendResponse(String response) throws IOException {
        output.writeObject(response);
        output.flush();
    }

    private void sendError(String error) throws IOException {
        sendResponse("ERROR: " + error);
    }
}