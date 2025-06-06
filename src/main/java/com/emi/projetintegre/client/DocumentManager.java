package com.emi.projetintegre.client;

import com.emi.projetintegre.models.PersonalDocument;
import com.emi.projetintegre.services.FileSecurity;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class DocumentManager {
    private final Supplier<ObjectOutputStream> outputStreamSupplier;
    private final Supplier<ObjectInputStream> inputStreamSupplier;
    private final Function<Object, Boolean> sendFunction;
    private final Function<Void, Boolean> isConnectedFunction;
    private final Function<Void, Boolean> isAuthenticatedFunction;
    private final FileSecurity fileSecurity;

    public DocumentManager(
        Supplier<ObjectOutputStream> outputStreamSupplier,
        Supplier<ObjectInputStream> inputStreamSupplier,
        Function<Object, Boolean> sendFunction,
        Function<Void, Boolean> isConnectedFunction,
        Function<Void, Boolean> isAuthenticatedFunction,
        FileSecurity fileSecurity
    ) {
        this.outputStreamSupplier = outputStreamSupplier;
        this.inputStreamSupplier = inputStreamSupplier;
        this.sendFunction = sendFunction;
        this.isConnectedFunction = isConnectedFunction;
        this.isAuthenticatedFunction = isAuthenticatedFunction;
        this.fileSecurity = fileSecurity;
    }

    public String checkDuplicateFile(String fileName) {
        if (!isConnectedFunction.apply(null) || !isAuthenticatedFunction.apply(null)) {
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
                return (String) response;
            }
            return "ERROR:UNEXPECTED_RESPONSE";
        } catch (Exception e) {
            System.err.println("Check duplicate error: " + e.getMessage());
            return "ERROR:CHECK_FAILED";
        }
    }

    private boolean uploadFileInternal(String filePath, String fileName, boolean encrypt) {
        if (!isConnectedFunction.apply(null) || !isAuthenticatedFunction.apply(null)) {
            return false;
        }

        try {
            filePath = filePath.replace("\"", "").trim();
            Path inputPath = Paths.get(filePath);

            if (!Files.exists(inputPath) || !Files.isRegularFile(inputPath)) {
                return false;
            }

            if (fileName == null || fileName.trim().isEmpty()) {
                return false;
            }
            fileName = fileName.trim();

            String duplicateCheck = checkDuplicateFile(fileName);
            if ("DUPLICATE_FILE".equals(duplicateCheck) || !"FILE_NOT_FOUND".equals(duplicateCheck)) {
                return false;
            }

            String fileType = "";
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileType = fileName.substring(dotIndex + 1);
            }

            byte[] fileContent;
            long fileSize;
            String keyString = "";
            LocalDateTime uploadDate = LocalDateTime.now();
            String command = encrypt ? "UPLOAD_ENCRYPTED_FILE_WITH_METADATA" : "UPLOAD_FILE_WITH_METADATA";

            if (encrypt) {
                fileSecurity.generateKey();
                Path tempEncryptedFile = Files.createTempFile("encrypted_", ".bin");
                try {
                    fileSecurity.encryptFile(inputPath, tempEncryptedFile);
                    fileContent = Files.readAllBytes(tempEncryptedFile);
                    fileSize = fileContent.length;
                    keyString = fileSecurity.getKeyAsString();
                } finally {
                    Files.deleteIfExists(tempEncryptedFile);
                }
            } else {
                fileContent = Files.readAllBytes(inputPath);
                fileSize = fileContent.length;
            }

            ObjectOutputStream output = outputStreamSupplier.get();
            output.writeObject(command);
            output.writeUTF(fileName);
            output.writeUTF(fileType);
            output.writeLong(fileSize);
            output.writeObject(uploadDate);
            if (encrypt) {
                output.writeUTF(keyString);
            }
            output.writeObject(fileContent);
            output.flush();

            ObjectInputStream input = inputStreamSupplier.get();
            Object response = input.readObject();
            return response instanceof String && response.equals("UPLOAD_SUCCESS");
        } catch (Exception e) {
            System.err.println((encrypt ? "Encrypted upload" : "Upload") + " error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean uploadDocument(String filePath, String fileName) {
        return uploadFileInternal(filePath, fileName, false);
    }

    public boolean uploadDocument(String filePath) {
        File file = new File(filePath.replace("\"", "").trim());
        return uploadDocument(filePath, file.getName());
    }

    public boolean uploadEncryptedDocument(String filePath, String fileName) {
        return uploadFileInternal(filePath, fileName, true);
    }

    public boolean uploadEncryptedDocument(String filePath) {
        File file = new File(filePath.replace("\"", "").trim());
        return uploadEncryptedDocument(filePath, file.getName());
    }

    public ObservableList<PersonalDocument> getListDocuments(String query) {
        ObservableList<PersonalDocument> documents = FXCollections.observableArrayList();

        if (!isConnectedFunction.apply(null) || !isAuthenticatedFunction.apply(null)) {
            return documents;
        }

        try {
            sendFunction.apply("LIST_DOCUMENTS");
            ObjectOutputStream output = outputStreamSupplier.get();
            output.writeUTF(query != null ? query : "");
            output.flush();

            ObjectInputStream input = inputStreamSupplier.get();
            Object response = input.readObject();
            if (response instanceof List) {
                @SuppressWarnings("unchecked")
                List<List<String>> docList = (List<List<String>>) response;
                for (List<String> doc : docList) {
                    if (doc.size() < 5) {
                        continue;
                    }

                    int docID;
                    try {
                        docID = Integer.parseInt(doc.get(0));
                    } catch (NumberFormatException e) {
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
                        uploadDate = LocalDateTime.now();
                    }

                    long fileSize;
                    try {
                        fileSize = Long.parseLong(fileSizeStr);
                    } catch (NumberFormatException e) {
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
                }
            }

            Object status = input.readObject();
            if (!(status instanceof String) || !status.equals("LIST_SUCCESS")) {
                return documents;
            }
            return documents;
        } catch (Exception e) {
            System.err.println("List documents error: " + e.getMessage());
            e.printStackTrace();
            return documents;
        }
    }

    public ObservableList<PersonalDocument> getListSharedWithMeDocuments(String query) {
        ObservableList<PersonalDocument> documents = FXCollections.observableArrayList();

        if (!isConnectedFunction.apply(null) || !isAuthenticatedFunction.apply(null)) {
            return documents;
        }

        try {
            sendFunction.apply("LIST_SHARED_WITH_ME_DOCUMENTS");
            ObjectOutputStream output = outputStreamSupplier.get();
            output.writeUTF(query != null ? query : "");
            output.flush();

            ObjectInputStream input = inputStreamSupplier.get();
            Object response = input.readObject();
            if (response instanceof List) {
                @SuppressWarnings("unchecked")
                List<List<String>> docList = (List<List<String>>) response;
                for (List<String> doc : docList) {
                    if (doc.size() < 5) {
                        continue;
                    }

                    int docID;
                    try {
                        docID = Integer.parseInt(doc.get(0));
                    } catch (NumberFormatException e) {
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
                        uploadDate = LocalDateTime.now();
                    }

                    long fileSize;
                    try {
                        fileSize = Long.parseLong(fileSizeStr);
                    } catch (NumberFormatException e) {
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
                }
            }

            Object status = input.readObject();
            if (!(status instanceof String) || !status.equals("LIST_SHARED_WITH_ME_SUCCESS")) {
                return documents;
            }
            return documents;
        } catch (Exception e) {
            System.err.println("List documents error: " + e.getMessage());
            e.printStackTrace();
            return documents;
        }
    }

    public boolean downloadDocument(int docID, String saveDirectory) {
        if (!isConnectedFunction.apply(null) || !isAuthenticatedFunction.apply(null)) {
            return false;
        }

        try {
            saveDirectory = saveDirectory.replace("\"", "").trim();
            Path savePath = Paths.get(saveDirectory);
            if (!Files.exists(savePath) || !Files.isDirectory(savePath)) {
                return false;
            }

            sendFunction.apply("DOWNLOAD_DOCUMENT");
            ObjectOutputStream output = outputStreamSupplier.get();
            output.writeObject(docID);
            output.flush();

            ObjectInputStream input = inputStreamSupplier.get();
            Object response = input.readObject();
            if (!(response instanceof List)) {
                System.err.println("Invalid download response format");
                return false;
            }

            @SuppressWarnings("unchecked")
            List<Object> responseData = (List<Object>) response;
            if (responseData.size() < 2) {  // Changed from 3 to 2 since we now only expect filename and content
                System.err.println("Incomplete download response");
                return false;
            }

            String fileName = (String) responseData.get(0);
            byte[] fileContent = (byte[]) responseData.get(1);

            Path outputFile = savePath.resolve(fileName);
            // Just write the file directly - no encryption handling
            Files.write(outputFile, fileContent);

            Object status = input.readObject();
            return status instanceof String && status.equals("DOWNLOAD_SUCCESS");
        } catch (Exception e) {
            System.err.println("Download error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}