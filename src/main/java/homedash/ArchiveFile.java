package homedash;
import javafx.beans.property.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class ArchiveFile {
    private final IntegerProperty archiveID;
    private final StringProperty fileName;
    private final StringProperty fileHash;
    private final ObjectProperty<LocalDateTime> uploadDate;
    private final ObjectProperty<LocalDateTime> modifyDate;
    private final IntegerProperty ownerId;
    private final StringProperty fileType;
    private final LongProperty fileSize;
    private final StringProperty filePath;
    private final ObjectProperty<LocalDateTime> deleteDate;

    public ArchiveFile(
            int archiveID,
            String fileName,
            String fileHash,
            LocalDateTime uploadDate,
            LocalDateTime modifyDate,
            int ownerId,
            String fileType,
            long fileSize,
            String filePath,
            LocalDateTime deleteDate) {
        
        this.archiveID = new SimpleIntegerProperty(archiveID);
        this.fileName = new SimpleStringProperty(fileName);
        this.fileHash = new SimpleStringProperty(fileHash);
        this.uploadDate = new SimpleObjectProperty<>(uploadDate);
        this.modifyDate = new SimpleObjectProperty<>(modifyDate);
        this.ownerId = new SimpleIntegerProperty(ownerId);
        this.fileType = new SimpleStringProperty(fileType);
        this.fileSize = new SimpleLongProperty(fileSize);
        this.filePath = new SimpleStringProperty(filePath);
        this.deleteDate = new SimpleObjectProperty<>(deleteDate);
    }

    // Getters pour les propriétés (nécessaires pour PropertyValueFactory)
    public IntegerProperty archiveIDProperty() { return archiveID; }
    public StringProperty fileNameProperty() { return fileName; }
    public StringProperty fileHashProperty() { return fileHash; }
    public ObjectProperty<LocalDateTime> uploadDateProperty() { return uploadDate; }
    public ObjectProperty<LocalDateTime> modifyDateProperty() { return modifyDate; }
    public IntegerProperty ownerIdProperty() { return ownerId; }
    public StringProperty fileTypeProperty() { return fileType; }
    public LongProperty fileSizeProperty() { return fileSize; }
    public StringProperty filePathProperty() { return filePath; }
    public ObjectProperty<LocalDateTime> deleteDateProperty() { return deleteDate; }

    // Getters standards
    public int getArchiveID() { return archiveID.get(); }
    public String getFileName() { return fileName.get(); }
    public String getFileHash() { return fileHash.get(); }
    public LocalDateTime getUploadDate() { return uploadDate.get(); }
    public LocalDateTime getModifyDate() { return modifyDate.get(); }
    public int getOwnerId() { return ownerId.get(); }
    public String getFileType() { return fileType.get(); }
    public long getFileSize() { return fileSize.get(); }
    public String getFilePath() { return filePath.get(); }
    public LocalDateTime getDeleteDate() { return deleteDate.get(); }

    // Setters (optionnels selon besoins)
    public void setFilePath(String value) { filePath.set(value); }
    public void setDeleteDate(LocalDateTime value) { deleteDate.set(value); }
}