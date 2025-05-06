package com.emi.projetintegre.models;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

import com.emi.projetintegre.services.PasswordManager;

public abstract class User {
    protected int userID;
    protected String login;
    protected String password; // hashed
    protected LocalDate creationDatePassword;
    protected LocalDate validityDatePassword;
    protected PasswordManager passwordManager;

    // Constructeur par défaut
    public User() {
    }

    // Constructeur avec paramètres
    public User(int userID, String login, String password, LocalDate creationDatePassword, LocalDate validityDatePassword) throws NoSuchAlgorithmException {
        this.userID = userID;
        this.login = login;
        setPassword(password);  // Appel du setter pour hacher le mot de passe
        this.creationDatePassword = creationDatePassword;
        this.validityDatePassword = validityDatePassword;
    }

    // Getter et Setter pour userID
    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    // Getter et Setter pour login
    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    // Getter et Setter pour password (avec hachage)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) throws NoSuchAlgorithmException {
        this.password = passwordManager.hashPassword(password);  // Hachage du mot de passe lors de l'appel du setter
    }

    // Getter et Setter pour creationDatePassword
    public LocalDate getCreationDatePassword() {
        return creationDatePassword;
    }

    public void setCreationDatePassword(LocalDate creationDatePassword) {
        this.creationDatePassword = creationDatePassword;
    }

    // Getter et Setter pour validityDatePassword
    public LocalDate getValidityDatePassword() {
        return validityDatePassword;
    }

    public void setValidityDatePassword(LocalDate validityDatePassword) {
        this.validityDatePassword = validityDatePassword;
    }

    // Méthode toString
    @Override
    public String toString() {
        return "User{" +
                "userID=" + userID +
                ", login='" + login + '\'' +
                ", creationDatePassword=" + creationDatePassword +
                ", validityDatePassword=" + validityDatePassword +
                '}';
    }
}
