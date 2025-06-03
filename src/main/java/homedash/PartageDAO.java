package homedash;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PartageDAO {
	public static void partagerFichier(String fichierNom, UtilisateurPermission permission, int ownerId) {
	    int userId = UtilisateurDAO.findByName(permission.getNomUtilisateur()).getId();
	    
	    String sql = "INSERT INTO partages (fichier_id, utilisateur_id, lire, ecrire, charger, supprimer, ownerid) "
	               + "SELECT p.docID, ?, ?, ?, ?, ?, ? "
	               + "FROM PersonalDocuments p "
	               + "WHERE p.file_name = ? "
	               + "ON DUPLICATE KEY UPDATE "
	               + "lire = VALUES(lire), "
	               + "ecrire = VALUES(ecrire), "
	               + "charger = VALUES(charger), "
	               + "supprimer = VALUES(supprimer)";
	    
	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement stmt = conn.prepareStatement(sql)) {

	        stmt.setInt(1, userId); // utilisateur_id (destinataire)
	        stmt.setBoolean(2, permission.isLire());
	        stmt.setBoolean(3, permission.isEcrire());
	        stmt.setBoolean(4, permission.isCharger());
	        stmt.setBoolean(5, permission.isSupprimer());
	        stmt.setInt(6, ownerId); // ownerid (propriétaire qui partage)
	        stmt.setString(7, fichierNom);

	        stmt.executeUpdate();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}

	public static List<SharedFile> getFichiersPartagesAvecMoi(int utilisateurId) {
	    List<SharedFile> fichiers = new ArrayList<>();
	    String sql = "SELECT p.file_name, owner_user.login AS owner, s.date_partage, p.file_size, "
	               + "CONCAT(IF(s.lire=1, 'Lire ', ''), "
	               +       "IF(s.ecrire=1, 'Écrire ', ''), "
	               +       "IF(s.charger=1, 'Charger ', ''), "
	               +       "IF(s.supprimer=1, 'Supprimer', '')) AS permissions "
	               + "FROM partages s "
	               + "JOIN PersonalDocuments p ON s.fichier_id = p.docID "
	               + "JOIN Utilisateurs2 owner_user ON s.ownerid = owner_user.userID " // Correction ici
	               + "WHERE s.utilisateur_id = ?";

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {

	        pstmt.setInt(1, utilisateurId);
	        ResultSet rs = pstmt.executeQuery();

	        while (rs.next()) {
	            String fileName = rs.getString("file_name");
	            String owner = rs.getString("owner");
	            LocalDateTime shareDate = rs.getTimestamp("date_partage").toLocalDateTime();
	            long fileSize = rs.getLong("file_size");
	            String permissions = rs.getString("permissions").trim();

	            fichiers.add(new SharedFile(fileName, owner, shareDate, fileSize, permissions));
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return fichiers;
	}
}



