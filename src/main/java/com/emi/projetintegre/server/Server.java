package com.emi.projetintegre.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static void main(String[] args) {
        int port = 5000;
        ExecutorService threadPool = Executors.newFixedThreadPool(10); // Thread pool to handle 10 clients concurrently

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running on port " + port);

            // Run the server until it is manually stopped
            while (true) {
                try {
                    // Accept client connections
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress());

                    // Submit a new task to the thread pool to handle the client connection
                    threadPool.submit(new ClientHandler(clientSocket));

                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error starting the server on port " + port + ": " + e.getMessage());
        } finally {
            // Shut down the thread pool gracefully
            threadPool.shutdown();
            System.out.println("Server is shutting down.");
        }
    }
}
