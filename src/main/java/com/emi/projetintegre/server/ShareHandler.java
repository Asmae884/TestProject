package com.emi.projetintegre.server;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShareHandler {
    private final Connection dbConnection;
    private final ObjectOutputStream output;
    private final int userId;

    public ShareHandler(Connection dbConnection, ObjectOutputStream output, int userId) {
        this.dbConnection = dbConnection;
        this.output = output;
        this.userId = userId;
    }

    public void handleShareDocument(int docId, int userIdToShare, int accessId) throws IOException {
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

            // Validate userIdToShare exists
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

            // Validate accessId
            if (accessId < 1 || accessId > 7) {
                sendError("INVALID_ACCESS_ID");
                return;
            }

            // Insert or update UserPermissions
            String sql = "INSERT INTO UserPermissions (docID, userID, accessID) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE accessID = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, docId);
                stmt.setInt(2, userIdToShare);
                stmt.setInt(3, accessId);
                stmt.setInt(4, accessId);
                stmt.executeUpdate();
                sendResponse("SHARE_SUCCESS");
            }
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
        }
    }

    public void handleRemoveShare(int docId, int userIdToRemove) throws IOException {
        if (userId == -1) {
            sendError("INVALID_USER_ID");
            return;
        }
        if (dbConnection == null || isConnectionClosed()) {
            sendError("DATABASE_CONNECTION_ERROR");
            return;
        }
        try {
            // Validate document
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

            // Validate userIdToRemove
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

            // Delete permission
            String sql = "DELETE FROM UserPermissions WHERE docID = ? AND userID = ?";
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, docId);
                stmt.setInt(2, userIdToRemove);
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    sendResponse("REMOVE_SUCCESS");
                } else {
                    sendError("NO_PERMISSION_FOUND");
                }
            }
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
        }
    }

    public void handleGetDocumentPermissions(int docId) throws IOException {
        if (userId == -1) {
            sendError("INVALID_USER_ID");
            return;
        }
        if (dbConnection == null || isConnectionClosed()) {
            sendError("DATABASE_CONNECTION_ERROR");
            return;
        }
        try {
            // Validate document
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

            // Fetch permissions
            String sql = "SELECT up.userID, u.login, up.accessID FROM UserPermissions up " +
                        "JOIN Users u ON up.userID = u.userID WHERE up.docID = ?";
            List<List<String>> permissions = new ArrayList<>();
            try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
                stmt.setInt(1, docId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        List<String> permission = new ArrayList<>();
                        permission.add(String.valueOf(rs.getInt("userID")));
                        permission.add(rs.getString("login") != null ? rs.getString("login") : "Unknown");
                        boolean[] flags = getPermissionFlags(rs.getInt("accessID"));
                        permission.add(String.valueOf(flags[0])); // canRead
                        permission.add(String.valueOf(flags[1])); // canWrite
                        permission.add(String.valueOf(flags[2])); // canDownload
                        permission.add(String.valueOf(flags[3])); // canDelete
                        permissions.add(permission);
                    }
                }
            }
            output.writeObject(permissions);
            output.flush();
            sendResponse("PERMISSIONS_LIST_SUCCESS");
        } catch (SQLException e) {
            sendError("DATABASE_ERROR: " + e.getMessage());
        }
    }

    private boolean[] getPermissionFlags(int accessId) {
        boolean canRead = false, canWrite = false, canDownload = false, canDelete = false;
        switch (accessId) {
            case 7: canRead = canWrite = canDownload = canDelete = true; break;
            case 6: canRead = canWrite = canDelete = true; break;
            case 5: canRead = canWrite = canDownload = true; break;
            case 4: canRead = canDelete = true; break;
            case 3: canRead = canDownload = true; break;
            case 2: canRead = canWrite = true; break;
            case 1: canRead = true; break;
        }
        return new boolean[]{canRead, canWrite, canDownload, canDelete};
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