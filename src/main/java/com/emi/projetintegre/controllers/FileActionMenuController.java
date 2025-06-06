package com.emi.projetintegre.controllers;

import com.emi.projetintegre.client.ClientSocketManager;
import com.emi.projetintegre.models.PersonalDocument;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import javafx.scene.Node;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.io.File;
import java.io.IOException;

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
            
            // Create a DirectoryChooser for selecting save location
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Download Location");
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            
            // Show the directory chooser dialog
            Window stage = ((Node) event.getSource()).getScene().getWindow();
            File selectedDirectory = directoryChooser.showDialog(stage);
            
            if (selectedDirectory != null) {
                String saveDirectory = selectedDirectory.getAbsolutePath();
                boolean success = clientSocketManager.downloadDocument(document.getDocID(), saveDirectory);
                
                // Show appropriate feedback to user
                Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                alert.setTitle(success ? "Download Success" : "Download Failed");
                alert.setHeaderText(null);
                alert.setContentText(success 
                    ? "File '" + document.getFileName() + "' downloaded successfully to " + saveDirectory
                    : "Failed to download file '" + document.getFileName() + "'");
                alert.showAndWait();
            } else {
                // User cancelled directory selection
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Download Cancelled");
                alert.setHeaderText(null);
                alert.setContentText("Download cancelled: No directory selected");
                alert.showAndWait();
            }
        }
        closePopup(event);
    }

    @FXML
    private void handleShare(MouseEvent event) {
        if (document != null) {
            try {
                // Load the share dialog
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emi/projetintegre/views/ShareDialog.fxml"));
                Parent root = loader.load();
                
                // Configure the controller
                ShareDialogController shareController = loader.getController();
                shareController.setDocument(document);
                shareController.setClientSocketManager(clientSocketManager);
                
                // Create and show the new stage
                Stage shareStage = new Stage();
                shareStage.setTitle("Share file: " + document.getFileName());
                shareStage.setScene(new Scene(root));
                shareStage.initModality(Modality.APPLICATION_MODAL);
                shareStage.showAndWait();
                
            } catch (IOException e) {
                System.err.println("Error loading share dialog: " + e.getMessage());
                e.printStackTrace();
            }
        }
        closePopup(event);
    }

    @FXML
    private void handleEncrypt(MouseEvent event) {
        if (document != null && clientSocketManager != null) {
            System.out.println("Encrypting file: " + document.getFileName());
            boolean success = clientSocketManager.encryptFile(document.getDocID());
            Alert alert = new Alert(success ? AlertType.INFORMATION : AlertType.ERROR);
            alert.setTitle(success ? "Encryption Successful" : "Encryption Failed");
            alert.setHeaderText(null);
            alert.setContentText(success 
                ? "File '" + document.getFileName() + "' has been encrypted successfully."
                : "Failed to encrypt file '" + document.getFileName() + "'. Please try again or check if the file is already encrypted.");
            alert.showAndWait();
            
            if (success && onActionComplete != null) {
                onActionComplete.run(); // Refresh tables
            }
        } else {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Encryption Error");
            alert.setHeaderText(null);
            alert.setContentText("Cannot encrypt file: No document selected or client not connected.");
            alert.showAndWait();
        }
        closePopup(event);
    }

    @FXML
    private void handleDecrypt(MouseEvent event) {
        if (document != null && clientSocketManager != null) {
            System.out.println("Decrypting file: " + document.getFileName());
            boolean success = clientSocketManager.decryptFile(document.getDocID());
            Alert alert = new Alert(success ? AlertType.INFORMATION : AlertType.ERROR);
            alert.setTitle(success ? "Decryption Successful" : "Decryption Failed");
            alert.setHeaderText(null);
            alert.setContentText(success 
                ? "File '" + document.getFileName() + "' has been decrypted successfully."
                : "Failed to decrypt file '" + document.getFileName() + "'. Please try again or check if the file is encrypted.");
            alert.showAndWait();
            
            if (success && onActionComplete != null) {
                onActionComplete.run(); // Refresh tables
            }
        } else {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Decryption Error");
            alert.setHeaderText(null);
            alert.setContentText("Cannot decrypt file: No document selected or client not connected.");
            alert.showAndWait();
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
            boolean success = clientSocketManager.deleteDocument(document.getDocID());
            Alert alert = new Alert(success ? AlertType.INFORMATION : AlertType.ERROR);
            alert.setTitle(success ? "Deletion Successful" : "Deletion Failed");
            alert.setHeaderText(null);
            alert.setContentText(success 
                ? "File '" + document.getFileName() + "' has been deleted successfully."
                : "Failed to delete file '" + document.getFileName() + "'. Please try again.");
            alert.showAndWait();

            if (success && onActionComplete != null) {
                onActionComplete.run(); // Refresh tables
            }
        } else {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Deletion Error");
            alert.setHeaderText(null);
            alert.setContentText("Cannot delete file: No document selected or client not connected.");
            alert.showAndWait();
        }
        closePopup(event);
    }

    private void closePopup(MouseEvent event) {
        Node source = (Node) event.getSource();
        Popup popup = (Popup) source.getScene().getWindow();
        popup.hide();
    }
}