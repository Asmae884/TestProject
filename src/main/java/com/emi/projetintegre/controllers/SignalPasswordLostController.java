package com.emi.projetintegre.controllers;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SignalPasswordLostController {

    @FXML private VBox rootPane;
    @FXML private TextField usernameField;
    @FXML private Button sendButton;
    @FXML private Button cancelButton;

    @FXML
    public void initialize() {
        setupHoverEffects();
    }

    private void setupHoverEffects() {
        // Hover effects for send button
        sendButton.setOnMouseEntered(e -> {
            sendButton.setStyle("-fx-background-color: #B0E69A; -fx-text-fill: #5d3a7a; -fx-background-radius: 4;");
        });
        
        sendButton.setOnMouseExited(e -> {
            sendButton.setStyle("-fx-background-color: #CBFFB4; -fx-text-fill: #5d3a7a; -fx-background-radius: 4;");
        });

        // Hover effects for cancel button
        cancelButton.setOnMouseEntered(e -> {
            cancelButton.setStyle("-fx-background-color: #FFB6CC; -fx-text-fill: #5D3A7A; -fx-background-radius: 4;");
        });
        
        cancelButton.setOnMouseExited(e -> {
            cancelButton.setStyle("-fx-background-color: #FFD0E0; -fx-text-fill: #5D3A7A; -fx-background-radius: 4;");
        });
    }

    @FXML
    private void handleSend() {
        String username = usernameField.getText().trim();
        
        if (username.isEmpty()) {
            showAlert("Erreur", "Veuillez entrer un nom d'utilisateur");
            return;
        }

        // Change to loading screen
        rootPane.getChildren().clear();
        Label loadingLabel = new Label("Votre demande a été envoyée");
        loadingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #333333;");
        rootPane.getChildren().add(loadingLabel);

        // Wait 2 seconds then close the application
        PauseTransition delay = new PauseTransition(Duration.seconds(5));
        delay.setOnFinished(event -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.close();
            // Optionally, exit the entire application
            System.exit(0);
        });
        delay.play();
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}