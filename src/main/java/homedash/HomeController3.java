package homedash;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HomeController3 {
    // Injections FXML (inchangées)
    @FXML private AnchorPane anchor1;
    @FXML private BorderPane bord1;
    @FXML private VBox contentBox;
    @FXML private TableColumn<Fichier, LocalDateTime> fileModifiedColumn;
    @FXML private TableColumn<Fichier, String> fileNameColumn;
    @FXML private TableColumn<Fichier, Long> fileSizeColumn;
    @FXML private TableView<Fichier> fileTable;
    @FXML private TableColumn<Fichier, String> fileTypeColumn;
    @FXML private TableColumn<Fichier, Void> fileoptionColumn;
    // ... autres déclarations FXML ...


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
    private final ObservableList<Fichier> fileList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableView();
        loadFilesFromDatabase(); // Chargement initial des fichiers
    }

    private void setupTableView() {
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        fileTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        fileSizeColumn.setCellValueFactory(new PropertyValueFactory<>("taille"));
        fileModifiedColumn.setCellValueFactory(new PropertyValueFactory<>("dateUpload"));

        fileoptionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button actionButton = new Button("⋮");
            private final ContextMenu contextMenu = new ContextMenu();

            {
                actionButton.setStyle("-fx-font-size: 16px; -fx-background-color: #CBFFB4; -fx-cursor: hand;");
                
                MenuItem openItem = new MenuItem("Ouvrir");
                MenuItem encryptItem= new MenuItem("Crypter");
                MenuItem decryptItem = new MenuItem("Décrypter");
                MenuItem updateItem = new MenuItem("Modifier");
                MenuItem downloadItem = new MenuItem("Telecharger");
                MenuItem shareItem = new MenuItem("Partager");
                MenuItem deleteItem = new MenuItem("Supprimer");

                contextMenu.getItems().addAll(openItem, encryptItem,decryptItem,updateItem,downloadItem,shareItem,deleteItem);

                openItem.setOnAction(event -> handleOpen(getTableView().getItems().get(getIndex())));
                encryptItem.setOnAction(event -> handleEncrypt(getTableView().getItems().get(getIndex())));
                decryptItem.setOnAction(event -> handleDecrypt(getTableView().getItems().get(getIndex())));          
                updateItem.setOnAction(event -> handleOpen(getTableView().getItems().get(getIndex())));
                downloadItem.setOnAction(event -> handleOpen(getTableView().getItems().get(getIndex())));
                shareItem.setOnAction(event -> handleShare(getTableView().getItems().get(getIndex())));
                deleteItem.setOnAction(event -> handleDelete(getTableView().getItems().get(getIndex())));

                actionButton.setOnMouseClicked(event -> {
                    if (!contextMenu.isShowing()) {
                        contextMenu.show(actionButton, event.getScreenX(), event.getScreenY());
                    } else {
                        contextMenu.hide();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionButton);
            }

		    private void handleShare(Fichier fichier) {
		        try {
		            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ShareDialog.fxml"));
		            Parent root = loader.load();

		            ShareDialogController controller = loader.getController();
		            controller.initData(fichier);

		            Stage stage = new Stage();
		            stage.setScene(new Scene(root));
		            stage.setTitle("Partager un fichier");
		            stage.showAndWait();
		        } catch (IOException e) {
		            e.printStackTrace();
		            showAlert(AlertType.ERROR, "Erreur", "Impossible d'ouvrir la fenêtre de partage");
		        }
		    }
        });

        fileTable.setItems(fileList);
    }

    private void loadFilesFromDatabase() {
        fileList.clear();
        String query = "SELECT file_name, file_type, file_size, modify_date FROM PersonalDocuments WHERE owner_id = ?  ORDER BY modify_date DESC";
        
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            
            statement.setInt(1, Session.getUserId());
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
 
                String nom = resultSet.getString("file_name");
                String type = resultSet.getString("file_type");
                long taille = resultSet.getLong("file_size");
                
                // Gestion des dates null
                Timestamp timestamp = resultSet.getTimestamp("modify_date");
                LocalDateTime dateUpload = (timestamp != null) ? timestamp.toLocalDateTime() : null;
                
                fileList.add(new Fichier(nom, type, taille, dateUpload));
            }
        } catch (Exception e) {
            showError("Erreur de chargement des fichiers");
            e.printStackTrace();
        }
    }
    @FXML
    public void handleUploadAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier");
        File selectedFile = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());

        if (selectedFile != null) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String fileName = selectedFile.getName();
                String extension = getFileExtension(fileName);
                long fileSize = selectedFile.length();
                String fileHash = computeFileHash(selectedFile);

                String insertQuery = "INSERT INTO PersonalDocuments (owner_id, file_name, file_type, file_size, file_hash) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                    stmt.setInt(1, Session.getUserId());
                    stmt.setString(2, fileName);
                    stmt.setString(3, extension);
                    stmt.setLong(4, fileSize);
                    stmt.setString(5, fileHash);

                    stmt.executeUpdate();
                    loadFilesFromDatabase(); // Rafraîchir la table
                    
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Fichier téléchargé avec succès !");
                }
            } catch (SQLException e) {
                showError("Erreur de base de données : " + e.getMessage());
            } catch (Exception e) {
                showError("Erreur lors du traitement du fichier");
                e.printStackTrace();
            }
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? "" : fileName.substring(lastDot + 1);
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

    // Gestion des actions
    private void handleOpen(Fichier fichier) {
        showAlert(Alert.AlertType.INFORMATION, "Ouverture", "Ouverture de : " + fichier.getNom());
    }
    
 


   /* private void handleDelete(Fichier fichier) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String deleteQuery = "DELETE FROM PersonalDocuments WHERE file_name = ? AND owner_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
                stmt.setString(1, fichier.getNom());
                stmt.setInt(2, Session.getUserId());
                stmt.executeUpdate();
                
                fileList.remove(fichier);
                showAlert(Alert.AlertType.INFORMATION, "Suppression", "Fichier supprimé : " + fichier.getNom());
            }
        } catch (Exception e) {
            showError("Erreur lors de la suppression");
            e.printStackTrace();
        }
    }*/

   private void handleEncrypt(Fichier fichier) {
        // Implémentation du cryptage ici
        showAlert(Alert.AlertType.INFORMATION, "Cryptage", "Cryptage de : " + fichier.getNom());
    }

    private void handleDecrypt(Fichier fichier) {
        // Implémentation du décryptage ici
        showAlert(Alert.AlertType.INFORMATION, "Décryptage", "Décryptage de : " + fichier.getNom());
    }

    // Méthodes utilitaires
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String message) {
        showAlert(Alert.AlertType.ERROR, "Erreur", message);
    }

    private void handleDelete(Fichier fichier) {
        if (fichier == null) {
            showAlert(Alert.AlertType.WARNING, "Suppression", "Aucun fichier sélectionné.");
            return;
        }

        // Debug : Afficher le nom du fichier
        System.out.println("[DEBUG] Nom du fichier utilisé : " + fichier.getNom());

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Étape 1 : Insertion dans Archive via file_name
            String archiveQuery = """
                INSERT INTO Archive (file_name, file_hash, upload_date, modify_date, owner_id, file_type, file_size, file_path, delete_date)
                SELECT file_name, file_hash, upload_date, modify_date, owner_id, file_type, file_size, file_path, CURRENT_TIMESTAMP
                FROM PersonalDocuments
                WHERE file_name = ?;
            """;

            try (PreparedStatement archiveStmt = conn.prepareStatement(archiveQuery)) {
                archiveStmt.setString(1, fichier.getNom());
                int rowsInserted = archiveStmt.executeUpdate();

                if (rowsInserted == 0) {
                    System.out.println("[ERREUR] Aucun fichier trouvé avec le nom : " + fichier.getNom());
                    showAlert(Alert.AlertType.WARNING, "Erreur", "Document non trouvé.");
                    conn.rollback();
                    return;
                }
            }

            // Étape 2 : Suppression de PersonalDocuments via file_name
            String deleteQuery = "DELETE FROM PersonalDocuments WHERE file_name = ?";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery)) {
                deleteStmt.setString(1, fichier.getNom());
                int deletedRows = deleteStmt.executeUpdate();
                System.out.println("[DEBUG] Lignes supprimées : " + deletedRows);
            }

            conn.commit();
            fileList.remove(fichier);
            //redirectBasedOnTrashState();

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Fichier déplacé vers la corbeille.");

        } catch (SQLException e) {
            showError("Erreur SQL : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("Erreur inattendue : " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * Vérifie si la table Archive est vide et redirige vers Trash.fxml ou EmptyTrash.fxml.
     */
   /* private void redirectBasedOnTrashState() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            String countQuery = "SELECT COUNT(*) FROM Archive";
            ResultSet rs = stmt.executeQuery(countQuery);
            
            if (rs.next() && rs.getInt(1) > 0) {
                // Rediriger vers Trash.fxml
                Parent root = FXMLLoader.load(getClass().getResource("/views/Trash.fxml"));
                Stage stage = (Stage) anchor1.getScene().getWindow();
                stage.setScene(new Scene(root));
            } else {
                // Rediriger vers EmptyTrash.fxml
                Parent root = FXMLLoader.load(getClass().getResource("/views/EmptyTrash.fxml"));
                Stage stage = (Stage) anchor1.getScene().getWindow();
                stage.setScene(new Scene(root));
            }

        } catch (SQLException | IOException e) {
            showError("Erreur lors de la redirection : " + e.getMessage());
        }
    }*/



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

    // Méthodes de navigation (à implémenter selon vos besoins)
    @FXML void handleHomeAction(ActionEvent event) { /* ... */ }
    @FXML void handleMySharedAction(ActionEvent event) { /* ... */ }
   // private Stage stage;


    @FXML
    void handleTrashAction(ActionEvent event) {
        boolean isTableEmpty = true;
        String sql = "SELECT COUNT(*) AS total FROM Archive";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                isTableEmpty = rs.getInt("total") == 0; // Vérifie si la table est vide
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Afficher une alerte en cas d'erreur
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de vérifier l'état de la corbeille.");
            return;
        }

        // Charger la page FXML appropriée
        String fxmlFile = isTableEmpty ? "/views/emptyTrash.fxml" : "/views/Trash.fxml";

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            // Obtenir la scène actuelle à partir de l'événement
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la vue.");
        }
    }

    
    @FXML
    public void handleSharedWithMe(ActionEvent event) {
        try {
            // Charger le fichier FXML pour la nouvelle page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SharedWithMe.fxml"));
            Parent sharedWithMePage = loader.load();

            // Obtenir la scène actuelle à partir de l'événement
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Configurer la nouvelle scène
            Scene scene = new Scene(sharedWithMePage);
            stage.setScene(scene);
            stage.setTitle("Fichiers partagés avec moi"); // Définir un titre pour la nouvelle fenêtre
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            // Ajouter un message d'erreur pour informer l'utilisateur
            showError("Impossible de charger la page des fichiers partagés.");
        }
    }




    
    @FXML void handleStarredAction(ActionEvent event) { /* ... */ }
    
    // ... autres méthodes de navigation ...
    
}


