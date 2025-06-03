package homedash;

import java.time.LocalDateTime;

public class SharedFile {
    private String fileName;
    private String owner;
    private LocalDateTime shareDate;
    private long fileSize;
    private String permissions;

    public SharedFile(String fileName, String owner, LocalDateTime shareDate, long fileSize, String permissions) {
        this.fileName = fileName;
        this.owner = owner;
        this.shareDate = shareDate;
        this.fileSize = fileSize;
        this.permissions = permissions;
    }

    // Getters
    public String getFileName() { return fileName; }
    public String getOwner() { return owner; }
    public LocalDateTime getShareDate() { return shareDate; }
    public long getFileSize() { return fileSize; }
    public String getPermissions() { return permissions; }

    // Setters (optionnels)
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setShareDate(LocalDateTime shareDate) { this.shareDate = shareDate; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
}