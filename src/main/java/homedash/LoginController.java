package homedash;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

//import org.mindrot.jbcrypt.BCrypt;

public class LoginController {

    @FXML
    private Hyperlink forgotPasswordLink;

    @FXML
    private Rectangle gradient1;

    @FXML
    private Rectangle gradient2;

    @FXML
    private Rectangle gradient3;

    @FXML
    private StackPane leftPane;

    @FXML
    private Button loginButton;

    @FXML
    private PasswordField passwordField;

    @FXML
    private VBox rightPane;

    @FXML
    private TextField usernameField;

    @FXML
    void forgotmdp(ActionEvent event) {

    }

    @FXML
    void seconnecter(ActionEvent event) throws IOException {
        String login = usernameField.getText();  // Champ de texte pour le login
        String mdp = passwordField.getText();    // Mot de passe saisi

        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT * FROM Utilisateurs2 WHERE login = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, login);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String storedPassword = rs.getString("password");  // plus "hashed_password"

                        if (mdp.equals(storedPassword)) {
                            int userId = rs.getInt("userID");
                            boolean isAdmin = rs.getBoolean("is_admin");

                            Session.setUser(userId, login, isAdmin);

                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Home.fxml"));
                            Parent root = loader.load();
                            Scene scene = new Scene(root);
                            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                            stage.setScene(scene);
                            stage.show();
                        } else {
                            afficherErreur("Nom d'utilisateur ou mot de passe incorrect");
                        }
                    } else {
                        afficherErreur("Nom d'utilisateur ou mot de passe incorrect");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            afficherErreur("Erreur de connexion à la base de données");
        }
    }


    	public static void afficherErreur(String message) {
    	    // Créer une fenêtre d'erreur pour afficher le message
    	    Alert alert = new Alert(Alert.AlertType.ERROR);
    	    alert.setTitle("Erreur de connexion");
    	    alert.setHeaderText(null);
    	    alert.setContentText(message);
    	    alert.showAndWait();
    	}}






