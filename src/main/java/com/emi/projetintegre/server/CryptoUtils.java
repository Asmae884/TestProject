package com.emi.projetintegre.server;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.util.*;
import java.nio.*;
import java.nio.file.*;
import java.io.*;

public final class CryptoUtils {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12;  // bytes
    private static final int KEY_SIZE = 256;  // bits
    private static final int BUFFER_SIZE = 8192; // Buffer size for large files

    public record EncryptedData(byte[] iv, byte[] ciphertext) {
        /**
         * Combines IV and ciphertext into a single byte array.
         * @return The concatenated byte array (IV + ciphertext).
         */
        public byte[] toByteArray() {
            return ByteBuffer.allocate(iv.length + ciphertext.length)
                    .put(iv)
                    .put(ciphertext)
                    .array();
        }
    }

    /**
     * Generates a secure AES key.
     * @return A new SecretKey for AES encryption.
     * @throws NoSuchAlgorithmException If the AES algorithm is not available.
     */
    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        try {
            keyGen.init(KEY_SIZE, SecureRandom.getInstanceStrong());
        } catch (NoSuchAlgorithmException e) {
            // Fallback to default SecureRandom for compatibility
            keyGen.init(KEY_SIZE, new SecureRandom());
        }
        return keyGen.generateKey();
    }

    /**
     * Converts a Base64-encoded string to a SecretKey.
     * @param keyString The Base64-encoded key string.
     * @return The SecretKey object.
     * @throws IllegalArgumentException If the key string is invalid.
     */
    public static SecretKey stringToKey(String keyString) throws IllegalArgumentException {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(keyString);
            return new SecretKeySpec(decodedKey, ALGORITHM);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64-encoded key string", e);
        }
    }

    /**
     * Validates a SecretKey for AES encryption.
     * @param key The SecretKey to validate.
     * @return True if the key is valid for AES with the correct key size, false otherwise.
     */
    public static boolean isKeyValid(SecretKey key) {
        return key != null && ALGORITHM.equals(key.getAlgorithm()) && key.getEncoded().length * 8 == KEY_SIZE;
    }

    /**
     * Encrypts a file to another file using AES-GCM.
     * @param inputFile The source plaintext file.
     * @param outputFile The destination encrypted file.
     * @param key The secret key for encryption.
     * @throws IOException If an I/O error occurs or the input file is invalid.
     * @throws GeneralSecurityException If a cryptographic error occurs.
     */
    public static void encryptFile(Path inputFile, Path outputFile, SecretKey key)
            throws IOException, GeneralSecurityException {
        if (!Files.exists(inputFile) || !Files.isRegularFile(inputFile)) {
            throw new IOException("Input file does not exist or is not a regular file: " + inputFile);
        }
        try (InputStream in = Files.newInputStream(inputFile);
             OutputStream out = Files.newOutputStream(outputFile)) {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = generateIv();
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));

            // Write IV at the beginning of the file
            out.write(iv);

            // Encrypt content in chunks
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, bytesRead);
                if (output != null) {
                    out.write(output);
                }
            }

            // Finalize encryption
            out.write(cipher.doFinal());
        }
    }

    /**
     * Decrypts a file to another file using AES-GCM.
     * @param inputFile The source encrypted file.
     * @param outputFile The destination plaintext file.
     * @param key The secret key for decryption.
     * @throws IOException If an I/O error occurs or the input file is invalid.
     * @throws GeneralSecurityException If a cryptographic error occurs.
     */
    public static void decryptFile(Path inputFile, Path outputFile, SecretKey key)
            throws IOException, GeneralSecurityException {
        if (!Files.exists(inputFile) || !Files.isRegularFile(inputFile)) {
            throw new IOException("Input file does not exist or is not a regular file: " + inputFile);
        }
        try (InputStream in = Files.newInputStream(inputFile);
             OutputStream out = Files.newOutputStream(outputFile)) {
            // Read IV from the beginning of the file
            byte[] iv = new byte[IV_LENGTH];
            if (in.read(iv) != IV_LENGTH) {
                throw new IOException("Invalid encrypted file (missing IV): " + inputFile);
            }

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));

            // Decrypt content in chunks
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, bytesRead);
                if (output != null) {
                    out.write(output);
                }
            }

            // Finalize decryption
            out.write(cipher.doFinal());
        }
    }

    /**
     * Encrypts data in memory using AES-GCM.
     * @param plaintext The data to encrypt.
     * @param key The secret key for encryption.
     * @return The encrypted data (IV + ciphertext).
     * @throws GeneralSecurityException If a cryptographic error occurs.
     */
    public static byte[] encrypt(byte[] plaintext, SecretKey key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        byte[] iv = generateIv();
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
        return new EncryptedData(iv, cipher.doFinal(plaintext)).toByteArray();
    }

    /**
     * Decrypts data in memory using AES-GCM.
     * @param encryptedData The encrypted data (IV + ciphertext).
     * @param key The secret key for decryption.
     * @return The decrypted plaintext.
     * @throws GeneralSecurityException If a cryptographic error occurs.
     */
    public static byte[] decrypt(byte[] encryptedData, SecretKey key) throws GeneralSecurityException {
        EncryptedData data = parseEncryptedData(encryptedData);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, data.iv()));
        return cipher.doFinal(data.ciphertext());
    }

    /**
     * Generates a random initialization vector (IV).
     * @return A 12-byte IV for AES-GCM.
     */
    private static byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * Parses encrypted data into IV and ciphertext.
     * @param encrypted The encrypted data (IV + ciphertext).
     * @return An EncryptedData record containing the IV and ciphertext.
     * @throws IllegalArgumentException If the encrypted data is invalid.
     */
    private static EncryptedData parseEncryptedData(byte[] encrypted) {
        if (encrypted.length < IV_LENGTH) {
            throw new IllegalArgumentException("Encrypted data too short to contain IV");
        }
        ByteBuffer buffer = ByteBuffer.wrap(encrypted);
        byte[] iv = new byte[IV_LENGTH];
        buffer.get(iv);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);
        return new EncryptedData(iv, ciphertext);
    }

    /**
     * Converts a SecretKey to a Base64-encoded string.
     * @param key The secret key to encode.
     * @return The Base64-encoded string representation of the key.
     */
    public static String keyToString(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}