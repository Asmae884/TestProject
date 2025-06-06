package com.emi.projetintegre.controllers;

import com.emi.projetintegre.client.ClientSocketManager;
import com.emi.projetintegre.models.PersonalDocument;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import javafx.scene.Node;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;


public class FileActionSharedWithMeMenuController {

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
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emi/projetintegre/views/FileViewer.fxml"));
                Scene scene = new Scene(loader.load());
                FileViewerController controller = loader.getController();
                controller.setClientSocketManager(clientSocketManager);
                controller.setDocument(document);

                Stage stage = new Stage();
                stage.setTitle("View File: " + document.getFileName());
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setScene(scene);
                stage.showAndWait();
            } catch (IOException e) {
                //showAlert("Error", "Failed to open file viewer: " + e.getMessage());
                e.printStackTrace();
            }
        }
        closePopup(event);
    }
    
    @FXML
    private void handleWrite(MouseEvent event) {
        if (document != null) {
            System.out.println("Opening file: " + document.getFileName());
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emi/projetintegre/views/FileWriter.fxml"));
                Scene scene = new Scene(loader.load());
                FileWriterController controller = loader.getController();
                controller.setClientSocketManager(clientSocketManager);
                controller.setDocument(document);

                Stage stage = new Stage();
                stage.setTitle("Modify File: " + document.getFileName());
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setScene(scene);
                stage.showAndWait();
            } catch (IOException e) {
                //showAlert("Error", "Failed to open file viewer: " + e.getMessage());
                e.printStackTrace();
            }
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