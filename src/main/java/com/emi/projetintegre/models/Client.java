package com.emi.projetintegre.models;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

public class Client extends User {
    private LocalDate creationDate;
    private LocalDate validityDate;
    private String cle;

    public Client() {
    	
    }
    
    public Client(int userID, String login, String password, LocalDate creationDatePassword, LocalDate validityDatePassword,
            LocalDate creationDate, LocalDate validityDate, String cle) throws NoSuchAlgorithmException {
    	super(userID, login, password, creationDatePassword, validityDatePassword);
    	this.creationDate = creationDate;
    	this.validityDate = validityDate;
    	this.cle = cle;
	}


    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDate getValidityDate() {
        return validityDate;
    }

    public void setValidityDate(LocalDate validityDate) {
        this.validityDate = validityDate;
    }

    public String getCle() {
        return cle;
    }

    public void setCle(String cle) {
        this.cle = cle;
    }

    @Override
    public String toString() {
        return "Client{" +
                "userID=" + getUserID() +
                ", login='" + getLogin() + '\'' +
                ", creationDatePassword=" + getCreationDatePassword() +
                ", validityDatePassword=" + getValidityDatePassword() +
                ", creationDate=" + creationDate +
                ", validityDate=" + validityDate +
                ", cle='" + cle + '\'' +
                '}';
    }
}
