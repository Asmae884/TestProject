package homedash;

import javafx.collections.FXCollections;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;




public class RechercheController {

    @FXML
    private AnchorPane anchor1;

    @FXML
    private BorderPane bord1;

    @FXML
    private VBox contentBox;



    @FXML
    private TilePane folderTile;

    @FXML
    private HBox h1;

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
    private Label results1;

    @FXML
    private TableView<Fichier> fileTable;

    @FXML
   private TableColumn<Fichier, String> fileNameColumn;

    @FXML
    private TableColumn<Fichier, Integer> fileSizeColumn;
    @FXML
    private TableColumn<?, ?> fileTypeColumn;

    @FXML
    private TableColumn<Fichier, LocalDateTime> fileModifiedColumn;

    @FXML
    private void initialize() {
    	fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
    	fileTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        fileSizeColumn.setCellValueFactory(new PropertyValueFactory<>("taille"));
        fileModifiedColumn.setCellValueFactory(new PropertyValueFactory<>("dateUpload"));
    }

    public void afficherResultats(List<Fichier> fichiers) {
        ObservableList<Fichier> data = FXCollections.observableArrayList(fichiers);
        fileTable.setItems(data);
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

    @FXML
    void handleUploadAction(ActionEvent event) {

    }

    
    @FXML
    void handleHomeAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Home.fxml"));
            Parent homeRoot = loader.load();

            // Récupère la scène actuelle via l'événement
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(homeRoot);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
}

