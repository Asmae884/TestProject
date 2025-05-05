package com.emi.projetintegre.client;

import java.io.*;
import java.util.function.Function;
import java.util.function.Supplier;

import com.emi.projetintegre.services.PasswordManager;

public class AuthenticationManager {
    private final Supplier<ObjectOutputStream> outputStreamSupplier;
    private final Supplier<ObjectInputStream> inputStreamSupplier;
    private final Function<Object, Boolean> sendFunction;
    private final Function<Void, Boolean> isConnectedFunction;

    public AuthenticationManager(
        Supplier<ObjectOutputStream> outputStreamSupplier,
        Supplier<ObjectInputStream> inputStreamSupplier,
        Function<Object, Boolean> sendFunction,
        Function<Void, Boolean> isConnectedFunction
    ) {
        this.outputStreamSupplier = outputStreamSupplier;
        this.inputStreamSupplier = inputStreamSupplier;
        this.sendFunction = sendFunction;
        this.isConnectedFunction = isConnectedFunction;
    }

    public boolean authenticate(String login, String password) {
        if (!isConnectedFunction.apply(null)) {
            System.out.println("Not connected to server!");
            return false;
        }
        
        try {
            String hashedPassword = PasswordManager.hashPassword(password);
            String[] credentials = {login, hashedPassword};
            
            sendFunction.apply("AUTHENTICATE");
            sendFunction.apply(credentials);
            
            System.out.println("Credentials sent to server...");
            
            ObjectInputStream input = inputStreamSupplier.get();
            Object response = input.readObject();
            if (response instanceof String && response.equals("AUTH_SUCCESS")) {
                System.out.println("Authentication successful");
                return true;
            }
            System.out.println("Authentication failed: " + response);
            return false;
        } catch (Exception e) {
            System.err.println("Authentication error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}