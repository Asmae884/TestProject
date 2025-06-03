package homedash;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/interface_connexion";
    private static final String USER = "root";
    private static final String PASSWORD = "Asmae2004.";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    //public static void insertFile(String file_name, String file_hash, String upload_date,int owner_id ) {
    public static void insertFile(String nom, String chemin, long taille) {
       String sql = "INSERT INTO fichiers (nom, chemin, taille) VALUES (?, ?, ?)";
        //String sql = "INSERT INTO PersonalDocuments (file_name,file_hash,upload_date,owner_id)) VALUES (?, ?, ?,?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            //stmt.setString(1,file_name);
            //stmt.setString(2, file_hash);
            //stmt.setString(3, upload_date);
            //stmt.setInt(4, owner_id);
            stmt.setString(1,nom);
            stmt.setString(2, chemin);
            stmt.setLong(3, taille);
     
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
