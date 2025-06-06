package com.emi.projetintegre.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import java.io.IOException;

public class MenuController {
    @FXML private BorderPane bord1;
    @FXML private AnchorPane contentArea;
    @FXML private Label title;
    @FXML private Label profileInitials;
    @FXML private ProgressBar storageProgress;
    @FXML private Label storageLabel;
    
    @FXML
    public void initialize() {
        // Load home view by default
        loadView("home");
        
        // Set up any initial values
        updateStorageInfo();
        updateProfileInfo();
    }

    @FXML
    private void handleHomeAction() {
        loadView("home");
    }

    @FXML
    private void handleSharedWithMeAction() {
        loadView("shared-with-me");
    }

    @FXML
    private void handleMySharedAction() {
        loadView("my-shared");
    }

    @FXML
    private void handleRecentAction() {
        loadView("recent");
    }

    @FXML
    private void handleStarredAction() {
        loadView("starred");
    }

    @FXML
    private void handleTrashAction() {
        loadView("trash");
    }

    @FXML
    private void handleUploadAction() {
        // Handle upload action
    }

    private void loadView(String viewName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emi/projetintegre/views/" + viewName + ".fxml"));
            AnchorPane view = loader.load();
            
            // Set the loaded view to fill the content area
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            
            // Clear existing content and set new content
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            
        } catch (IOException e) {
            e.printStackTrace();
            // Handle error appropriately
        }
    }

    private void updateStorageInfo() {
        // Update storage progress and label
        double usedStorage = 5.14; // Get actual value
        double totalStorage = 15.0; // Get actual value
        storageProgress.setProgress(usedStorage / totalStorage);
        storageLabel.setText(String.format("%.2f Go utilisés sur %.0f Go", usedStorage, totalStorage));
    }

    private void updateProfileInfo() {
        // Set profile initials based on logged-in user
        String userInitials = "NP"; // Get actual initials
        profileInitials.setText(userInitials);
    }
}
