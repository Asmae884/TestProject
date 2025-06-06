package com.emi.projetintegre.server;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

public class FileSecurity {
    private SecretKey currentKey;

    /**
     * Constructor for FileSecurity with no initial key.
     */
    public FileSecurity() {
        this.currentKey = null;
    }

    // In FileSecurity class (not provided, so this is a suggestion)
    public void setKey(SecretKey key) {
        this.currentKey = key;
    }

    public SecretKey getCurrentKey() {
        return currentKey;
    }

    /**
     * Constructor for FileSecurity with a provided key.
     * @param key The SecretKey to use for encryption/decryption.
     */
    public FileSecurity(SecretKey key) {
        this.currentKey = key;
    }

    /**
     * Generates a new AES key.
     * @return The generated SecretKey.
     * @throws NoSuchAlgorithmException If the AES algorithm is not available.
     */
    public SecretKey generateKey() throws NoSuchAlgorithmException {
        this.currentKey = CryptoUtils.generateKey();
        return this.currentKey;
    }

    /**
     * Loads a key from a Base64-encoded string.ss
     * @param keyString The Base64-encoded key string.
     * @throws IllegalArgumentException If the key string is invalid.
     */
    public void loadKey(String keyString) throws IllegalArgumentException {
        this.currentKey = CryptoUtils.stringToKey(keyString);
        if (!CryptoUtils.isKeyValid(this.currentKey)) {
            this.currentKey = null;
            throw new IllegalArgumentException("Invalid key provided.");
        }
    }

    /**
     * Encrypts a file using the current key.
     * @param inputFile The source file to encrypt.
     * @param outputFile The destination file for encrypted data.
     * @throws IllegalStateException If no key is loaded.
     * @throws IOException If an I/O error occurs.
     * @throws GeneralSecurityException If a security error occurs.
     */
    public void encryptFile(Path inputFile, Path outputFile) throws IOException, GeneralSecurityException {
        if (currentKey == null) {
            throw new IllegalStateException("No key loaded for encryption.");
        }
        if (!Files.exists(inputFile) || !Files.isRegularFile(inputFile)) {
            throw new IOException("Input file does not exist or is not a valid file.");
        }
        CryptoUtils.encryptFile(inputFile, outputFile, currentKey);
    }

    /**
     * Decrypts a file using the current key.
     * @param inputFile The source encrypted file.
     * @param outputFile The destination file for decrypted data.
     * @throws IllegalStateException If no key is loaded.
     * @throws IOException If an I/O error occurs.
     * @throws GeneralSecurityException If a security error occurs.
     */
    public void decryptFile(Path inputFile, Path outputFile) throws IOException, GeneralSecurityException {
        if (currentKey == null) {
            throw new IllegalStateException("No key loaded for decryption.");
        }
        if (!Files.exists(inputFile) || !Files.isRegularFile(inputFile)) {
            throw new IOException("Input file does not exist or is not a valid file.");
        }
        CryptoUtils.decryptFile(inputFile, outputFile, currentKey);
    }

    /**
     * Gets the current key as a Base64-encoded string.
     * @return The Base64-encoded key string, or null if no key is loaded.
     */
    public String getKeyAsString() {
        if (currentKey == null) {
            return null;
        }
        return CryptoUtils.keyToString(currentKey);
    }

    /**
     * Checks if a key is currently loaded.
     * @return True if a key is loaded, false otherwise.
     */
    public boolean hasKey() {
        return currentKey != null;
    }
}
