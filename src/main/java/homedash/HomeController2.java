package homedash;



import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import homedash.DatabaseConnection;
import homedash.Fichier;
import homedash.RechercheController;

import java.security.MessageDigest;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HomeController2 {

    @FXML
    private AnchorPane anchor1;

    @FXML
    private BorderPane bord1;

    @FXML
    private VBox contentBox;

    @FXML
    private TableColumn<?, ?> fileModifiedColumn;

    @FXML
    private TableColumn<?, ?> fileNameColumn;

    @FXML
    private TableColumn<?, ?> fileSizeColumn;

    @FXML
    private TableView<?> fileTable;

    @FXML
    private TableColumn<?, ?> fileTypeColumn;

    @FXML
    private TilePane folderTile;

    @FXML
    private HBox h1;

    @FXML
    private HBox handleUploadFile;

    @FXML
    private VBox headerBox;

    @FXML
    private Button homeBtn;

    @FXML
    private AnchorPane leftMenu;

    @FXML
    private ImageView logo;

    @FXML
    private Button mySharedBtn;

    @FXML
    private VBox navItems;

    @FXML
    private Circle profileCircle;

    @FXML
    private Label profileInitials;

    @FXML
    private Button recentBtn;

    @FXML
    private TableColumn<?, ?> recentModifiedColumn;

    @FXML
    private TableColumn<?, ?> recentNameColumn;

    @FXML
    private TableView<?> recentTable;

    @FXML
    private TableColumn<?, ?> recentTypeColumn;

    @FXML
    private TableColumn<?, ?> recentsizeColumn;

    @FXML
    private VBox scrollContent;

    @FXML
    private TextField search;

    @FXML
    private HBox searchBox;

    @FXML
    private Button sharedWithMeBtn;

    @FXML
    private Button starredBtn;

    @FXML
    private VBox storageBox;

    @FXML
    private Label storageLabel;

    @FXML
    private ProgressBar storageProgress;

    @FXML
    private Label title;

    @FXML
    private VBox topMenu;

    @FXML
    private Button trashBtn;

    @FXML
    private Button uploadBtn;

    @FXML
    void handleHomeAction(ActionEvent event) {

    }

    @FXML
    void handleMySharedAction(ActionEvent event) {

    }

    @FXML
    void handleRecentAction(ActionEvent event) {

    }

    @FXML
    void handleSharedWithMeAction(ActionEvent event) {

    }

    @FXML
    void handleStarredAction(ActionEvent event) {

    }

    @FXML
    void handleTrashAction(ActionEvent event) {

    }
    private String computeFileHash(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        byte[] hashBytes = digest.digest(fileBytes);
        
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @FXML

void handleUploadAction(ActionEvent event) {
    int utilisateurActuelId = Session.getUserId();

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Sélectionner un document à télécharger");
    File selectedFile = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());

    if (selectedFile != null) {
        try (Connection conn = DatabaseConnection.getConnection()) {

            // Récupération des infos fichier
            String fileName = selectedFile.getName();
            String extension = "";
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot != -1 && lastDot < fileName.length() - 1) {
                extension = fileName.substring(lastDot + 1);
            }

            long fileSize = selectedFile.length();
            String fileHash = computeFileHash(selectedFile);  // Calcul du hash

            // Insertion dans la BDD
            String insertQuery = "INSERT INTO PersonalDocuments (owner_id, file_name, file_type, file_size, file_hash) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                stmt.setInt(1, utilisateurActuelId);
                stmt.setString(2, fileName);
                stmt.setString(3, extension);
                stmt.setLong(4, fileSize);
                stmt.setString(5, fileHash);

                stmt.executeUpdate();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Succès");
                alert.setHeaderText(null);
                alert.setContentText("Fichier téléchargé avec succès !");
                alert.showAndWait();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            LoginController.afficherErreur("Erreur lors du téléchargement du fichier.");
        } catch (Exception e) {
            e.printStackTrace();
            LoginController.afficherErreur("Erreur lors du calcul du hash du fichier.");
        }
    }
    }
    @FXML
    private void rechercher(ActionEvent event) {
        String query = search.getText().trim();
        if (query.isEmpty()) {
            return; // Si le champ est vide, ne rien faire
        }

        List<Fichier> fichiers = rechercherFichiers(query);

        if (fichiers.isEmpty()) {
            chargerPage("/views/no result.fxml", null);
        } else {
            chargerPage("/views/Recherche.fxml", fichiers);
        }
    }

    private List<Fichier> rechercherFichiers(String query) {
        List<Fichier> fichiers = new ArrayList<>();
        String sql = "SELECT file_name, file_size,file_type,modify_date FROM PersonalDocuments WHERE LOWER(file_name) LIKE LOWER(?) AND owner_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, "%" + query + "%");
            statement.setInt(2, Session.getUserId());  // Rechercher uniquement les fichiers de l'utilisateur connecté

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String nom = resultSet.getString("file_name");
                String type = resultSet.getString("file_type");
                long taille = resultSet.getLong("file_size");
                Timestamp timestamp = resultSet.getTimestamp("modify_date");
                LocalDateTime dateUpload = timestamp != null ? timestamp.toLocalDateTime() : null;

                fichiers.add(new Fichier(nom,type,taille, dateUpload));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fichiers;
    }

    private void chargerPage(String fxml, List<Fichier> fichiers) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            if (fichiers != null && !fichiers.isEmpty()) {
                RechercheController controller = loader.getController();
                controller.afficherResultats(fichiers);
            }

            Stage stage = (Stage) search.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
    



    


