package com.emi.projetintegre.client;

import com.emi.projetintegre.controllers.DocumentController;
import com.emi.projetintegre.models.PersonalDocument;
import com.emi.projetintegre.models.User;
import com.emi.projetintegre.models.UserPermission;
import com.emi.projetintegre.services.FileSecurity;
import javafx.collections.ObservableList;
import com.emi.projetintegre.models.AccessLevel;
import com.emi.projetintegre.models.Client;
import javafx.collections.FXCollections;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

public class ClientSocketManager implements CommunicationManager {
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private boolean isAuthenticated;
    private DocumentManager documentManager;
    private DocumentController documentController;
    private AuthenticationManager authenticationManager;

    public ClientSocketManager() {
        this.socket = null;
        this.isAuthenticated = false;
        initializeManagers();
    }

    private void initializeManagers() {
        Function<Object, Boolean> sendFunction = this::send;
        Function<Void, Boolean> isConnectedFunction = _ -> isConnected();
        Function<Void, Boolean> isAuthenticatedFunction = _ -> isAuthenticated;
        FileSecurity fileSecurity = new FileSecurity();

        this.documentManager = new DocumentManager(
            () -> outputStream,
            () -> inputStream,
            sendFunction,
            isConnectedFunction,
            isAuthenticatedFunction,
            fileSecurity
        );
        this.authenticationManager = new AuthenticationManager(
            () -> outputStream,
            () -> inputStream,
            sendFunction,
            isConnectedFunction
        );
        this.documentController = new DocumentController(documentManager);
    }

