package homedash;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Utilisateur {
    private final IntegerProperty id;
    private final StringProperty login;

    public Utilisateur(int id, String login) {
        this.id = new SimpleIntegerProperty(id);
        this.login = new SimpleStringProperty(login);
    }

    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public String getLogin() {
        return login.get();
    }

    public void setLogin(String login) {
        this.login.set(login);
    }

    public StringProperty loginProperty() {
        return login;
    }
}

