package com.emi.projetintegre.controllers;

import com.emi.projetintegre.client.ClientSocketManager;
import com.emi.projetintegre.models.PersonalDocument;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;

public class FileWriterController {
    
    @FXML private Label fileNameLabel;
    @FXML private TextArea fileContentArea;
    @FXML private ImageView fileImageView;
    @FXML private Button uploadButton;
    @FXML private Button saveButton;
    @FXML private Label profileInitials;
    @FXML private Label title;

    private ClientSocketManager clientSocketManager;
    private PersonalDocument document;
    private String fileType;

    @FXML
    public void initialize() {
        fileContentArea.setEditable(true);
        title.setText("Crypticshare - Write File");
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
            System.err.println("Document is null in FileWriterController");
            fileContentArea.setText("Error: No document selected.");
            fileContentArea.setVisible(true);
            fileImageView.setVisible(false);
            uploadButton.setVisible(false);
            saveButton.setVisible(false);
        }
    }
    
    

private void loadFileContent() {
    if (clientSocketManager == null || !clientSocketManager.isConnected()) {
        System.err.println("Cannot load file content: ClientSocketManager is null or not connected");
        showError("Error: Not connected to the server. Please check your connection and try again.");
        return;
    }

    if (document == null) {
        System.err.println("Cannot load file content: Document is null");
        showError("Error: No document selected.");
        return;
    }

    try {
        Object[] result = clientSocketManager.getDocumentContent(document.getDocID());
        if (result != null && result.length == 2) {
            byte[] content = (byte[]) result[0];
            fileType = ((String) result[1]).toLowerCase();

            if (content != null) {
                if (fileType.equals("txt")) {
                    // Handle text files
                    fileContentArea.setText(new String(content, java.nio.charset.StandardCharsets.UTF_8));
                    fileContentArea.setVisible(true);
                    saveButton.setVisible(true);
                    fileImageView.getParent().setVisible(false); // Hide image container
                    System.out.println("Successfully loaded text content for docID: " + document.getDocID());
                } else if (fileType.equals("png") || fileType.equals("jpg") || fileType.equals("jpeg")) {
                    // Handle image files
                    Image image = new Image(new ByteArrayInputStream(content));
                    if (!image.isError()) {
                        fileImageView.setImage(image);
                        fileImageView.getParent().setVisible(true); // Show image container
                        uploadButton.setVisible(true);
                        fileContentArea.setVisible(false);
                        saveButton.setVisible(false);
                        System.out.println("Successfully loaded image content for docID: " + document.getDocID());
                    } else {
                        showError("Error: Failed to load image.");
                    }
                } else {
                    showError("Error: Unsupported file type: " + fileType);
                    System.err.println("Unsupported file type: " + fileType + " for docID: " + document.getDocID());
                }
            } else {
                showError("Error: Could not retrieve file content.");
                System.err.println("Failed to retrieve content for docID: " + document.getDocID());
            }
        } else {
            showError("Error: Could not retrieve file content.");
            System.err.println("Failed to retrieve content for docID: " + document.getDocID());
        }
    } catch (Exception e) {
        System.err.println("Exception loading content for docID " + document.getDocID() + ": " + e.getMessage());
        e.printStackTrace();
        showError("Error: Failed to load file content due to an issue: " + e.getMessage());
    }
}

// Helper method to show errors
private void showError(String errorMessage) {
    fileContentArea.setText(errorMessage);
    fileContentArea.setVisible(true);
    fileImageView.getParent().setVisible(false);
    uploadButton.setVisible(false);
    saveButton.setVisible(false);
}
   @FXML
    private void handleSaveAction() {
        if (clientSocketManager == null || !clientSocketManager.isConnected()) {
            System.err.println("Cannot save file content: ClientSocketManager is null or not connected");
            fileContentArea.setText("Error: Not connected to the server. Please check your connection and try again.");
            return;
        }

        if (document == null) {
            System.err.println("Cannot save file content: Document is null");
            fileContentArea.setText("Error: No document selected.");
            return;
        }

        if (!fileType.equals("txt")) {
            System.err.println("Save action is only supported for text files");
            fileContentArea.setText("Error: Save is only supported for text files.");
            return;
        }

        try {
            String content = fileContentArea.getText();
            boolean success = clientSocketManager.saveDocumentContent(document.getDocID(), content);
            if (success) {
                System.out.println("Successfully saved content for docID: " + document.getDocID());
            } else {
                System.err.println("Failed to save content for docID: " + document.getDocID());
                fileContentArea.setText("Error: Could not save file content.");
            }
        } catch (Exception e) {
            System.err.println("Exception saving content for docID " + document.getDocID() + ": " + e.getMessage());
            e.printStackTrace();
            fileContentArea.setText("Error: Failed to save file content due to an issue: " + e.getMessage());
        }
    }

    @FXML
    private void handleUploadImageAction() {
        if (clientSocketManager == null || !clientSocketManager.isConnected()) {
            System.err.println("Cannot upload image: ClientSocketManager is null or not connected");
            fileContentArea.setText("Error: Not connected to the server. Please check your connection and try again.");
            fileContentArea.setVisible(true);
            fileImageView.setVisible(false);
            uploadButton.setVisible(false);
            return;
        }

        if (document == null) {
            System.err.println("Cannot upload image: Document is null");
            fileContentArea.setText("Error: No document selected.");
            fileContentArea.setVisible(true);
            fileImageView.setVisible(false);
            uploadButton.setVisible(false);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(fileContentArea.getScene().getWindow());

        if (selectedFile != null) {
            try {
                byte[] imageContent = Files.readAllBytes(selectedFile.toPath());
                String newFileType = selectedFile.getName().substring(selectedFile.getName().lastIndexOf('.') + 1).toLowerCase();
                
                if (!newFileType.equals(fileType)) {
                    fileContentArea.setText("Error: Selected file type (" + newFileType + ") does not match original file type (" + fileType + ").");
                    fileContentArea.setVisible(true);
                    fileImageView.setVisible(false);
                    uploadButton.setVisible(false);
                    return;
                }

                boolean success = clientSocketManager.updateDocumentContent(document.getDocID(), imageContent, newFileType);
                if (success) {
                    Image newImage = new Image(new ByteArrayInputStream(imageContent));
                    if (!newImage.isError()) {
                        fileImageView.setImage(newImage);
                        System.out.println("Successfully uploaded new image for docID: " + document.getDocID());
                    } else {
                        fileContentArea.setText("Error: Failed to load uploaded image.");
                        fileContentArea.setVisible(true);
                        fileImageView.setVisible(false);
                        uploadButton.setVisible(false);
                    }
                } else {
                    fileContentArea.setText("Error: Could not upload new image.");
                    fileContentArea.setVisible(true);
                    fileImageView.setVisible(false);
                    uploadButton.setVisible(false);
                    System.err.println("Failed to upload image for docID: " + document.getDocID());
                }
            } catch (Exception e) {
                System.err.println("Exception uploading image for docID " + document.getDocID() + ": " + e.getMessage());
                e.printStackTrace();
                fileContentArea.setText("Error: Failed to upload image due to an issue: " + e.getMessage());
                fileContentArea.setVisible(true);
                fileImageView.setVisible(false);
                uploadButton.setVisible(false);
            }
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