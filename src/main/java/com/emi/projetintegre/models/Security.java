package com.emi.projetintegre.models;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Security {

    // Static method for hashing the password
    public static String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = md.digest(password.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));  // Convertir chaque octet en hexadécimal
        }
        return sb.toString();
    }

}
