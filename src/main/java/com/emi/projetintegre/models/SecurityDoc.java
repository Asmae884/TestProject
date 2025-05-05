package com.emi.projetintegre.models;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
//import javax.crypto.KeyGenerator;
//import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class SecurityDoc {
    private String encryptionKey;

    public SecurityDoc(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    // Encrypts the data using AES
    public byte[] encrypt(byte[] data) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Decrypts the data using AES
    public byte[] decrypt(byte[] data) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Example method for generating a simple hash of data
    public String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
