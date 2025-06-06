package com.emi.projetintegre.controllers;

import com.emi.projetintegre.client.ClientSocketManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

public class UploadDialogController {

    @FXML private TextField pathField;
    @FXML private TextField nameField;
    @FXML private Label sizeLabel;
    @FXML private CheckBox encryptCheckBox;
    @FXML private Button browseButton;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;

    private ClientSocketManager clientSocketManager;
    private File selectedFile;
    private Runnable onUploadSuccess;

    public void setClientSocketManager(ClientSocketManager clientSocketManager) {
        this.clientSocketManager = clientSocketManager;
    }

    public void setOnUploadSuccess(Runnable onUploadSuccess) {
        this.onUploadSuccess = onUploadSuccess;
    }

    @FXML
    private void handleBrowseAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");
        selectedFile = fileChooser.showOpenDialog(getStage());
        if (selectedFile != null) {
            pathField.setText(selectedFile.getAbsolutePath());
            nameField.setText(selectedFile.getName());
            sizeLabel.setText(formatFileSize(selectedFile.length()));
        }
    }

    @FXML
    private void handleCancelAction() {
        closeDialog();
    }

    @FXML
    private void handleSaveAction() {
        if (selectedFile == null || !selectedFile.exists()) {
            showAlert("Error", "Please select a valid file");
            return;
        }

        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showAlert("Error", "File name cannot be empty");
            return;
        }

        if (clientSocketManager == null || !clientSocketManager.isConnected()) {
            showAlert("Error", "Not connected to server");
            return;
        }

        String filePath = selectedFile.getAbsolutePath();
        String fileName = nameField.getText().trim();

        // Handle duplicate file names
        while (true) {
            String duplicateCheck = clientSocketManager.checkDuplicateFile(fileName);
            if ("FILE_NOT_FOUND".equals(duplicateCheck)) {
                // No duplicate, proceed with upload
                break;
            } else if ("DUPLICATE_FILE".equals(duplicateCheck)) {
                // Show dialog to prompt for new file name
                TextInputDialog dialog = new TextInputDialog(fileName);
                dialog.setTitle("Duplicate File Name");
                dialog.setHeaderText("A file named '" + fileName + "' already exists.");
                dialog.setContentText("Please enter a new file name:");

                dialog.showAndWait().ifPresent(newName -> {
                    if (newName != null && !newName.trim().isEmpty()) {
                        nameField.setText(newName.trim());
                    } else {
                        nameField.setText(""); // Signal cancellation
                    }
                });

                fileName = nameField.getText().trim();
                if (fileName.isEmpty()) {
                    showAlert("Info", "Upload cancelled due to duplicate file name");
                    return;
                }
            } else {
                // Error during duplicate check
                showAlert("Error", "Failed to check for duplicate file: " + duplicateCheck);
                return;
            }
        }

        // Perform the upload based on encryption checkbox
        boolean success;
        if (encryptCheckBox.isSelected()) {
            success = clientSocketManager.uploadEncryptedDocument(filePath, fileName);
        } else {
            success = clientSocketManager.uploadDocument(filePath, fileName);
        }

        if (success) {
            showAlert("Success", "File uploaded successfully");
            if (onUploadSuccess != null) {
                onUploadSuccess.run();
            }
            closeDialog();
        } else {
            showAlert("Error", "Failed to upload file");
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = String.valueOf("KMGTPE".charAt(exp - 1));
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeDialog() {
        getStage().close();
    }

    private Stage getStage() {
        return (Stage) cancelButton.getScene().getWindow();
    }
}