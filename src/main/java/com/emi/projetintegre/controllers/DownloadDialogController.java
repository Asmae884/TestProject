package com.emi.projetintegre.controllers;


import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;

public class DownloadDialogController {

    @FXML private TextField pathField;
    @FXML private TextField nameField;
    @FXML private Label sizeLabel;
    @FXML private CheckBox decryptCheckBox;
    @FXML private Button browseButton;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;
/*
    private ClientSocketManager clientSocketManager;
    private PersonalDocument document;
    private Runnable onDownloadSuccess;

    public void setClientSocketManager(ClientSocketManager clientSocketManager) {
        this.clientSocketManager = clientSocketManager;
    }

    public void setDocument(PersonalDocument document) {
        this.document = document;
        initializeFields();
    }

    public void setOnDownloadSuccess(Runnable onDownloadSuccess) {
        this.onDownloadSuccess = onDownloadSuccess;
    }

    private void initializeFields() {
        if (document != null) {
            nameField.setText(document.getFileName());
            sizeLabel.setText(formatFileSize(document.getSize()));
            pathField.setText(System.getProperty("user.home"));
            decryptCheckBox.setSelected(false);
        }
    }

    @FXML
    private void handleBrowseAction() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Download Location");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File selectedDirectory = directoryChooser.showDialog(getStage());
        if (selectedDirectory != null) {
            pathField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    private void handleCancelAction() {
        closeDialog();
    }

    @FXML/*
    private void handleSaveAction() {
        if (document == null || clientSocketManager == null || !clientSocketManager.isConnected()) {
            showAlert("Error", "Not connected to server or no document selected");
            return;
        }

        String saveDirectory = pathField.getText().trim();
        String fileName = nameField.getText().trim();

        if (saveDirectory.isEmpty() || !new File(saveDirectory).isDirectory()) {
            showAlert("Error", "Please select a valid directory");
            return;
        }

        if (fileName.isEmpty()) {
            showAlert("Error", "File name cannot be empty");
            return;
        }

        boolean decrypt = decryptCheckBox.isSelected();
        //boolean success = clientSocketManager.downloadDocument(document.getDocID(), saveDirectory, fileName, decrypt);

        //if (success) {
          //  showAlert("Success", "File '" + fileName + "' downloaded successfully to " + saveDirectory);
            //if (onDownloadSuccess != null) {
              //  onDownloadSuccess.run();
            //}
            //closeDialog();
        }
            // Check if the failure was due to decryption issues
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
    */
}