    @Override
    public void connect() {
        try {
            if (this.socket != null && !this.socket.isClosed()) {
                disconnect();
            }
            
            String serverAddress = "192.168.1.104";
            int port = 3000;
            
            this.socket = new Socket();
            this.socket.connect(new java.net.InetSocketAddress(serverAddress, port), 5000);
            this.socket.setSoTimeout(30000);
            
            outputStream = new ObjectOutputStream(this.socket.getOutputStream());
            inputStream = new ObjectInputStream(this.socket.getInputStream());
            
            System.out.println("Connected to the server...");
            initializeManagers();
        } catch (Exception e) {
            disconnect();
            System.err.println("Failed to connect to the server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean send(Object data) {
        try {
            if (isConnected()) {
                outputStream.writeObject(data);
                outputStream.flush();
                return true;
            }
            System.err.println("Cannot send data: not connected");
            return false;
        } catch (Exception e) {
            System.err.println("Send error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public String checkDuplicateFile(String fileName) {
        return documentManager.checkDuplicateFile(fileName);
    }

    public boolean uploadDocument(String filePath, String fileName) {
        return documentController.uploadDocument(filePath, fileName);
    }

    public boolean uploadDocument(String filePath) {
        return documentController.uploadDocument(filePath);
    }

    public boolean uploadEncryptedDocument(String filePath, String fileName) {
        return documentController.uploadEncryptedDocument(filePath, fileName);
    }

    public boolean uploadEncryptedDocument(String filePath) {
        return documentController.uploadEncryptedDocument(filePath);
    }

    public ObservableList<PersonalDocument> getListDocuments() {
        return documentController.getDocuments(null);
    }

    public ObservableList<PersonalDocument> getListDocuments(String query) {
        return documentController.getDocuments(query);
    }
    
    public ObservableList<PersonalDocument> getListSharedWithMeDocuments() {
        return documentController.getSharedWithMeDocuments(null);
    }

    public ObservableList<PersonalDocument> getListSharedWithMeDocuments(String query) {
        return documentController.getSharedWithMeDocuments(query);
    }

    public void showDocuments() {
        documentController.showDocuments(null);
    }

    public void showDocuments(String query) {
        documentController.showDocuments(query);
    }
    
    public void showDocumentsSharedWithMe() {
        documentController.showDocumentsSharedWithMe(null);
    }

    public void showDocumentsSharedWithMe(String query) {
        documentController.showDocumentsSharedWithMe(query);
    }

    public boolean downloadDocument(int docID, String saveDirectory) {
        return documentController.downloadDocument(docID, saveDirectory);
    }

    public Object[] getDocumentContent(int docID) {
        try {
            if (!isConnected() || !isAuthenticated) {
                System.err.println("Cannot get document content: not connected or not authenticated");
                return null;
            }
            // Send command
            outputStream.writeObject("GET_DOCUMENT_CONTENT:" + docID);
            outputStream.flush();
            System.out.println("Sent GET_DOCUMENT_CONTENT:" + docID + " to server");

            // Read response objects in order
            Object isEncryptedObj = inputStream.readObject();
            if (!(isEncryptedObj instanceof Boolean)) {
                System.err.println("Unexpected isEncrypted type: " + (isEncryptedObj != null ? isEncryptedObj.getClass().getName() : "null"));
                clearInputStream();
                return null;
            }
            boolean isEncrypted = (Boolean) isEncryptedObj;

            Object fileTypeObj = inputStream.readObject();
            if (!(fileTypeObj instanceof String)) {
                System.err.println("Unexpected fileType type: " + (fileTypeObj != null ? fileTypeObj.getClass().getName() : "null"));
                clearInputStream();
                return null;
            }
            String fileType = (String) fileTypeObj;

            Object fileContentObj = inputStream.readObject();
            if (!(fileContentObj instanceof byte[])) {
                System.err.println("Unexpected fileContent type: " + (fileContentObj != null ? fileContentObj.getClass().getName() : "null"));
                clearInputStream();
                return null;
            }
            byte[] fileContent = (byte[]) fileContentObj;

            Object finalResponse = inputStream.readObject();
            if (finalResponse instanceof String) {
                String responseStr = (String) finalResponse;
                if (responseStr.equals("CONTENT_SUCCESS")) {
                    System.out.println("Retrieved content for docID " + docID + ", encrypted: " + isEncrypted + ", type: " + fileType);
                    return new Object[]{fileContent, fileType};
                } else if (responseStr.startsWith("ERROR:")) {
                    System.err.println("Failed to get content for docID " + docID + ": " + responseStr.substring(6));
                    return null;
                } else {
                    System.err.println("Unexpected final response: " + responseStr);
                    return null;
                }
            } else {
                System.err.println("Unexpected final response type: " + (finalResponse != null ? finalResponse.getClass().getName() : "null"));
                clearInputStream();
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error getting document content for docID " + docID + ": " + e.getMessage());
            e.printStackTrace();
            clearInputStream();
            return null;
        }
    }
    
    public boolean updateDocumentContent(int docID, byte[] content, String fileType) {
        try {
            if (!isConnected() || !isAuthenticated) {
                System.err.println("Cannot update document content: not connected or not authenticated");
                return false;
            }

            // Send command and data
            outputStream.writeObject("UPDATE_DOCUMENT_CONTENT:" + docID);
            outputStream.writeObject(fileType);
            outputStream.writeObject(content);
            outputStream.flush();
            System.out.println("Sent UPDATE_DOCUMENT_CONTENT:" + docID + " with fileType: " + fileType + " to server");

            // Read response
            Object response = inputStream.readObject();
            if (response instanceof String) {
                String responseStr = (String) response;
                if (responseStr.equals("UPDATE_SUCCESS")) {
                    System.out.println("Successfully updated content for docID: " + docID);
                    return true;
                } else if (responseStr.startsWith("ERROR:")) {
                    System.err.println("Failed to update content for docID " + docID + ": " + responseStr.substring(6));
                    return false;
                } else {
                    System.err.println("Unexpected response: " + responseStr);
                    return false;
                }
            } else {
                System.err.println("Unexpected response type: " + (response != null ? response.getClass().getName() : "null"));
                clearInputStream();
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error updating document content for docID " + docID + ": " + e.getMessage());
            e.printStackTrace();
            clearInputStream();
            return false;
        }
    }

	// Helper method to fetch encryption key
	private String getEncryptionKey(int docID) {
	    try {
	        outputStream.writeObject("GET_ENCRYPTION_KEY:" + docID);
	        outputStream.flush();
	        // Clear stream to avoid pollution
	        clearInputStream();
	        Object response = inputStream.readObject();
	        System.out.println("Raw server response for encryption key: [" + response + "]");
	        if (response instanceof String) {
	            String responseStr = (String) response;
	            if (responseStr.startsWith("KEY:")) {
	                String key = responseStr.substring(4).trim(); // Trim to remove whitespace
	                System.out.println("Extracted encryption key: [" + key + "], length: " + key.length());
	                // Validate Base64 format
	                if (!key.matches("^[A-Za-z0-9+/=]+$")) {
	                    System.err.println("Invalid Base64 key format: [" + key + "]");
	                    return null;
	                }
	                // Check expected Base64 length for AES keys (16, 24, or 32 bytes after decoding)
	                int expectedLength = key.length() % 4 == 0 ? key.length() : key.length() + (4 - key.length() % 4);
	                if (key.length() != expectedLength) {
	                    System.err.println("Unexpected Base64 key length: " + key.length());
	                    return null;
	                }
	                return key;
	            } else if (responseStr.startsWith("ERROR:")) {
	                System.err.println("Failed to get encryption key: " + responseStr.substring(6));
	                return null;
	            }
	        }
	        System.err.println("Unexpected response type for encryption key: " + (response != null ? response.getClass().getName() : "null"));
	        return null;
	    } catch (Exception e) {
	        System.err.println("Error fetching encryption key for docID " + docID + ": " + e.getMessage());
	        e.printStackTrace();
	        clearInputStream();
	        return null;
	    }
	}
	
	// Helper method to clear input stream
	private void clearInputStream() {
	    try {
	        while (inputStream.available() > 0) {
	            inputStream.readObject();
	        }
	    } catch (Exception e) {
	        System.err.println("Error clearing input stream: " + e.getMessage());
	    }
	}

	private String decryptFileLocal(String encryptedContent, String encryptionKey) {
    try {
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            System.err.println("Encryption key is null or empty");
            return null;
        }
        System.out.println("Decrypting with key: [" + encryptionKey + "], length: " + encryptionKey.length());
        // Validate Base64 string
        if (!encryptionKey.matches("^[A-Za-z0-9+/=]+$")) {
            System.err.println("Invalid Base64 key format: [" + encryptionKey + "]");
            return null;
        }
        // Decode the Base64-encoded encryption key
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
        System.out.println("Decoded key length: " + keyBytes.length);
        // Validate AES key length
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            System.err.println("Invalid AES key length: " + keyBytes.length);
            return null;
        }
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

        // Create cipher instance
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // Decode and decrypt the content
        if (!encryptedContent.matches("^[A-Za-z0-9+/=]+$")) {
            System.err.println("Invalid Base64 content format");
            return null;
        }
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedContent);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
        System.err.println("Base64 decoding error: " + e.getMessage() + ", key: [" + encryptionKey + "]");
        e.printStackTrace();
        return null;
    } catch (Exception e) {
        System.err.println("Error decrypting content: " + e.getMessage() + ", key: [" + encryptionKey + "]");
        e.printStackTrace();
        return null;
    }
}
    
	public boolean encryptFile(int docID) {
        try {
            if (!isConnected() || !isAuthenticated) {
                System.err.println("Cannot encrypt file: not connected or not authenticated");
                return false;
            }
            send("ENCRYPT_FILE:" + docID);
            System.out.println("Sent ENCRYPT_FILE:" + docID + " to server");
            Object response = inputStream.readObject();
            System.out.println("Received response from server: " + (response != null ? response.toString() : "null"));
            if (response instanceof String) {
                String responseStr = (String) response;
                if (responseStr.equals("ENCRYPT_SUCCESS")) {
                    System.out.println("File encryption successful for docID: " + docID);
                    return true;
                } else if (responseStr.startsWith("ERROR:")) {
                    String errorMsg = responseStr.substring(6).trim(); // Extract error message
                    System.err.println("Encryption failed for docID " + docID + ": " + errorMsg);
                    return false;
                } else {
                    System.err.println("Unexpected response: " + responseStr);
                    return false;
                }
            } else {
                System.err.println("Unexpected response type: " + (response != null ? response.getClass().getName() : "null"));
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error encrypting file for docID " + docID + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean decryptFile(int docID) {
        try {
            if (!isConnected() || !isAuthenticated) {
                System.err.println("Cannot decrypt file: not connected or not authenticated");
                return false;
            }
            send("DECRYPT_FILE:" + docID);
            System.out.println("Sent DECRYPT_FILE:" + docID + " to server");
            Object response = inputStream.readObject();
            System.out.println("Received response from server: " + (response != null ? response.toString() : "null"));
            if (response instanceof String) {
                String responseStr = (String) response;
                if (responseStr.equals("DECRYPT_SUCCESS")) {
                    System.out.println("File decryption successful for docID: " + docID);
                    return true;
                } else if (responseStr.startsWith("ERROR:")) {
                    String errorMsg = responseStr.substring(6).trim(); // Extract error message
                    System.err.println("Decryption failed for docID " + docID + ": " + errorMsg);
                    return false;
                } else {
                    System.err.println("Unexpected response: " + responseStr);
                    return false;
                }
            } else {
                System.err.println("Unexpected response type: " + (response != null ? response.getClass().getName() : "null"));
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error decrypting file for docID " + docID + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendCredentials(String login, String password) {
        boolean authenticated = authenticationManager.authenticate(login, password);
        if (authenticated) {
            isAuthenticated = true;
        }
        return authenticated;
    }

    public ObservableList<Client> getListUsers() {
        ObservableList<Client> users = FXCollections.observableArrayList();
    
        if (!isConnected() || !isAuthenticated) {
            System.err.println("Cannot get users: not connected or not authenticated");
            return users;
        }
    
        try {
            send("LIST_USERS");
            outputStream.flush();
    
            Object response = inputStream.readObject();
            if (response instanceof List) {
                @SuppressWarnings("unchecked")
                List<List<String>> userList = (List<List<String>>) response;
                for (List<String> userData : userList) {
                    if (userData.size() < 2) {
                        continue;
                    }
    
                    int userID;
                    try {
                        userID = Integer.parseInt(userData.get(0));
                    } catch (NumberFormatException e) {
                        continue;
                    }
    
                    String login = userData.get(1);
    
                    Client client = new Client(
                        userID,
                        login != null ? login : "Unknown",
                        null, // password
                        null, // creationDatePassword
                        null, // validityDatePassword
                        null, // creationDate
                        null, // validityDate
                        null  // cle
                    );
                    users.add(client);
                }
            }
    
            Object status = inputStream.readObject();
            if (!(status instanceof String) || !status.equals("USERS_LIST_SUCCESS")) {
                System.err.println("Unexpected status: " + (status instanceof String ? status : "Invalid response type"));
                return users;
            }
            return users;
        } catch (Exception e) {
            System.err.println("List users error: " + e.getMessage());
            e.printStackTrace();
            return users;
        }
    }

    public ObservableList<Client> getListUsersShare(int docId) {
        ObservableList<Client> users = FXCollections.observableArrayList();
    
        if (!isConnected() || !isAuthenticated) {
            System.err.println("Cannot get shared users: not connected or not authenticated");
            return users;
        }
    
        try {
            send("LIST_USERS_SHARE:" + docId);
            outputStream.flush();
    
            Object response = inputStream.readObject();
            if (response instanceof List) {
                @SuppressWarnings("unchecked")
                List<List<String>> userList = (List<List<String>>) response;
                for (List<String> userData : userList) {
                    if (userData.size() < 2) {
                        continue;
                    }
    
                    int userID;
                    try {
                        userID = Integer.parseInt(userData.get(0));
                    } catch (NumberFormatException e) {
                        continue;
                    }
    
                    String login = userData.get(1);
    
                    Client client = new Client(
                        userID,
                        login != null ? login : "Unknown",
                        null, // password
                        null, // creationDatePassword
                        null, // validityDatePassword
                        null, // creationDate
                        null, // validityDate
                        null  // cle
                    );
                    users.add(client);
                }
            }
    
            Object status = inputStream.readObject();
            if (!(status instanceof String) || !status.equals("USERS_SHARE_LIST_SUCCESS")) {
                System.err.println("Unexpected status: " + (status instanceof String ? status : "Invalid response type"));
                return users;
            }
            return users;
        } catch (Exception e) {
            System.err.println("List shared users error: " + e.getMessage());
            e.printStackTrace();
            return users;
        }
    }

    public List<UserPermission> getDocumentPermissions(int documentId) {
        List<UserPermission> permissions = FXCollections.observableArrayList();
    
        if (!isConnected() || !isAuthenticated) {
            System.err.println("Cannot get document permissions: not connected or not authenticated");
            return permissions;
        }
    
        try {
            send("GET_DOCUMENT_PERMISSIONS:" + documentId);
            outputStream.flush();
    
            Object response = inputStream.readObject();
            if (response instanceof List) {
                @SuppressWarnings("unchecked")
                List<List<String>> permissionList = (List<List<String>>) response;
                for (List<String> permData : permissionList) {
                    if (permData.size() < 6) {
                        System.err.println("Invalid permission data format: " + permData);
                        continue;
                    }
    
                    try {
                        int userId = Integer.parseInt(permData.get(0));
                        String login = permData.get(1);
                        boolean canRead = Boolean.parseBoolean(permData.get(2));
                        boolean canWrite = Boolean.parseBoolean(permData.get(3));
                        boolean canDownload = Boolean.parseBoolean(permData.get(4));
                        boolean canDelete = Boolean.parseBoolean(permData.get(5));
                        int accessLevelId = determineAccessLevelId(canRead, canWrite, canDownload, canDelete);
                        AccessLevel accessLevel = AccessLevel.fromId(accessLevelId);
    
                        User user = new User(userId, login, null, null, null);
                        UserPermission permission = new UserPermission(user, accessLevel, null);
                        permissions.add(permission);
                    } catch (Exception e) {
                        System.err.println("Error parsing permission data: " + permData + ", " + e.getMessage());
                        continue;
                    }
                }
            }
    
            Object status = inputStream.readObject();
            if (!(status instanceof String) || !status.equals("PERMISSIONS_LIST_SUCCESS")) {
                System.err.println("Unexpected status: " + (status instanceof String ? status : "Invalid response type"));
                return permissions;
            }
    
            return permissions;
        } catch (Exception e) {
            System.err.println("Error retrieving document permissions: " + e.getMessage());
            e.printStackTrace();
            return permissions;
        }
    }

    public boolean shareDocument(int docId, int userId, int accessLevelId) {
        try {
            if (!isConnected() || !isAuthenticated) {
                System.err.println("Cannot share document: not connected or not authenticated");
                return false;
            }
            send("SHARE_DOCUMENT:" + docId + ":" + userId + ":" + accessLevelId);
            Object response = inputStream.readObject();
            if (response instanceof String && response.equals("SHARE_SUCCESS")) {
                return true;
            } else {
                System.err.println("Share document failed: " + (response instanceof String ? response : "Unexpected response"));
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error sharing document: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeShare(int docId, int userId) {
        try {
            if (!isConnected() || !isAuthenticated) {
                System.err.println("Cannot remove share: not connected or not authenticated");
                return false;
            }
            send("REMOVE_SHARE:" + docId + ":" + userId);
            Object response = inputStream.readObject();
            if (response instanceof String && response.equals("REMOVE_SUCCESS")) {
                return true;
            } else {
                System.err.println("Remove share failed: " + (response instanceof String ? response : "Unexpected response"));
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error removing share: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private int determineAccessLevelId(boolean read, boolean write, boolean download, boolean delete) {
        if (read && write && download && delete) return 7;  // ALL_PERMISSIONS
        if (read && write && delete) return 6;             // READ_WRITE_DELETE
        if (read && write && download) return 5;           // READ_WRITE_DOWNLOAD
        if (read && delete) return 4;                      // READ_DELETE
        if (read && download) return 3;                    // READ_DOWNLOAD
        if (read && write) return 2;                       // READ_WRITE
        return 1;                                          // READ_ONLY
    }
    
    public boolean deleteDocument(int docID) {
        try {
            if (!isConnected() || !isAuthenticated) {
                System.err.println("Cannot delete file: not connected or not authenticated");
                return false;
            }
            send("DELETE_DOCUMENT:" + docID);
            System.out.println("Sent DELETE_DOCUMENT:" + docID + " to server");
            Object response = inputStream.readObject();
            System.out.println("Received response from server: " + (response != null ? response.toString() : "null"));
            if (response instanceof String) {
                String responseStr = (String) response;
                if (responseStr.equals("DELETE_SUCCESS")) {
                    System.out.println("File deletion successful for docID: " + docID);
                    return true;
                } else if (responseStr.startsWith("ERROR:")) {
                    String errorMsg = responseStr.substring(6).trim(); // Extract error message
                    System.err.println("Deletion failed for docID " + docID + ": " + errorMsg);
                    return false;
                } else {
                    System.err.println("Unexpected response: " + responseStr);
                    return false;
                }
            } else {
                System.err.println("Unexpected response type: " + (response != null ? response.getClass().getName() : "null"));
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error deleting file for docID " + docID + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean permanentDeleteDocuments(List<Integer> docIDs) {
        try {
            if (!isConnected() || !isAuthenticated) {
                System.err.println("Cannot permanently delete documents: not connected or not authenticated");
                return false;
            }

            // Send command with list of document IDs
            send("PERMANENT_DELETE_DOCUMENTS");
            outputStream.writeObject(docIDs);
            outputStream.flush();
            System.out.println("Sent PERMANENT_DELETE_DOCUMENTS for docIDs: " + docIDs);

            // Read response
            Object response = inputStream.readObject();
            if (response instanceof String) {
                String responseStr = (String) response;
                if (responseStr.equals("PERMANENT_DELETE_SUCCESS")) {
                    System.out.println("Documents permanently deleted successfully: " + docIDs);
                    return true;
                } else if (responseStr.startsWith("ERROR:")) {
                    String errorMsg = responseStr.substring(6).trim();
                    System.err.println("Permanent Delete failed for docIDs " + docIDs + ": " + errorMsg);
                    return false;
                } else {
                    System.err.println("Unexpected response: " + responseStr);
                    return false;
                }
            } else {
                System.err.println("Unexpected response type: " + (response != null ? response.getClass().getName() : "null"));
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error Permanently deleting documents for docIDs " + docIDs + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean restoreDocuments(List<Integer> docIDs) {
        try {
            if (!isConnected() || !isAuthenticated) {
                System.err.println("Cannot restore documents: not connected or not authenticated");
                return false;
            }

            // Send command with list of document IDs
            send("RESTORE_DOCUMENTS");
            outputStream.writeObject(docIDs);
            outputStream.flush();
            System.out.println("Sent RESTORE_DOCUMENTS for docIDs: " + docIDs);

            // Read response
            Object response = inputStream.readObject();
            if (response instanceof String) {
                String responseStr = (String) response;
                if (responseStr.equals("RESTORE_SUCCESS")) {
                    System.out.println("Documents restored successfully: " + docIDs);
                    return true;
                } else if (responseStr.startsWith("ERROR:")) {
                    String errorMsg = responseStr.substring(6).trim();
                    System.err.println("Restore failed for docIDs " + docIDs + ": " + errorMsg);
                    return false;
                } else {
                    System.err.println("Unexpected response: " + responseStr);
                    return false;
                }
            } else {
                System.err.println("Unexpected response type: " + (response != null ? response.getClass().getName() : "null"));
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error restoring documents for docIDs " + docIDs + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public ObservableList<PersonalDocument> getListDeletedDocuments(String query) {
        ObservableList<PersonalDocument> documents = FXCollections.observableArrayList();
        if (!isConnected() || !isAuthenticated) {
            System.err.println("Cannot get deleted documents: not connected or not authenticated");
            return documents;
        }

        try {
            // Send command
            send("LIST_DELETED_DOCUMENTS");
            outputStream.writeUTF(query != null ? query : "");
            outputStream.flush();
            System.out.println("Sent LIST_DELETED_DOCUMENTS with query: " + (query != null ? query : "null"));

            // Read response
            Object response = inputStream.readObject();
            if (response instanceof List) {
                @SuppressWarnings("unchecked")
                List<List<String>> docList = (List<List<String>>) response;
                for (List<String> docData : docList) {
                    if (docData.size() < 5) {
                        System.err.println("Invalid document data: " + docData);
                        continue;
                    }

                    try {
                        int docID = Integer.parseInt(docData.get(0));
                        String fileName = docData.get(1);
                        String fileType = docData.get(2);
                        LocalDateTime uploadDate = LocalDateTime.parse(docData.get(3), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        long fileSize = Long.parseLong(docData.get(4));

                        // Use the new constructor
                        PersonalDocument document = new PersonalDocument(docID, fileName, fileType, uploadDate, fileSize);
                        documents.add(document);
                    } catch (Exception e) {
                        System.err.println("Error parsing document data: " + docData + ", " + e.getMessage());
                        continue;
                    }
                }
            }

            Object status = inputStream.readObject();
            if (!(status instanceof String) || !status.equals("LIST_DELETED_SUCCESS")) {
                System.err.println("Unexpected status: " + (status instanceof String ? status : "Invalid response type"));
            }

            System.out.println("Retrieved " + documents.size() + " deleted documents");
            return documents;
        } catch (Exception e) {
            System.err.println("Error retrieving deleted documents: " + e.getMessage());
            e.printStackTrace();
            return documents;
        }
    }

    public boolean saveDocumentContent(int docID, String content) {
        try {
            if (!isConnected() || !isAuthenticated) {
                System.err.println("Cannot save document content: not connected or not authenticated");
                return false;
            }

            // Send command and data
            outputStream.writeObject("SAVE_DOCUMENT_CONTENT:" + docID);
            outputStream.writeObject(content);
            outputStream.flush();
            System.out.println("Sent SAVE_DOCUMENT_CONTENT:" + docID + " to server");

            // Read response
            Object response = inputStream.readObject();
            if (response instanceof String) {
                String responseStr = (String) response;
                if (responseStr.equals("SAVE_SUCCESS")) {
                    System.out.println("Successfully saved content for docID: " + docID);
                    return true;
                } else if (responseStr.startsWith("ERROR:")) {
                    System.err.println("Failed to save content for docID " + docID + ": " + responseStr.substring(6));
                    return false;
                } else {
                    System.err.println("Unexpected response: " + responseStr);
                    return false;
                }
            } else {
                System.err.println("Unexpected response type: " + (response != null ? response.getClass().getName() : "null"));
                clearInputStream();
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error saving document content for docID " + docID + ": " + e.getMessage());
            e.printStackTrace();
            clearInputStream();
            return false;
        }
    }
    
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed() && outputStream != null) {
                try {
                    send("DISCONNECT");
                } catch (Exception e) {
                    System.err.println("Warning: Could not send disconnect notification: " + e.getMessage());
                }
            }

            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    System.err.println("Error closing output stream: " + e.getMessage());
                }
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    System.err.println("Error closing input stream: " + e.getMessage());
                }
            }

            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
            }
        } finally {
            outputStream = null;
            inputStream = null;
            socket = null;
            isAuthenticated = false;
            initializeManagers();
        }
    }
}