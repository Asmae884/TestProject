package com.emi.projetintegre.models;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class UserPermission {
    private final User user;
    private final ObjectProperty<AccessLevel> accessLevel;
    private PersonalDocument personalDocument;

    // Constructor associating User, AccessLevel, and PersonalDocument
    public UserPermission(User user, AccessLevel accessLevel, PersonalDocument personalDocument) {
        this.user = user;
        this.accessLevel = new SimpleObjectProperty<>(accessLevel);
        this.personalDocument = personalDocument;
    }

    // Getter for user
    public User getUser() {
        return user;
    }

    // Getter and setter for accessLevel
    public AccessLevel getAccessLevel() {
        return accessLevel.get();
    }

    public ObjectProperty<AccessLevel> accessLevelProperty() {
        return accessLevel;
    }

    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel.set(accessLevel);
    }

    // Getter for personalDocument
    public PersonalDocument getPersonalDocument() {
        return personalDocument;
    }

    // Setter for personalDocument
    public void setPersonalDocument(PersonalDocument personalDocument) {
        this.personalDocument = personalDocument;
    }

    // Convenience methods to check permissions
    public boolean canRead() {
        return accessLevel.get().canRead();
    }

    public boolean canWrite() {
        return accessLevel.get().canWrite();
    }

    public boolean canDownload() {
        return accessLevel.get().canDownload();
    }

    public boolean canDelete() {
        return accessLevel.get().canDelete();
    }

    // Get the access level ID
    public int getAccessLevelId() {
        return accessLevel.get().getId();
    }

    @Override
    public String toString() {
        return "UserPermission{" +
                "user=" + user.getLogin() +
                ", accessLevel=" + accessLevel.get() +
                ", personalDocument=" + (personalDocument != null ? personalDocument.getFileName() : "null") +
                '}';
    }
}
