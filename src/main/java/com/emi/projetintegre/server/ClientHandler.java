package com.emi.projetintegre.server;

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

import com.emi.projetintegre.models.PersonalDocument;

public class ClientHandler implements Runnable {
    private static final String UPLOAD_DIR = "UserFiles";
    private static final int MAX_AUTH_ATTEMPTS = 3;
    private static final int BUFFER_SIZE = 4096;

    private final Socket clientSocket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private Connection dbConnection;
    private int userId = -1;
    private String username;
    private boolean isAuthenticated = false;
    private int authAttempts = 0;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(clientSocket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(clientSocket.getInputStream());

            connectToDB();
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

    private void connectToDB() {
        String url = "jdbc:mysql://localhost:3306/SecureCommDB";
        String username = "secureapp";
        String password = "THISisFUNNY&&5627";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            dbConnection = DriverManager.getConnection(url, username, password);
            System.out.println("Database connection established for client: " + clientSocket.getInetAddress());
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found!");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    private void processClientCommands() throws IOException, ClassNotFoundException {
        while (!clientSocket.isClosed()) {
            Object commandObj = input.readObject();
            System.out.println("Received command from client " + clientSocket.getInetAddress() + ": " + commandObj);
            if (!(commandObj instanceof String)) {
                sendError("INVALID_COMMAND");
                continue;
            }

            String command = (String) commandObj;
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

                case "LIST_DOCUMENTS":
                    if (!isAuthenticated) {
                        sendError("NOT_AUTHENTICATED");
                        break;
                    }
                    handleListDocuments();
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

                case "DISCONNECT":
                    System.out.println("Client " + userId + " requested disconnect");
                    return;

                default:
                    sendError("UNKNOWN_COMMAND");
            }
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
            // Check database for existing file name for this user
            String sql = "SELECT COUNT(*) FROM PersonalDocuments WHERE owner_id = ? AND file_name = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, fileName);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int count = rs.getInt(1);
                    if (count > 0) {
                        System.out.println("Duplicate file found in database: " + fileName + " for userID: " + userId);
                        return true;
                    }
                }
            }

            // Optionally, check file system as well
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
            return false;
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

            Path userDir = getUserDirectory();
            Path filePath = userDir.resolve(fileName);

            if (checkDuplicateFile(fileName)) {
                sendResponse("DUPLICATE_FILE");
                return;
            }

            if (!hasEnoughSpace(fileSize)) {
                sendResponse("INSUFFICIENT_SPACE");
                return;
            }

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
            storeDocumentInDB(fileName, fileHash, uploadDate, fileType, fileSize);

