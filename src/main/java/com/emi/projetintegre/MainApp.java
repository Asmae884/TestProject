package com.emi.projetintegre;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emi/projetintegre/views/Login.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 1280, 720);
        primaryStage.setTitle("CrypticShare - Connexion");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}