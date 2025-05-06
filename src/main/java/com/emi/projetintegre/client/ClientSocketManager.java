package com.emi.projetintegre.client;

import java.io.*;
import java.net.Socket;
import java.util.function.Function;

import com.emi.projetintegre.models.PersonalDocument;
import javafx.collections.ObservableList;

public class ClientSocketManager implements CommunicationManager {
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private boolean isAuthenticated;
    private DocumentManager documentManager;
    private AuthenticationManager authenticationManager;

    public ClientSocketManager() {
        this.socket = null;
        this.isAuthenticated = false;
        initializeManagers();
    }

    private void initializeManagers() {
        Function<Object, Boolean> sendFunction = this::send;
        Function<Void, Boolean> isConnectedFunction = v -> isConnected();
        Function<Void, Boolean> isAuthenticatedFunction = v -> isAuthenticated;

        this.documentManager = new DocumentManager(
            () -> outputStream,
            () -> inputStream,
            sendFunction,
            isConnectedFunction,
            isAuthenticatedFunction
        );
        this.authenticationManager = new AuthenticationManager(
            () -> outputStream,
            () -> inputStream,
            sendFunction,
            isConnectedFunction
        );
    }

    @Override
    public void connect() {
        try {
            if (this.socket != null && !this.socket.isClosed()) {
                disconnect();
            }
            
            String serverAddress = "192.168.1.106";
            int port = 5000;
            
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
            System.out.println("Cannot send data: not connected");
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
        return documentManager.uploadDocument(filePath, fileName);
    }

    public boolean uploadDocument(String filePath) {
        return documentManager.uploadDocument(filePath);
    }

    public ObservableList<PersonalDocument> getListDocuments() {
        return documentManager.getListDocuments(null);
    }

    public ObservableList<PersonalDocument> getListDocuments(String query) {
        return documentManager.getListDocuments(query);
    }

    public void showDocuments() {
        documentManager.showDocuments(null);
    }

    public void showDocuments(String query) {
        documentManager.showDocuments(query);
    }

    public boolean downloadDocument(int docID, String saveDirectory) {
        return documentManager.downloadDocument(docID, saveDirectory);
    }

    public boolean sendCredentials(String login, String password) {
        boolean authenticated = authenticationManager.authenticate(login, password);
        if (authenticated) {
            isAuthenticated = true;
        }
        return authenticated;
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed() && outputStream != null) {
                try {
                    send("DISCONNECT");
                } catch (Exception e) {
                    System.err.println("Warning: Could not send disconnect notification");
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