package com.emi.projetintegre.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PersonalDocument implements Serializable {
    private static final long serialVersionUID = 1L;
    private int docID;
    private String fileName;
    private byte[] content;
    private String numberType;
    private long size;
    private LocalDateTime uploadDate;

    public PersonalDocument() {}

    public PersonalDocument(int docID, String fileName, byte[] content, String numberType, long size, LocalDateTime uploadDate) {
        this.docID = docID;
        this.fileName = fileName;
        this.content = content;
        this.numberType = numberType;
        this.size = size;
        this.uploadDate = uploadDate;
    }

    // New constructor for listing documents without content
    public PersonalDocument(int docID, String fileName, String numberType, LocalDateTime uploadDate, long size) {
        this.docID = docID;
        this.fileName = fileName;
        this.numberType = numberType;
        this.uploadDate = uploadDate;
        this.size = size;
        this.content = null; // Explicitly set to null
    }

    public int getDocID() {
        return this.docID;
    }

    public void setDocID(int docID) {
        this.docID = docID;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getContent() {
        return this.content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getNumberType() {
        return this.numberType;
    }

    public void setNumberType(String numberType) {
        this.numberType = numberType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    @Override
    public String toString() {
        return "PersonalDocument{" +
               "docID=" + docID +
               ", fileName='" + fileName + '\'' +
               ", numberType='" + numberType + '\'' +
               ", size=" + size +
               ", uploadDate=" + uploadDate +
               '}';
    }
}