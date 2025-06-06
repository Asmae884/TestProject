package com.emi.projetintegre.controllers;

import com.emi.projetintegre.client.ClientSocketManager;
import com.emi.projetintegre.models.PersonalDocument;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.ByteArrayInputStream;

public class FileViewerController {
    
    @FXML private Label fileNameLabel;
    @FXML private TextArea fileContentArea;
    @FXML private ImageView fileImageView;
    @FXML private Label profileInitials;
    @FXML private Label title;

    private ClientSocketManager clientSocketManager;
    private PersonalDocument document;

    @FXML
    public void initialize() {
        // Ensure the TextArea is not editable
        fileContentArea.setEditable(false);
        title.setText("Crypticshare - View File");
    }

    public void setClientSocketManager(ClientSocketManager clientSocketManager) {
        this.clientSocketManager = clientSocketManager;
    }

    public void setDocument(PersonalDocument document) {
        this.document = document;
        if (document != null) {
            fileNameLabel.setText(document.getFileName());
            System.out.println("Loading content for file: " + document.getFileName() + " (docID: " + document.getDocID() + ")");
            loadFileContent();
        } else {
            System.err.println("Document is null in FileViewerController");
            fileContentArea.setText("Error: No document selected.");
            fileImageView.setVisible(false);
            fileContentArea.setVisible(true);
        }
    }

    private void loadFileContent() {
        if (clientSocketManager == null || !clientSocketManager.isConnected()) {
            System.err.println("Cannot load file content: ClientSocketManager is null or not connected");
            fileContentArea.setText("Error: Not connected to the server. Please check your connection and try again.");
            fileImageView.setVisible(false);
            fileContentArea.setVisible(true);
            return;
        }

        if (document == null) {
            System.err.println("Cannot load file content: Document is null");
            fileContentArea.setText("Error: No document selected.");
            fileImageView.setVisible(false);
            fileContentArea.setVisible(true);
            return;
        }

        try {
            // Get content and file type
            Object[] result = clientSocketManager.getDocumentContent(document.getDocID());
            if (result != null && result.length == 2) {
                byte[] content = (byte[]) result[0];
                String fileType = (String) result[1];

                if (content != null) {
                    // Handle based on file type
                    if (fileType.equalsIgnoreCase("txt")) {
                        fileContentArea.setText(new String(content, java.nio.charset.StandardCharsets.UTF_8));
                        fileContentArea.setVisible(true);
                        fileImageView.setVisible(false);
                        System.out.println("Successfully loaded text content for docID: " + document.getDocID());
                    } else if (fileType.equalsIgnoreCase("png") || fileType.equalsIgnoreCase("jpg") || fileType.equalsIgnoreCase("jpeg")) {
                        Image image = new Image(new ByteArrayInputStream(content));
                        if (!image.isError()) {
                            fileImageView.setImage(image);
                            fileImageView.setVisible(true);
                            fileContentArea.setVisible(false);
                            System.out.println("Successfully loaded image content for docID: " + document.getDocID());
                        } else {
                            fileContentArea.setText("Error: Failed to load image.");
                            fileImageView.setVisible(false);
                            fileContentArea.setVisible(true);
                        }
                    } else {
                        fileContentArea.setText("Error: Unsupported file type: " + fileType);
                        fileImageView.setVisible(false);
                        fileContentArea.setVisible(true);
                        System.err.println("Unsupported file type: " + fileType + " for docID: " + document.getDocID());
                    }
                } else {
                    fileContentArea.setText("Error: Could not retrieve file content.");
                    fileImageView.setVisible(false);
                    fileContentArea.setVisible(true);
                    System.err.println("Failed to retrieve content for docID: " + document.getDocID());
                }
            } else {
                fileContentArea.setText("Error: Could not retrieve file content.");
                fileImageView.setVisible(false);
                fileContentArea.setVisible(true);
                System.err.println("Failed to retrieve content for docID: " + document.getDocID());
            }
        } catch (Exception e) {
            System.err.println("Exception loading content for docID " + document.getDocID() + ": " + e.getMessage());
            e.printStackTrace();
            fileContentArea.setText("Error: Failed to load file content due to an issue: " + e.getMessage());
            fileImageView.setVisible(false);
            fileContentArea.setVisible(true);
        }
    }

    @FXML
    private void handleBackAction(MouseEvent event) {
        Stage stage = (Stage) fileContentArea.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleHomeAction() {
        Stage stage = (Stage) fileContentArea.getScene().getWindow();
        stage.close();
    }
}