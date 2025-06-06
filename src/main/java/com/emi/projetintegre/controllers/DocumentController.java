package com.emi.projetintegre.controllers;

import com.emi.projetintegre.client.DocumentManager;
import com.emi.projetintegre.models.PersonalDocument;
import javafx.collections.ObservableList;

public class DocumentController {
    private final DocumentManager documentManager;

    public DocumentController(DocumentManager documentManager) {
        this.documentManager = documentManager;
    }

    public ObservableList<PersonalDocument> getDocuments(String query) {
        return documentManager.getListDocuments(query);
    }
    
    public ObservableList<PersonalDocument> getSharedWithMeDocuments(String query) {
        return documentManager.getListSharedWithMeDocuments(query);
    }

    public boolean uploadDocument(String filePath, String fileName) {
        boolean success = documentManager.uploadDocument(filePath, fileName);
        if (success) {
            System.out.println("File uploaded successfully: " + fileName);
        } else {
            System.out.println("Failed to upload file: " + fileName);
        }
        return success;
    }

    public boolean uploadDocument(String filePath) {
        boolean success = documentManager.uploadDocument(filePath);
        if (success) {
            System.out.println("File uploaded successfully: " + new java.io.File(filePath).getName());
        } else {
            System.out.println("Failed to upload file: " + new java.io.File(filePath).getName());
        }
        return success;
    }

    public boolean uploadEncryptedDocument(String filePath, String fileName) {
        boolean success = documentManager.uploadEncryptedDocument(filePath, fileName);
        if (success) {
            System.out.println("Encrypted file uploaded successfully: " + fileName);
        } else {
            System.out.println("Failed to upload encrypted file: " + fileName);
        }
        return success;
    }

    public boolean uploadEncryptedDocument(String filePath) {
        boolean success = documentManager.uploadEncryptedDocument(filePath);
        if (success) {
            System.out.println("Encrypted file uploaded successfully: " + new java.io.File(filePath).getName());
        } else {
            System.out.println("Failed to upload encrypted file: " + new java.io.File(filePath).getName());
        }
        return success;
    }

    public boolean downloadDocument(int docID, String saveDirectory) {
        boolean success = documentManager.downloadDocument(docID, saveDirectory);
        if (success) {
            System.out.println("File downloaded successfully: ID " + docID);
        } else {
            System.out.println("Failed to download file: ID " + docID);
        }
        return success;
    }

    public void showDocuments(String query) {
        ObservableList<PersonalDocument> documents = getDocuments(query);
        if (documents.isEmpty()) {
            System.out.println("No documents received or error occurred.");
            return;
        }

        System.out.println("\nAvailable Documents:");
        for (PersonalDocument doc : documents) {
            System.out.println("Document ID: " + doc.getDocID() +
                             ", File: " + (doc.getFileName() != null ? doc.getFileName() : "Unknown") +
                             ", Type: " + (doc.getNumberType() != null ? doc.getNumberType() : "Unknown") +
                             ", Upload Date: " + doc.getUploadDate() +
                             ", Size: " + doc.getSize() + " bytes");
        }
    }
    
    public void showDocumentsSharedWithMe(String query) {
        ObservableList<PersonalDocument> documents = getSharedWithMeDocuments(query);
        if (documents.isEmpty()) {
            System.out.println("No Shared documents received or error occurred.");
            return;
        }

        System.out.println("\nAvailable Documents:");
        for (PersonalDocument doc : documents) {
            System.out.println("Document ID: " + doc.getDocID() +
                             ", File: " + (doc.getFileName() != null ? doc.getFileName() : "Unknown") +
                             ", Type: " + (doc.getNumberType() != null ? doc.getNumberType() : "Unknown") +
                             ", Upload Date: " + doc.getUploadDate() +
                             ", Size: " + doc.getSize() + " bytes");
        }
    }
}