package com.emi.projetintegre.models;

import java.time.LocalDate;
import java.util.Objects;

public class DocumentAccess {
    private int accessID;
    private LocalDate grantedDate;
    private LocalDate validityDate;
    private boolean read = true;
    private boolean write;
    private boolean download;
    private boolean delete;

    public DocumentAccess() {
    }
    
    public DocumentAccess(int accessID, LocalDate grantedDate, boolean read, boolean write, boolean download, boolean delete) {
        this.accessID = accessID;
        this.grantedDate = grantedDate;
        this.read = read;
        this.write = write;
        this.download = download;
        this.delete = delete;
    }

    public int getAccessID() {
        return accessID;
    }

    public void setAccessID(int accessID) {
        this.accessID = accessID;
    }

    public LocalDate getGrantedDate() {
        return grantedDate;
    }

    public void setGrantedDate(LocalDate grantedDate) {
        if (grantedDate == null || grantedDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Granted date cannot be null or in the future");
        }
        this.grantedDate = grantedDate;
    }

	public boolean isDelete() {
		return delete;
	}

	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	public boolean isDownload() {
		return download;
	}

	public void setDownload(boolean download) {
		this.download = download;
	}

	public boolean isWrite() {
		return write;
	}

	public void setWrite(boolean write) {
		this.write = write;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}
	
    public LocalDate getValidityDate() { 
    	return validityDate; 
    }
    
    public void setValidityDate(LocalDate validityDate) { 
    	this.validityDate = validityDate; 
    }
	
    public String toString() {
        return "DocumentAccess{" +
               "accessID=" + this.accessID +
               ", grantedDate=" + this.grantedDate +
               ", validityDate=" + this.validityDate +
                ", read=" + this.read +
                ", write=" + this.write +
                ", download=" + this.download +
                ", delete=" + this.delete +
               '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentAccess that = (DocumentAccess) o;
        return accessID == that.accessID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessID);
    }
}
