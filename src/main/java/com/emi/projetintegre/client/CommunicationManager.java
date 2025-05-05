package com.emi.projetintegre.client;

public interface CommunicationManager {
    void connect();
    boolean send(Object data);
    boolean uploadDocument(String filePath, String fileName);
}