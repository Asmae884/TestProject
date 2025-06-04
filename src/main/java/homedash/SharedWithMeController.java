package homedash;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SharedWithMeController {

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
    private Label swm;

    @FXML
    private ImageView swmimg;

    @FXML
    private Label title;

    @FXML
    private VBox topMenu;

    @FXML
    private Button trashBtn;

    @FXML
    private Button uploadBtn;

    @FXML
    private TableColumn<Partage, String> fileDateShareColumn;

    @FXML
    private TableColumn<Partage, String> fileNameColumn;

    @FXML
    private TableColumn<Partage, String> fileOwnerColumn;

    @FXML
    private TableColumn<Partage, String> filePermissionColumn;

    @FXML
    private TableColumn<Partage, String> fileSizeColumn;

    @FXML
    private TableView<Partage> fileTable;


    private ObservableList<Partage> partageList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Associer les colonnes aux propriétés de la classe Partage
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("file_name"));
        fileOwnerColumn.setCellValueFactory(new PropertyValueFactory<>("proprietaire"));
        fileDateShareColumn.setCellValueFactory(new PropertyValueFactory<>("date_partage"));
        fileSizeColumn.setCellValueFactory(new PropertyValueFactory<>("file_size"));
        filePermissionColumn.setCellValueFactory(new PropertyValueFactory<>("permissions"));

        // Charger les fichiers partagés avec moi
        loadSharedWithMeFiles();
    }
    
    private void loadSharedWithMeFiles() {
        //partageList.clear();

        String query = """
            SELECT f.file_name, 
                   owner_user.login AS proprietaire, 
                   f.file_size, 
                   p.date_partage,
                   CONCAT(IF(p.lire=1, 'Lire ', ''),
                          IF(p.ecrire=1, 'Écrire ', ''),
                          IF(p.charger=1, 'Charger ', ''),
                          IF(p.supprimer=1, 'Supprimer', '')) AS permissions
            FROM partages p
            INNER JOIN personaldocuments f ON p.fichier_id = f.docID
            INNER JOIN utilisateurs2 owner_user ON p.ownerid = owner_user.userID
            WHERE p.utilisateur_id = ?;
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            int userId = Session.getUserId();
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Partage partage = new Partage(
                    rs.getString("file_name"),
                    rs.getString("proprietaire"),
                    rs.getString("date_partage"),
                    rs.getString("file_size"),
                    rs.getString("permissions")
                );
                partageList.add(partage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        fileTable.setItems(partageList);
        System.out.println("Nombre d'éléments dans partageList : " + partageList.size());
        for (Partage p : partageList) {
            System.out.println(p.getFile_name());
        }

    }


  

    @FXML
    void handleHomeAction(ActionEvent event) {
        // Gestion de l'action pour le bouton "Home"
    }

    @FXML
    void handleMySharedAction(ActionEvent event) {
        // Gestion de l'action pour le bouton "My Shared"
    }

    @FXML
    void handleSharedWithMeAction(ActionEvent event) {
        // Gestion de l'action pour le bouton "Shared With Me"
    }

    @FXML
    void handleStarredAction(ActionEvent event) {
        // Gestion de l'action pour le bouton "Starred"
    }

    @FXML
    void handleTrashAction(ActionEvent event) {
        // Gestion de l'action pour le bouton "Trash"
    }

    @FXML
    void handleUploadAction(ActionEvent event) {
        // Gestion de l'action pour le bouton "Upload"
    }

    @FXML
    void rechercher(ActionEvent event) {
        // Gestion de l'action pour la recherche
    }
}
