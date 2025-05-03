package homedash;
import java.time.LocalDateTime;



public class Fichier {
    private String nom;
    private String type;
    private Long taille;
    private LocalDateTime dateUpload;

    public Fichier(String nom,String type, Long taille, LocalDateTime dateUpload) {
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
}


