package homedash;

/*import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


public class UtilisateurPermission {
    private final StringProperty nomUtilisateur;
    private final BooleanProperty lire;
    private final BooleanProperty ecrire;
    private final BooleanProperty charger;
    private final BooleanProperty supprimer;


    public UtilisateurPermission(String nomUtilisateur, boolean lire, boolean ecrire, boolean charger, boolean supprimer) {
        this.nomUtilisateur = new SimpleStringProperty(nomUtilisateur);
        this.lire = new SimpleBooleanProperty(lire);
        this.ecrire = new SimpleBooleanProperty(ecrire);
        this.charger = new SimpleBooleanProperty(charger);
        this.supprimer = new SimpleBooleanProperty(supprimer);
    }

    public String getNomUtilisateur() {
        return nomUtilisateur.get();
    }

    public void setNomUtilisateur(String nomUtilisateur) {
        if (nomUtilisateur == null || nomUtilisateur.isEmpty()) {
            throw new IllegalArgumentException("Le nom d'utilisateur ne peut pas être vide.");
        }
        this.nomUtilisateur.set(nomUtilisateur);
    }

    public StringProperty nomUtilisateurProperty() {
        return nomUtilisateur;
    }

    public boolean isLire() {
        return lire.get();
    }

    public void setLire(boolean lire) {
        this.lire.set(lire);
    }

    public BooleanProperty lireProperty() {
        return lire;
    }

    public boolean isEcrire() {
        return ecrire.get();
    }

    public void setEcrire(boolean ecrire) {
        this.ecrire.set(ecrire);
    }

    public BooleanProperty ecrireProperty() {
        return ecrire;
    }

    public boolean isCharger() {
        return charger.get();
    }

    public void setCharger(boolean charger) {
        this.charger.set(charger);
    }

    public BooleanProperty chargerProperty() {
        return charger;
    }

    public boolean isSupprimer() {
        return supprimer.get();
    }

    public void setSupprimer(boolean supprimer) {
        this.supprimer.set(supprimer);
    }

    public BooleanProperty supprimerProperty() {
        return supprimer;
    }

    public void setPermissions(boolean lire, boolean ecrire, boolean charger, boolean supprimer) {
        this.lire.set(lire);
        this.ecrire.set(ecrire);
        this.charger.set(charger);
        this.supprimer.set(supprimer);
    }

    @Override
    public String toString() {
        return "UtilisateurPermission{" +
                "nomUtilisateur=" + nomUtilisateur.get() +
                ", lire=" + lire.get() +
                ", ecrire=" + ecrire.get() +
                ", charger=" + charger.get() +
                ", supprimer=" + supprimer.get() +
                '}';
    }
}*/
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class UtilisateurPermission {

    private final StringProperty nomUtilisateur;
    private final BooleanProperty lire;
    private final BooleanProperty ecrire;
    private final BooleanProperty charger;
    private final BooleanProperty supprimer;

    public UtilisateurPermission(String nomUtilisateur, boolean lire, boolean ecrire, boolean charger, boolean supprimer) {
        this.nomUtilisateur = new SimpleStringProperty(nomUtilisateur);
        this.lire = new SimpleBooleanProperty(lire);
        this.ecrire = new SimpleBooleanProperty(ecrire);
        this.charger = new SimpleBooleanProperty(charger);
        this.supprimer = new SimpleBooleanProperty(supprimer);
    }

    public String getNomUtilisateur() {
        return nomUtilisateur.get();
    }

    public StringProperty nomUtilisateurProperty() {
        return nomUtilisateur;
    }

    public boolean isLire() {
        return lire.get();
    }

    public BooleanProperty lireProperty() {
        return lire;
    }

    public boolean isEcrire() {
        return ecrire.get();
    }

    public BooleanProperty ecrireProperty() {
        return ecrire;
    }

    public boolean isCharger() {
        return charger.get();
    }

    public BooleanProperty chargerProperty() {
        return charger;
    }

    public boolean isSupprimer() {
        return supprimer.get();
    }

    public BooleanProperty supprimerProperty() {
        return supprimer;
    }
}  

