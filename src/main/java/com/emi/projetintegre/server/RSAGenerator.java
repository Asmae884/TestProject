package com.emi.projetintegre.server;

import java.security.*;
import java.util.*;
import java.util.Base64;

public class RSAGenerator {
    public static void main(String[] args) {
        try {
            int numPairs = 4;
            List<String> publicKeys = new ArrayList<>();
            List<String> privateKeys = new ArrayList<>();

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048); // Key size

            for (int i = 0; i < numPairs; i++) {
                KeyPair pair = keyGen.generateKeyPair();
                PublicKey publicKey = pair.getPublic();
                PrivateKey privateKey = pair.getPrivate();

                String pubEncoded = Base64.getEncoder().encodeToString(publicKey.getEncoded());
                String privEncoded = Base64.getEncoder().encodeToString(privateKey.getEncoded());

                publicKeys.add(pubEncoded);
                privateKeys.add(privEncoded);

                System.out.println("🔑 Pair #" + (i + 1));
                System.out.println("Public Key:\n" + pubEncoded);
                System.out.println("Private Key:\n" + privEncoded);
                System.out.println("--------------------------------------------------");
            }

            // You now have publicKeys and privateKeys lists for later use or DB storage

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}