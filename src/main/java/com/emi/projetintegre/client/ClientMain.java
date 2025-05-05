package com.emi.projetintegre.client;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) {
        ClientSocketManager client = new ClientSocketManager();
        Scanner scanner = new Scanner(System.in);

        try {
            // Connect to server
            System.out.println("Connecting to server...");
            client.connect();
            
            if (!client.isConnected()) {
                System.out.println("Failed to connect to server. Exiting...");
                return;
            }

            // Authentication
            int maxAttempts = 3;
            int attempts = 0;
            boolean loggedIn = false;
            
            while (attempts < maxAttempts && !loggedIn) {
                System.out.print("\nAttempt " + (attempts + 1) + " of " + maxAttempts);
                System.out.print("\nLogin: ");
                String login = scanner.nextLine().trim();
                
                System.out.print("Password: ");
                String password = scanner.nextLine().trim();

                loggedIn = client.sendCredentials(login, password);
                
                if (!loggedIn) {
                    System.out.println("Authentication failed. Please try again.");
                    attempts++;
                }
            }

            if (!loggedIn) {
                System.out.println("\nToo many failed attempts. Exiting...");
                return;
            }

            System.out.println("\nAuthentication successful!\n");

            // File upload
         // In your ClientMain.java, modify the file upload section like this:

            boolean uploadSuccess = false;
            while (!uploadSuccess) {
                System.out.print("Enter path to file to upload (or 'exit' to quit): ");
                String path = scanner.nextLine().trim();
                
                if (path.equalsIgnoreCase("exit")) {
                    break;
                }

                // Remove surrounding quotes if present
                path = path.replaceAll("^\"|\"$", "");
                
                // Replace double backslashes with single ones
                path = path.replace("\\\\", "\\");
                
                // Try to normalize the path
                try {
                    path = Paths.get(path).normalize().toString();
                } catch (InvalidPathException e) {
                    System.out.println("Invalid path format: " + path);
                    continue;
                }

                uploadSuccess = client.uploadDocument(path);
                
                if (!uploadSuccess) {
                    System.out.println("Upload failed. Would you like to try another file? (yes/no)");
                    String choice = scanner.nextLine().trim();
                    if (!choice.equalsIgnoreCase("yes")) {
                        break;
                    }
                } else {
                    System.out.println("File uploaded successfully!");
                }
            }

            // Optionally add more operations here (list/download files, etc.)
            
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up resources
            try {
                if (scanner != null) {
                    scanner.close();
                }
                client.disconnect();
                System.out.println("Disconnected from server.");
            } catch (Exception e) {
                System.err.println("Error during cleanup: " + e.getMessage());
            }
        }
    }
}