package com.emi.projetintegre.services;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordManager {
    public static String hashPassword(String password) throws NoSuchAlgorithmException {
    	if (password == null) {
            return null; // Or handle differently, e.g., return a default hash or throw an exception
        }
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = md.digest(password.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}