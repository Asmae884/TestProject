package com.emi.projetintegre.server;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class UserHandler {
    private static final String UPLOAD_DIR = "/mnt/Shared_Folder/UserFiles";
    private static final int MAX_AUTH_ATTEMPTS = 3;

    private final Connection dbConnection;
    private final ObjectOutputStream output;
    private final ObjectInputStream input;
    private String username;
    private int userId;
    private int authAttempts;

    public UserHandler(Connection dbConnection, ObjectOutputStream output, ObjectInputStream input, int userId) {
        this.dbConnection = dbConnection;
        this.output = output;
        this.input = input;
        this.userId = userId;
        this.authAttempts = 0;
    }

    public boolean handleAuthenticate() throws IOException, ClassNotFoundException {
        if (authAttempts >= MAX_AUTH_ATTEMPTS) {
            sendError("MAX_AUTH_ATTEMPTS_EXCEEDED");
            return false;
        }
        String[] credentials = (String[]) input.readObject();
        if (authenticate(credentials[0], credentials[1])) {
            authAttempts = 0;
            sendResponse("AUTH_SUCCESS");
            return true;
        } else {
            authAttempts++;
            sendResponse("AUTH_FAIL");
            return false;
        }
    }

    private boolean authenticate(String login, String password) {
        try (PreparedStatement stmt = dbConnection.prepareStatement(
                "SELECT userID, hashed_password, login FROM Users WHERE login = ?")) {
            stmt.setString(1, login);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    if (rs.getString("hashed_password").equals(password)) {
                        userId = rs.getInt("userID");
                        username = rs.getString("login");
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
        return false;
    }

    public void handleListUsers() throws IOException {
        if (userId == -1) {
            sendError("INVALID_USER_ID");
            return;
        }
        if (dbConnection == null || isConnectionClosed()) {
            sendError("DATABASE_CONNECTION_ERROR");
            return;
        }
        try {
            String sql = "SELECT userID, login FROM Users WHERE login != ?";
            List<List<String>> users = new ArrayList<>();
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        List<String> user = new ArrayList<>();
                        user.add(String.valueOf(rs.getInt("userID")));
                        user.add(rs.getString("login") != null ? rs.getString("login") : "Unknown");
                        users.add(user);
                    }
                }
            }
            output.writeObject(users);
            output.flush();
            sendResponse("USERS_LIST_SUCCESS");
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
        }
    }

    public Path getUserDirectory() throws IOException {
        Path userDir = Paths.get(UPLOAD_DIR, getUsername());
        Files.createDirectories(userDir);
        return userDir;
    }

    public String getUsername() {
        if (username != null && !username.isEmpty()) {
            return username;
        }
        try (PreparedStatement stmt = dbConnection.prepareStatement("SELECT login FROM Users WHERE userID = ?")) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    username = rs.getString("login");
                    return username;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching username: " + e.getMessage());
        }
        return "unknown_user";
    }

    public boolean hasEnoughSpace(long requiredSize) {
        File uploadDir = new File(UPLOAD_DIR);
        return uploadDir.getUsableSpace() > requiredSize * 1.1;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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