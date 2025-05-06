package com.emi.projetintegre.controllers;

import com.emi.projetintegre.client.ClientSocketManager;
import com.emi.projetintegre.models.PersonalDocument;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import javafx.scene.Node;

public class FileActionMenuController {

    private PersonalDocument document;
    private ClientSocketManager clientSocketManager;
    private Runnable onActionComplete;

    public void setDocument(PersonalDocument document) {
        this.document = document;
    }

    public void setClientSocketManager(ClientSocketManager clientSocketManager) {
        this.clientSocketManager = clientSocketManager;
    }

    public void setOnActionComplete(Runnable onActionComplete) {
        this.onActionComplete = onActionComplete;
    }

    @FXML
    private void handleOpen(MouseEvent event) {
        if (document != null) {
            System.out.println("Opening file: " + document.getFileName());
            // TODO: Implement file opening logic
        }
        closePopup(event);
    }

    @FXML
    private void handleDownload(MouseEvent event) {
        if (document != null && clientSocketManager != null) {
            System.out.println("Downloading file: " + document.getFileName());
            // TODO: Implement download logic using clientSocketManager
        }
        closePopup(event);
    }

    @FXML
    private void handleShare(MouseEvent event) {
        if (document != null) {
            System.out.println("Sharing file: " + document.getFileName());
            // TODO: Implement sharing logic
        }
        closePopup(event);
    }

    @FXML
    private void handleEncrypt(MouseEvent event) {
        if (document != null && clientSocketManager != null) {
            System.out.println("Encrypting file: " + document.getFileName());
            // TODO: Implement encryption logic
        }
        closePopup(event);
    }

    @FXML
    private void handleDecrypt(MouseEvent event) {
        if (document != null && clientSocketManager != null) {
            System.out.println("Decrypting file: " + document.getFileName());
            // TODO: Implement decryption logic
        }
        closePopup(event);
    }

    @FXML
    private void handleProperties(MouseEvent event) {
        if (document != null) {
            System.out.println("Showing properties for file: " + document.getFileName());
            // TODO: Implement properties dialog
        }
        closePopup(event);
    }

    @FXML
    private void handleDelete(MouseEvent event) {
        if (document != null && clientSocketManager != null) {
            System.out.println("Deleting file: " + document.getFileName());
            // TODO: Implement delete logic using clientSocketManager
            if (onActionComplete != null) {
                onActionComplete.run(); // Refresh tables
            }
        }
        closePopup(event);
    }

    private void closePopup(MouseEvent event) {
        Node source = (Node) event.getSource();
        Popup popup = (Popup) source.getScene().getWindow();
        popup.hide();
    }
}