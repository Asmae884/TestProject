package com.emi.projetintegre.controllers;

import com.emi.projetintegre.client.ClientSocketManager;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LoginController {

    @FXML private StackPane leftPane;
    @FXML private Rectangle gradient1;
    @FXML private Rectangle gradient2;
    @FXML private Rectangle gradient3;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink forgotPasswordLink;
    
    private ClientSocketManager clientSocketManager;
    private final List<Rectangle> rectangles = new ArrayList<>();
    private final Map<Rectangle, double[]> velocities = new HashMap<>();
    private final Map<Rectangle, Timeline> movementTimelines = new HashMap<>();
    private final List<Color> colorPalette = List.of(
        Color.web("#CBFFB4", 0.3), // gradient1 color and opacity
        Color.web("#FFD0E0", 0.3), // gradient2 color and opacity
        Color.web("#F3E8FD", 0.4)  // gradient3 color and opacity
    );
    private final Random random = new Random();
    private static final double PANE_WIDTH = 450; // Matches Rectangle in FXML
    private static final double PANE_HEIGHT = 600; // Matches Rectangle in FXML

    @FXML
    public void initialize() {
        clientSocketManager = new ClientSocketManager();
        initializeRectangles();
        setupAnimations();
        setupEventHandlers();
        setupHoverEffects();
        setupPaneInteraction();
    }

    private void initializeRectangles() {
        // Add initial rectangles to the list (already part of leftPane from FXML)
        rectangles.add(gradient1);
        rectangles.add(gradient2);
        rectangles.add(gradient3);
    }

    private void setupHoverEffects() {
        // Hover effects for login button
        loginButton.setOnMouseEntered(e -> {
            loginButton.setCursor(Cursor.HAND);
            loginButton.setStyle("-fx-background-color: #4A2D63; -fx-text-fill: white; -fx-background-radius: 8;");
        });
        
        loginButton.setOnMouseExited(e -> {
            loginButton.setCursor(Cursor.DEFAULT);
            loginButton.setStyle("-fx-background-color: #5D3A7A; -fx-text-fill: white; -fx-background-radius: 8;");
        });
        
        loginButton.setOnMousePressed(e -> {
            loginButton.setTranslateY(1);
            loginButton.setEffect(new DropShadow());
        });
         
        loginButton.setOnMouseReleased(e -> {
            loginButton.setTranslateY(0);
            loginButton.setEffect(null);
        });
 
        // Hover effects for forgot password link
        forgotPasswordLink.setOnMouseEntered(e -> {
            forgotPasswordLink.setCursor(Cursor.HAND);
            forgotPasswordLink.setStyle("-fx-text-fill: #FFA0C0; -fx-border-color: transparent;");
        });
        
        forgotPasswordLink.setOnMouseExited(e -> {
            forgotPasswordLink.setCursor(Cursor.DEFAULT);
            forgotPasswordLink.setStyle("-fx-text-fill: #FFD0E0; -fx-border-color: transparent;");
        });
        
        forgotPasswordLink.setOnMousePressed(e -> {
            forgotPasswordLink.setTranslateY(1);
        });
        
        forgotPasswordLink.setOnMouseReleased(e -> {
            forgotPasswordLink.setTranslateY(0);
        });
    }

    private void setupAnimations() {
        // Animate each initial rectangle
        for (Rectangle rect : rectangles) {
            animateRectangle(rect);
            setupRectangleInteraction(rect, false); // No hover scaling for initial rectangles
        }
    }

    private void animateRectangle(Rectangle rect) {
        // Random initial velocity
        double speed = 50 + random.nextDouble() * 100; // Pixels per second
        double angle = random.nextDouble() * 2 * Math.PI;
        double[] velocity = new double[]{speed * Math.cos(angle), speed * Math.sin(angle)}; // [vx, vy]
        velocities.put(rect, velocity);

        // Timeline for continuous movement
        Timeline timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        movementTimelines.put(rect, timeline); // Store the timeline

        KeyFrame keyFrame = new KeyFrame(Duration.millis(16), e -> {
            // Update position
            double newX = rect.getTranslateX() + velocity[0] * 0.016; // 16ms = 0.016s
            double newY = rect.getTranslateY() + velocity[1] * 0.016;

            // Bounce off edges
            if (newX < -PANE_WIDTH / 2 + rect.getWidth() / 2 || newX > PANE_WIDTH / 2 - rect.getWidth() / 2) {
                velocity[0] = -velocity[0]; // Update vx
                newX = Math.max(-PANE_WIDTH / 2 + rect.getWidth() / 2, Math.min(newX, PANE_WIDTH / 2 - rect.getWidth() / 2));
            }
            if (newY < -PANE_HEIGHT / 2 + rect.getHeight() / 2 || newY > PANE_HEIGHT / 2 - rect.getHeight() / 2) {
                velocity[1] = -velocity[1]; // Update vy
                newY = Math.max(-PANE_HEIGHT / 2 + rect.getHeight() / 2, Math.min(newY, PANE_HEIGHT / 2 - rect.getHeight() / 2));
            }

            rect.setTranslateX(newX);
            rect.setTranslateY(newY);
        });

        timeline.getKeyFrames().add(keyFrame);
        timeline.play();

        // Fade in when created
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), rect);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(rect.getOpacity());
        fadeIn.play();
    }

    private void setupRectangleInteraction(Rectangle rect, boolean enableHoverScaling) {
        // Hover effect: scale up (only for dynamically added rectangles)
        if (enableHoverScaling) {
            rect.setOnMouseEntered(e -> {
                rect.setCursor(Cursor.HAND);
                ScaleTransition scale = new ScaleTransition(Duration.millis(200), rect);
                scale.setToX(1.2);
                scale.setToY(1.2);
                scale.play();
            });

            rect.setOnMouseExited(e -> {
                rect.setCursor(Cursor.DEFAULT);
                ScaleTransition scale = new ScaleTransition(Duration.millis(200), rect);
                scale.setToX(1.0);
                scale.setToY(1.0);
                scale.play();
            });
        }

        // Click effect: confetti-like disappearance
        rect.setOnMouseClicked(e -> {
            // Stop the movement timeline
            Timeline timeline = movementTimelines.remove(rect);
            if (timeline != null) {
                timeline.stop();
            }
            velocities.remove(rect);

            // Confetti animation: fade out, scale down, slight random movement
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), rect);
            fadeOut.setToValue(0);

            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(500), rect);
            scaleDown.setToX(0.2);
            scaleDown.setToY(0.2);

            TranslateTransition scatter = new TranslateTransition(Duration.millis(500), rect);
            scatter.setByX((random.nextDouble() - 0.5) * 50); // Random movement up to ±25 pixels
            scatter.setByY((random.nextDouble() - 0.5) * 50);

            ParallelTransition confetti = new ParallelTransition(fadeOut, scaleDown, scatter);
            confetti.setOnFinished(event -> {
                leftPane.getChildren().remove(rect);
                rectangles.remove(rect);
            });
            confetti.play();
        });
    }

    private void setupPaneInteraction() {
        // Click on leftPane to spawn new rectangles
        leftPane.setOnMouseClicked(e -> {
            if (rectangles.size() < 6) { // Limit to 6 total rectangles (3 initial + 3 added)
                Rectangle newRect = createRandomRectangle();
                rectangles.add(newRect);
                leftPane.getChildren().add(newRect);
                animateRectangle(newRect);
                setupRectangleInteraction(newRect, true); // Enable hover scaling for new rectangles
            }
        });
    }

    private Rectangle createRandomRectangle() {
        double size = 50 + random.nextDouble() * 200; // Random size between 50 and 250
        Rectangle rect = new Rectangle(size, size);
        rect.setArcWidth(50);
        rect.setArcHeight(50);
        rect.setFill(colorPalette.get(random.nextInt(colorPalette.size()))); // Use color from palette
        rect.setEffect(new DropShadow(10, Color.gray(0.5, 0.5)));
        
        // Random initial position within pane bounds
        double x = (random.nextDouble() - 0.5) * (PANE_WIDTH - size);
        double y = (random.nextDouble() - 0.5) * (PANE_HEIGHT - size);
        rect.setTranslateX(x);
        rect.setTranslateY(y);

        return rect;
    }

    private void setupEventHandlers() {
        loginButton.setOnAction(event -> handleLogin());
        forgotPasswordLink.setOnAction(event -> handleForgotPassword());
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs", Alert.AlertType.ERROR);
            shakeField(usernameField);
            shakeField(passwordField);
            return;
        }

        try {
            // Show loading state
            loginButton.setDisable(true);
            loginButton.setText("Connexion en cours...");

            // Connect to server
            clientSocketManager.connect();
            
            if (!clientSocketManager.isConnected()) {
                showAlert("Erreur", "Impossible de se connecter au serveur", Alert.AlertType.ERROR);
                return;
            }

            // Authenticate
            boolean authSuccess = clientSocketManager.sendCredentials(username, password);
            
            if (authSuccess) {
                navigateToHome();
            } else {
                showAlert("Échec", "Identifiants incorrects", Alert.AlertType.ERROR);
                shakeField(usernameField);
                shakeField(passwordField);
            }
        } catch (Exception e) {
            showAlert("Erreur", "Une erreur est survenue: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        } finally {
            loginButton.setDisable(false);
            loginButton.setText("Se Connecter");
        }
    }

    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emi/projetintegre/views/SignalPasswordLost.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Récupération de mot de passe");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible de charger la fenêtre de récupération", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void navigateToHome() {
        try {
            System.out.println("Loading Home.fxml");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emi/projetintegre/views/Home.fxml"));
            Parent root = loader.load();
            
            // Pass the clientSocketManager to the Home controller
            HomeController homeController = loader.getController();
            System.out.println("Setting ClientSocketManager");
            homeController.setClientSocketManager(clientSocketManager);
            System.out.println("Refreshing tables after login");
            homeController.refreshTablesAfterLogin();
            
            System.out.println("Showing Home scene");
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible de charger la page d'accueil", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void shakeField(TextField field) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(70), field);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.playFromStart();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}