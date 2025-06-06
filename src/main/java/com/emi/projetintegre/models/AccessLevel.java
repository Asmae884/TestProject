package com.emi.projetintegre.models;

public enum AccessLevel {
    READ_ONLY(1, true, false, false, false),
    READ_WRITE(2, true, true, false, false),
    READ_DOWNLOAD(3, true, false, true, false),
    READ_DELETE(4, true, false, false, true),
    READ_WRITE_DOWNLOAD(5, true, true, true, false),
    READ_WRITE_DELETE(6, true, true, false, true),
    ALL_PERMISSIONS(7, true, true, true, true);

    private final int id;
    private final boolean canRead;
    private final boolean canWrite;
    private final boolean canDownload;
    private final boolean canDelete;

    AccessLevel(int id, boolean canRead, boolean canWrite, boolean canDownload, boolean canDelete) {
        this.id = id;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.canDownload = canDownload;
        this.canDelete = canDelete;
    }

    public static AccessLevel fromId(int id) {
        for (AccessLevel level : values()) {
            if (level.getId() == id) {
                return level;
            }
        }
        throw new IllegalArgumentException("No AccessLevel found for id: " + id);
    }

    public int getId() {
        return id;
    }

    public boolean canRead() {
        return canRead;
    }

    public boolean canWrite() {
        return canWrite;
    }

    public boolean canDownload() {
        return canDownload;
    }

    public boolean canDelete() {
        return canDelete;
    }

    @Override
    public String toString() {
        return name().toLowerCase().replace('_', ' ');
    }
}