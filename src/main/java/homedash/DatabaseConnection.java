package homedash;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/interface_connexion"; // Remplace par ton URL
    private static final String USER = "root"; // Remplace par ton utilisateur MySQL
    private static final String PASSWORD = "Asmae2004."; // Remplace par ton mot de passe MySQL

    public static Connection getConnection() throws SQLException {
        try {
            // Etablissement de la connexion à la base de données
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            // Gestion des erreurs de connexion
            System.out.println("Erreur de connexion à la base de données : " + e.getMessage());
            throw e; // Propager l'exception
        }
    }
}
