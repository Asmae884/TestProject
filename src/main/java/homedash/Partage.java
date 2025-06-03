package homedash;

/*public class Partage {
    private String nom;
    private String proprietaire;
    private String datePartage;
    private String taille;
    private String permissions;

    public Partage(String nom, String proprietaire, String datePartage, String taille, String permissions) {
        this.nom = nom;
        this.proprietaire = proprietaire;
        this.datePartage = datePartage;
        this.taille = taille;
        this.permissions = permissions;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getProprietaire() {
        return proprietaire;
    }

    public void setProprietaire(String proprietaire) {
        this.proprietaire = proprietaire;
    }

    public String getDatePartage() {
        return datePartage;
    }

    public void setDatePartage(String datePartage) {
        this.datePartage = datePartage;
    }

    public String getTaille() {
        return taille;
    }

    public void setTaille(String taille) {
        this.taille = taille;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
}
*/

public class Partage {
    private String nom;
    private String proprietaire;
    private String datePartage;
    private String taille;
    private String permissions;

    public Partage(String nom, String proprietaire, String datePartage, String taille, String permissions) {
        this.nom = nom;
        this.proprietaire = proprietaire;
        this.datePartage = datePartage;
        this.taille = taille;
        this.permissions = permissions;
    }

    // Getters et setters
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getProprietaire() { return proprietaire; }
    public void setProprietaire(String proprietaire) { this.proprietaire = proprietaire; }

    public String getDatePartage() { return datePartage; }
    public void setDatePartage(String datePartage) { this.datePartage = datePartage; }

    public String getTaille() { return taille; }
    public void setTaille(String taille) { this.taille = taille; }

    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
}

