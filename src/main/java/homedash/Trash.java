
package homedash;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Trash {

    @FXML
    private AnchorPane anchor1;

    @FXML
    private BorderPane bord1;

    @FXML
    private VBox contentBox;

    @FXML
    private Label corb;

    @FXML
    private ImageView corbimg;
   
        @FXML
        private TableView<ArchiveFile> fileTable; // fx:id="trashTable" dans le FXML

        @FXML
        private TableColumn<ArchiveFile, String> fileNameColumn;
        @FXML
        private TableColumn<ArchiveFile, String> filePlaceColumn;
        @FXML
        private TableColumn<ArchiveFile, LocalDateTime> fileDateColumn;
        @FXML
        private TableColumn<ArchiveFile, Long> fileSizeColumn;

   
    @FXML
    private TableColumn<?, ?> fileoptionColumn;

    @FXML
    private TilePane folderTile;

    @FXML
    private HBox h1;

    @FXML
    private VBox headerBox;

    @FXML
    private Button homeBtn;

    @FXML
    private Text lb;

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
    private Label title;

    @FXML
    private VBox topMenu;

    @FXML
    private Button trashBtn;

    @FXML
    private Button uploadBtn;
 // Dans TrashController.java
    @FXML
    private Button vidercorb;

    @FXML
    void deleteall(ActionEvent event) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "DELETE FROM Archive ";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.executeUpdate();
            }
            loadArchiveData(); // Rafraîchir les données
        } catch (SQLException e) {
            e.printStackTrace();
        }
    	

    }

 


            public void initialize() {
                // Lier les colonnes
                fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
                filePlaceColumn.setCellValueFactory(new PropertyValueFactory<>("filePath"));
                fileDateColumn.setCellValueFactory(new PropertyValueFactory<>("deleteDate"));
                fileSizeColumn.setCellValueFactory(new PropertyValueFactory<>("fileSize"));

                loadArchiveData();
                setupContextMenu();
            }

            private void loadArchiveData() {
                ObservableList<ArchiveFile> archiveFiles = FXCollections.observableArrayList();
                String query = "SELECT * FROM Archive";

                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        ArchiveFile file = new ArchiveFile(
                            rs.getInt("archiveID"),
                            rs.getString("file_name"),
                            rs.getString("file_hash"),
                            rs.getTimestamp("upload_date").toLocalDateTime(),
                            rs.getTimestamp("modify_date").toLocalDateTime(),
                            rs.getInt("owner_id"),
                            rs.getString("file_type"),
                            rs.getLong("file_size"),
                            rs.getString("file_path"),
                            rs.getTimestamp("delete_date").toLocalDateTime()
                        );
                        archiveFiles.add(file);
                    }

                    fileTable.setItems(archiveFiles); // Utilisez la variable correcte (trashTable ou fileTable)

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        

        private void setupContextMenu() {
            // Création du menu contextuel avec les options
            fileTable.setRowFactory(tv -> {
                TableRow<ArchiveFile> row = new TableRow<>();
                ContextMenu contextMenu = new ContextMenu();

                MenuItem restoreItem = new MenuItem("Restaurer");
                restoreItem.setOnAction(event -> handleRestore(row.getItem()));

                MenuItem deleteItem = new MenuItem("Effacer");
                deleteItem.setOnAction(event -> handleDeletePermanently(row.getItem()));

                contextMenu.getItems().addAll(restoreItem, deleteItem);

                row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                    .then((ContextMenu) null)
                    .otherwise(contextMenu)
                );

                return row;
            });
        }


        private void handleRestore(ArchiveFile file) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Étape 1 : Réinsérer dans PersonalDocuments
                String restoreQuery = """
                    INSERT INTO PersonalDocuments (file_name, file_hash, upload_date, owner_id, file_type, file_size, file_path)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
                try (PreparedStatement stmt = conn.prepareStatement(restoreQuery)) {
                    stmt.setString(1, file.getFileName());
                    stmt.setString(2, file.getFileHash());
                    stmt.setTimestamp(3, Timestamp.valueOf(file.getUploadDate()));
                    stmt.setInt(4, file.getOwnerId());
                    stmt.setString(5, file.getFileType());
                    stmt.setLong(6, file.getFileSize());
                    stmt.setString(7, file.getFilePath());
                    stmt.executeUpdate();
                }

                // Étape 2 : Supprimer de Archive
                String deleteQuery = "DELETE FROM Archive WHERE archiveID = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
                    stmt.setInt(1, file.getArchiveID());
                    stmt.executeUpdate();
                }

                // Rafraîchir la TableView
                loadArchiveData();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void handleDeletePermanently(ArchiveFile file) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String query = "DELETE FROM Archive WHERE archiveID = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, file.getArchiveID());
                    stmt.executeUpdate();
                }
                loadArchiveData(); // Rafraîchir les données
            } catch (SQLException e) {
                e.printStackTrace();
            }
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
    void handleSharedWithMeAction(ActionEvent event) {

    }

    @FXML
    void handleStarredAction(ActionEvent event) {

    }

    @FXML
    void handleTrashAction(ActionEvent event) {

    }

    @FXML
    void handleUploadAction(ActionEvent event) {

    }

    @FXML
    void rechercher(ActionEvent event) {

    }

}