            sendResponse("UPLOAD_SUCCESS");
        } catch (Exception e) {
            sendError("UPLOAD_ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void storeDocumentInDB(String fileName, String fileHash, LocalDateTime uploadDateTime,
                                   String fileType, long fileSize) throws SQLException {
        String sql = "INSERT INTO PersonalDocuments (file_name, file_hash, upload_date, modify_date, owner_id, file_type, file_size) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, fileName);
            stmt.setString(2, fileHash);
            stmt.setTimestamp(3, uploadDateTime != null ? Timestamp.valueOf(uploadDateTime) : null);
            stmt.setTimestamp(4, uploadDateTime != null ? Timestamp.valueOf(uploadDateTime) : null);
            stmt.setInt(5, userId);
            stmt.setString(6, fileType);
            stmt.setLong(7, fileSize);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Stored document '" + fileName + "' for userID: " + userId + ", rows affected: " + rowsAffected);
        }
    }

    private void storeFileInDatabase(PersonalDocument document, Path filePath) throws IOException {
        try {
            String fileHash = calculateFileHash(filePath);
            String sql = "INSERT INTO PersonalDocuments (file_name, file_hash, upload_date, owner_id, file_type, file_size) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, document.getFileName());
                stmt.setString(2, fileHash);
                LocalDateTime uploadDate = document.getUploadDate();
                LocalDateTime uploadDateTime = uploadDate != null
                        ? uploadDate
                        : LocalDateTime.now();
                stmt.setTimestamp(3, Timestamp.valueOf(uploadDateTime));
                stmt.setInt(4, userId);
                stmt.setString(5, document.getNumberType());
                stmt.setLong(6, document.getSize());

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

    private void handleListDocuments() throws IOException {
        if (!isAuthenticated) {
            System.err.println("Unauthorized attempt to list documents for client: " + clientSocket.getInetAddress());
            sendError("NOT_AUTHENTICATED");
            return;
        }

        if (userId == -1) {
            System.err.println("No valid userId set for LIST_DOCUMENTS");
            sendError("INVALID_USER_ID");
            return;
        }

        if (dbConnection == null || isConnectionClosed()) {
            System.err.println("Database connection is invalid for LIST_DOCUMENTS, userId: " + userId);
            sendError("DATABASE_CONNECTION_ERROR");
            return;
        }

        try {
            String sql = "SELECT docID, file_name, file_type, upload_date, file_size FROM PersonalDocuments WHERE owner_id = ?";
            List<List<String>> documents = new ArrayList<>();

            System.out.println("Querying documents for userId: " + userId);
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, userId);
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
                        System.out.println("Retrieved document: ID=" + docID + ", name=" + fileName + ", type=" + fileType +
                                ", date=" + uploadDateStr + ", size=" + fileSize);
                    }
                }
            }

            System.out.println("Sending " + documents.size() + " documents to client: " + clientSocket.getInetAddress());
            output.writeObject(documents);
            output.flush();
            System.out.println("Document list sent and flushed for userId: " + userId);
            sendResponse("LIST_SUCCESS");
        } catch (SQLException e) {
            System.err.println("SQL error in LIST_DOCUMENTS for userId " + userId + ": " + e.getMessage() +
                    ", SQLState: " + e.getSQLState() + ", ErrorCode: " + e.getErrorCode());
            e.printStackTrace();
            sendError("DATABASE_ERROR: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IO error in LIST_DOCUMENTS for userId " + userId + ": " + e.getMessage());
            throw e;
        }
    }

    private boolean isConnectionClosed() {
        try {
            return dbConnection.isClosed();
        } catch (SQLException e) {
            System.err.println("Error checking database connection state: " + e.getMessage());
            return true;
        }
    }

    private void handleDocumentDownload() throws IOException {
        sendResponse("DOWNLOAD_NOT_IMPLEMENTED");
    }

    private Path getUserDirectory() throws IOException {
        Path userDir = Paths.get("/mnt/Shared_Folder/UserFiles", getUsername());
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

        // Illegal characters for both Windows & Linux
        Pattern illegalChars = Pattern.compile(
            "[\\\\/:*?\"<>|]" +  // Windows reserved chars
            "|[-\u001F]" +  // Control chars (ASCII 0-31)
            "|^[.\\s]+$"          // Filename cannot be just dots/spaces
        );

        // Reserved filenames (Windows: CON, PRN, AUX, NUL, COM1-9, LPT1-9)
        Pattern reservedNames = Pattern.compile(
            "^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?$",
            Pattern.CASE_INSENSITIVE
        );

        // Check for path traversal ("../", "..\", etc.)
        boolean containsPathTraversal = fileName.contains("..") || 
                                       fileName.contains("./") || 
                                       fileName.contains(".\\");

        // Check for invalid characters, reserved names, and path traversal
        return !illegalChars.matcher(fileName).find() &&
               !reservedNames.matcher(fileName).matches() &&
               !containsPathTraversal &&
               !fileName.endsWith(".") &&  // Windows: cannot end with a dot
               !fileName.endsWith(" ");    // Windows: cannot end with a space
    }

    private boolean hasEnoughSpace(long requiredSize) {
        File uploadDir = new File(UPLOAD_DIR);
        return uploadDir.getUsableSpace() > requiredSize * 1.1;
    }

    private void sendResponse(String response) throws IOException {
        System.out.println("Sending response to client " + clientSocket.getInetAddress() + ": " + response);
        output.writeObject(response);
        output.flush();
    }

    private void sendError(String error) throws IOException {
        System.out.println("Sending error to client " + clientSocket.getInetAddress() + ": " + error);
        sendResponse("ERROR: " + error);
    }

    private void cleanupResources() {
        try {
            if (output != null) output.close();
            if (input != null) input.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            if (dbConnection != null && !dbConnection.isClosed()) dbConnection.close();
            System.out.println("Client " + userId + " resources cleaned up");
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
}