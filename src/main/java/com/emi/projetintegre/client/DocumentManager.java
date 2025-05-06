package com.emi.projetintegre.client;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.emi.projetintegre.models.PersonalDocument;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DocumentManager {
    private final Supplier<ObjectOutputStream> outputStreamSupplier;
    private final Supplier<ObjectInputStream> inputStreamSupplier;
    private final Function<Object, Boolean> sendFunction;
    private final Function<Void, Boolean> isConnectedFunction;
    private final Function<Void, Boolean> isAuthenticatedFunction;

    public DocumentManager(
        Supplier<ObjectOutputStream> outputStreamSupplier,
        Supplier<ObjectInputStream> inputStreamSupplier,
        Function<Object, Boolean> sendFunction,
        Function<Void, Boolean> isConnectedFunction,
        Function<Void, Boolean> isAuthenticatedFunction
    ) {
        this.outputStreamSupplier = outputStreamSupplier;
        this.inputStreamSupplier = inputStreamSupplier;
        this.sendFunction = sendFunction;
        this.isConnectedFunction = isConnectedFunction;
        this.isAuthenticatedFunction = isAuthenticatedFunction;
    }

    public String checkDuplicateFile(String fileName) {
        if (!isConnectedFunction.apply(null) || !isAuthenticatedFunction.apply(null)) {
            System.out.println("Not connected or not authenticated!");
            return "ERROR:NOT_AUTHENTICATED";
        }

        try {
            sendFunction.apply("CHECK_DUPLICATE_FILE");
            ObjectOutputStream output = outputStreamSupplier.get();
            output.writeUTF(fileName);
            output.flush();

            ObjectInputStream input = inputStreamSupplier.get();
            Object response = input.readObject();
            if (response instanceof String) {
                System.out.println("Check duplicate response: " + response);
                return (String) response;
            } else {
                System.out.println("Unexpected response: " + response);
                return "ERROR:UNEXPECTED_RESPONSE";
            }
        } catch (Exception e) {
            System.err.println("Error checking duplicate file: " + e.getMessage());
            e.printStackTrace();
            return "ERROR:CHECK_FAILED";
        }
    }

    public boolean uploadDocument(String filePath, String fileName) {
        if (!isConnectedFunction.apply(null) || !isAuthenticatedFunction.apply(null)) {
            System.out.println("Not connected or not authenticated!");
            return false;
        }

        try {
            filePath = filePath.replace("\"", "").trim();
            File file = new File(filePath);
            
            if (!file.exists()) {
                System.out.println("File does not exist: " + filePath);
                return false;
            }

            if (fileName == null || fileName.trim().isEmpty()) {
                System.out.println("Invalid file name");
                return false;
            }
            fileName = fileName.trim();

            String duplicateCheck = checkDuplicateFile(fileName);
            if ("DUPLICATE_FILE".equals(duplicateCheck)) {
                System.out.println("Duplicate file detected: " + fileName);
                return false;
            } else if (!"FILE_NOT_FOUND".equals(duplicateCheck)) {
                System.out.println("Error checking duplicate: " + duplicateCheck);
                return false;
            }

            String fileType = "";
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileType = fileName.substring(dotIndex + 1);
            }

            long fileSize = file.length();
            byte[] fileContent = Files.readAllBytes(file.toPath());
            LocalDateTime uploadDate = LocalDateTime.now();

            ObjectOutputStream output = outputStreamSupplier.get();
            output.writeObject("UPLOAD_FILE_WITH_METADATA");
            output.writeUTF(fileName);
            output.writeUTF(fileType);
            output.writeLong(fileSize);
            output.writeObject(uploadDate);
            output.writeObject(fileContent);
            output.flush();

            ObjectInputStream input = inputStreamSupplier.get();
            Object response = input.readObject();
            if (response instanceof String && response.equals("UPLOAD_SUCCESS")) {
                System.out.println("File uploaded successfully!");
                return true;
            }
            System.out.println("Upload failed: " + response);
            return false;

        } catch (Exception e) {
            System.err.println("Upload error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean uploadDocument(String filePath) {
        if (!isConnectedFunction.apply(null) || !isAuthenticatedFunction.apply(null)) {
            System.out.println("Not connected or not authenticated!");
            return false;
        }

        try {
            filePath = filePath.replace("\"", "").trim();
            File file = new File(filePath);
            
            if (!file.exists()) {
                System.out.println("File does not exist: " + filePath);
                return false;
            }

            String fileName = file.getName();

            String duplicateCheck = checkDuplicateFile(fileName);
            if ("DUPLICATE_FILE".equals(duplicateCheck)) {
                System.out.println("Duplicate file detected: " + fileName);
                return false;
            } else if (!"FILE_NOT_FOUND".equals(duplicateCheck)) {
                System.out.println("Error checking duplicate: " + duplicateCheck);
                return false;
            }

            long fileSize = file.length();
            String fileType = "";
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileType = fileName.substring(dotIndex + 1);
            }

            byte[] fileContent = Files.readAllBytes(file.toPath());
            LocalDateTime uploadDate = LocalDateTime.now();

            ObjectOutputStream output = outputStreamSupplier.get();
            output.writeObject("UPLOAD_FILE_WITH_METADATA");
            output.writeUTF(fileName);
            output.writeUTF(fileType);
            output.writeLong(fileSize);
            output.writeObject(uploadDate);
            output.writeObject(fileContent);
            output.flush();

            ObjectInputStream input = inputStreamSupplier.get();
            Object response = input.readObject();
            if (response instanceof String && response.equals("UPLOAD_SUCCESS")) {
                System.out.println("File uploaded successfully!");
                return true;
            }
            System.out.println("Upload failed: " + response);
            return false;

        } catch (Exception e) {
            System.err.println("Upload error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public ObservableList<PersonalDocument> getListDocuments(String query) {
        ObservableList<PersonalDocument> documents = FXCollections.observableArrayList();

        if (!isConnectedFunction.apply(null)) {
            System.err.println("Cannot retrieve documents: not connected to server");
            return documents;
        }
        if (!isAuthenticatedFunction.apply(null)) {
            System.err.println("Cannot retrieve documents: not authenticated");
            return documents;
        }

        try {
            System.out.println("Sending LIST_DOCUMENTS command to server" + (query != null ? " with query: " + query : ""));
            sendFunction.apply("LIST_DOCUMENTS");
            ObjectOutputStream output = outputStreamSupplier.get();
            output.writeUTF(query != null ? query : "");
            output.flush();

            ObjectInputStream input = inputStreamSupplier.get();
            Object response = input.readObject();
            System.out.println("Received response: " + response + 
                              " (Type: " + (response != null ? response.getClass().getName() : "null") + ")");

            if (response instanceof List) {
                @SuppressWarnings("unchecked")
                List<List<String>> docList = (List<List<String>>) response;
                System.out.println("Received " + docList.size() + " documents");

                for (List<String> doc : docList) {
                    if (doc.size() < 5) {
                        System.err.println("Invalid document format, expected 5 attributes, got: " + doc.size());
                        continue;
                    }

                    int docID;
                    try {
                        docID = Integer.parseInt(doc.get(0));
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid docID format: " + doc.get(0) + ", skipping document");
                        continue;
                    }

                    String fileName = doc.get(1);
                    String numberType = doc.get(2);
                    String uploadDateStr = doc.get(3);
                    String fileSizeStr = doc.get(4);

                    LocalDateTime uploadDate;
                    try {
                        uploadDate = LocalDateTime.parse(uploadDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } catch (Exception e) {
                        System.err.println("Invalid date format: " + uploadDateStr + ", using current date");
                        uploadDate = LocalDateTime.now();
                    }

                    long fileSize;
                    try {
                        fileSize = Long.parseLong(fileSizeStr);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid file size format: " + fileSizeStr + ", setting to 0");
                        fileSize = 0;
                    }

                    PersonalDocument document = new PersonalDocument(
                        docID,
                        fileName != null ? fileName : "Unknown",
                        null,
                        numberType != null ? numberType : "Unknown",
                        fileSize,
                        uploadDate
                    );
                    documents.add(document);
                    System.out.println("Processed document: " + document.getFileName() + 
                                      ", type: " + document.getNumberType() + 
                                      ", date: " + document.getUploadDate() + 
                                      ", size: " + document.getSize());
                }
            } else {
                System.err.println("Expected List<List<String>>, got: " + 
                                  (response != null ? response.getClass().getName() : "null"));
                return documents;
            }

            Object status = input.readObject();
            System.out.println("Status response: " + status + 
                              " (Type: " + (status != null ? status.getClass().getName() : "null") + ")");
            if (!(status instanceof String) || !status.equals("LIST_SUCCESS")) {
                System.err.println("Invalid or missing LIST_SUCCESS response: " + status);
                return documents;
            }

        } catch (InvalidClassException | ClassNotFoundException e) {
            System.err.println("Serialization error in getListDocuments: " + e.getMessage());
            e.printStackTrace();
            return documents;
        } catch (IOException e) {
            System.err.println("IO error in getListDocuments: " + e.getMessage());
            e.printStackTrace();
            return documents;
        }

        System.out.println("Returning " + documents.size() + " documents");
        return documents;
    }

    public void showDocuments(String query) {
        if (!isConnectedFunction.apply(null) || !isAuthenticatedFunction.apply(null)) {
            System.out.println("Not connected or not authenticated!");
            return;
        }

        try {
            System.out.println("Sending LIST_DOCUMENTS command to server" + (query != null ? " with query: " + query : ""));
            sendFunction.apply("LIST_DOCUMENTS");
            ObjectOutputStream output = outputStreamSupplier.get();
            output.writeUTF(query != null ? query : "");
            output.flush();

            ObjectInputStream input = inputStreamSupplier.get();
            Object response = input.readObject();
            if (response instanceof List) {
                @SuppressWarnings("unchecked")
                List<List<String>> documents = (List<List<String>>) response;
                System.out.println("\nAvailable Documents:");
                for (List<String> doc : documents) {
                    if (doc.size() < 5) {
                        System.err.println("Invalid document format, expected 5 attributes, got: " + doc.size());
                        continue;
                    }

                    String docID = doc.get(0);
                    String fileName = doc.get(1);
                    String numberType = doc.get(2);
                    String uploadDateStr = doc.get(3);
                    String fileSizeStr = doc.get(4);

                    LocalDateTime uploadDate;
                    try {
                        uploadDate = LocalDateTime.parse(uploadDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } catch (Exception e) {
                        System.err.println("Invalid date format: " + uploadDateStr + ", using current date");
                        uploadDate = LocalDateTime.now();
                    }

                    long fileSize;
                    try {
                        fileSize = Long.parseLong(fileSizeStr);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid file size format: " + fileSizeStr + ", setting to 0");
                        fileSize = 0;
                    }

                    System.out.println("Document ID: " + docID +
                                     ", File: " + (fileName != null ? fileName : "Unknown") + 
                                     ", Type: " + (numberType != null ? numberType : "Unknown") + 
                                     ", Upload Date: " + uploadDate + 
                                     ", Size: " + fileSize + " bytes");
                }
            } else {
                System.out.println("No documents received or invalid response format: " + 
                                  (response != null ? response.getClass().getName() : "null"));
            }

            Object status = input.readObject();
            if (!(status instanceof String) || !status.equals("LIST_SUCCESS")) {
                System.out.println("Failed to retrieve document list: " + status);
            }
        } catch (Exception e) {
            System.err.println("Error showing documents: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean downloadDocument(int docID, String saveDirectory) {
        if (!isConnectedFunction.apply(null) || !isAuthenticatedFunction.apply(null)) {
            System.out.println("Not connected or not authenticated!");
            return false;
        }

        try {
            saveDirectory = saveDirectory.replace("\"", "").trim();
            Path savePath = Paths.get(saveDirectory);
            if (!Files.exists(savePath) || !Files.isDirectory(savePath)) {
                System.out.println("Invalid save directory: " + saveDirectory);
                return false;
            }
            
            sendFunction.apply("DOWNLOAD_DOCUMENT");
            sendFunction.apply(docID);
            
            ObjectInputStream input = inputStreamSupplier.get();
            Object response = input.readObject();
            if (response instanceof String && response.equals("DOWNLOAD_NOT_IMPLEMENTED")) {
                System.out.println("Download functionality not implemented on server");
                return false;
            } else if (response instanceof String) {
                System.out.println("Download failed: " + response);
                return false;
            } else {
                System.out.println("Unexpected response format: " + response);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Download error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}