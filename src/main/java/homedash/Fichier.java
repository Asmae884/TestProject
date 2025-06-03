package homedash;

import java.time.LocalDateTime;

public class Fichier {
    private String nom;
    private String type;
    private Long taille;
    private LocalDateTime dateUpload;
    private boolean lirePermission;
    private boolean ecrirePermission;
    private boolean chargerPermission;
    private boolean supprimerPermission;
    
    public Fichier(String nom, String type, Long taille, LocalDateTime dateUpload) {
        this.nom = nom;
        this.type = type;
        this.taille = taille;
        this.dateUpload = dateUpload;
    }

    // Getters et setters
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getTaille() { return taille; }
    public void setTaille(Long taille) { this.taille = taille; }
    public LocalDateTime getDateUpload() { return dateUpload; }
    public void setDateUpload(LocalDateTime dateUpload) { this.dateUpload = dateUpload; }

    // ... constructeur et getters/setters existants ...

    // Ajouter les getters/setters pour les permissions
    public boolean hasLirePermission() { return lirePermission; }
    public void setLirePermission(boolean lirePermission) { this.lirePermission = lirePermission; }
    
    public boolean hasEcrirePermission() { return ecrirePermission; }
    public void setEcrirePermission(boolean ecrirePermission) { this.ecrirePermission = ecrirePermission; }
    
    public boolean hasChargerPermission() { return chargerPermission; }
    public void setChargerPermission(boolean chargerPermission) { this.chargerPermission = chargerPermission; }
    
    public boolean hasSupprimerPermission() { return supprimerPermission; }
    public void setSupprimerPermission(boolean supprimerPermission) { this.supprimerPermission = supprimerPermission; }
}



