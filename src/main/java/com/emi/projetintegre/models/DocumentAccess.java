package com.emi.projetintegre.models;

import java.time.LocalDate;

public class DocumentAccess {
    private int accessID;
    private LocalDate grantedDate;

    public DocumentAccess(int accessID, LocalDate grantedDate) {
        this.accessID = accessID;
        this.grantedDate = grantedDate;
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
        this.grantedDate = grantedDate;
    }

    @Override
    public String toString() {
        return "DocumentAccess{" +
               "accessID=" + accessID +
               ", grantedDate=" + grantedDate +
               '}';
    }
}
