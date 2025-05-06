package com.emi.projetintegre.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import java.io.IOException;
import com.emi.projetintegre.client.ClientSocketManager;
import com.emi.projetintegre.models.PersonalDocument;

public class HomeController {

    // Header
    @FXML private TextField search;
    @FXML private Circle profileCircle;
    @FXML private Label profileInitials;

    // Left Menu
    @FXML private Button uploadBtn;
    @FXML private Button homeBtn;
    @FXML private Button sharedWithMeBtn;
    @FXML private Button mySharedBtn;
    @FXML private Button recentBtn;
    @FXML private Button starredBtn;
    @FXML private Button trashBtn;
    @FXML private Label storageLabel;
    @FXML private ProgressBar storageProgress;

    // Content
    @FXML private TitledPane recentPane;
    @FXML private TableView<PersonalDocument> recentTable;
    @FXML private TableColumn<PersonalDocument, String> recentNameColumn;
    @FXML private TableColumn<PersonalDocument, String> recentTypeColumn;
    @FXML private TableColumn<PersonalDocument, String> recentModifiedColumn;
    @FXML private TableColumn<PersonalDocument, String> recentsizeColumn;
    @FXML private TableColumn<PersonalDocument, Void> recentMenuColumn;
    @FXML private TableView<PersonalDocument> fileTable;
    @FXML private TableColumn<PersonalDocument, String> fileNameColumn;
    @FXML private TableColumn<PersonalDocument, String> fileTypeColumn;
    @FXML private TableColumn<PersonalDocument, String> fileModifiedColumn;
    @FXML private TableColumn<PersonalDocument, String> fileSizeColumn;
    @FXML private TableColumn<PersonalDocument, Void> fileMenuColumn;
    @FXML private TilePane folderTile;
    @FXML private StackPane noResultsPane;
    @FXML private Label documentsLabel;
    @FXML private Label foldersLabel;

    private ClientSocketManager clientSocketManager;
    private boolean userInteractedWithRecentPane = false;
    private boolean lastRecentPaneExpanded = false;

    @FXML
    public void initialize() {
        System.out.println("HomeController initialize: clientSocketManager=" + 
                           (clientSocketManager != null ? "Initialized, Connected=" + clientSocketManager.isConnected() : "Null"));
        setupTableColumns();
        setupButtonHoverEffects();
        setupSearchField();
        updateStorageInfo();

        // Track user interaction with recentPane
        recentPane.expandedProperty().addListener((obs, oldVal, newVal) -> {
            userInteractedWithRecentPane = true;
            lastRecentPaneExpanded = newVal;
            System.out.println("recentPane expanded state changed to: " + newVal);
        });

        // Prevent focus transfer between tables
        recentTable.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                fileTable.getSelectionModel().clearSelection();
            }
        });
        fileTable.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                recentTable.getSelectionModel().clearSelection();
            }
        });

        // Reset view when search field is cleared
        search.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                populateTables(null); // Reset to default view
            }
        });
    }

    public void refreshTablesAfterLogin() {
        populateTables(null);
    }

    private void setupTableColumns() {
        // Recent Table Columns
        recentNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFileName()));
        recentTypeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNumberType()));
        recentModifiedColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getUploadDate().toLocalDate().toString()));
        recentsizeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
            formatSize(cellData.getValue().getSize())));

        // File Table Columns
        fileNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFileName()));
        fileTypeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNumberType()));
        fileModifiedColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getUploadDate().toLocalDate().toString()));
        fileSizeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
            formatSize(cellData.getValue().getSize())));

        // Setup Menu Columns
        setupMenuColumn(recentMenuColumn);
        setupMenuColumn(fileMenuColumn);
    }

    private void setupMenuColumn(TableColumn<PersonalDocument, Void> menuColumn) {
        menuColumn.setPrefWidth(50);
        menuColumn.setStyle("-fx-alignment: CENTER;");
        menuColumn.setCellFactory(param -> new TableCell<PersonalDocument, Void>() {
            private final Button menuButton = new Button("⋮");

            {
                menuButton.setStyle("-fx-font-size: 14; -fx-background-color: transparent; -fx-text-fill: black;");
                menuButton.setVisible(false);
                menuButton.setOnAction(event -> {
                    PersonalDocument doc = getTableView().getItems().get(getIndex());
                    showFileActionMenu(doc, menuButton);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(menuButton);
                    getTableRow().hoverProperty().addListener((obs, oldVal, newVal) -> {
                        menuButton.setVisible(newVal && !empty);
                        System.out.println("Hover on " + getTableView().getId() + " row " + getIndex() + ": " + newVal);
                        if (newVal && getTableView().getSelectionModel().getSelectedIndex() != getIndex()) {
                            getTableView().getSelectionModel().clearSelection();
                        }
                    });
                }
            }
        });
    }

    private void showFileActionMenu(PersonalDocument document, Button anchor) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emi/projetintegre/views/FileActionMenu.fxml"));
            VBox menu = loader.load();
            FileActionMenuController controller = loader.getController();
            controller.setDocument(document);
            controller.setClientSocketManager(clientSocketManager);
            controller.setOnActionComplete(() -> populateTables(search.getText().trim()));

            Popup popup = new Popup();
            popup.getContent().add(menu);
            popup.setAutoHide(true);

            double x = anchor.localToScreen(anchor.getBoundsInLocal()).getMinX();
            double y = anchor.localToScreen(anchor.getBoundsInLocal()).getMaxY();
            popup.show(anchor.getScene().getWindow(), x, y);
        } catch (IOException e) {
            showAlert("Error", "Failed to load file action menu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String formatSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private void populateTables(String query) {
        recentTable.getSelectionModel().clearSelection();
        fileTable.getSelectionModel().clearSelection();
        recentTable.getSortOrder().clear();
        fileTable.getSortOrder().clear();

        ObservableList<PersonalDocument> documents = FXCollections.observableArrayList();
        if (clientSocketManager == null || !clientSocketManager.isConnected()) {
            showAlert("Connection Error", "Not connected to the server. Please check your connection and try again.");
            return;
        }

        documents = clientSocketManager.getListDocuments(query);
        boolean isSearchMode = query != null && !query.trim().isEmpty();
        boolean hasResults = !documents.isEmpty();

        if (isSearchMode && !hasResults) {
            // No results: Show noResultsPane, hide everything else
            fileTable.setItems(FXCollections.observableArrayList());
            recentTable.setItems(FXCollections.observableArrayList());
            folderTile.setVisible(false);
            folderTile.setManaged(false);
            recentPane.setVisible(false);
            recentPane.setManaged(false);
            documentsLabel.setVisible(false);
            documentsLabel.setManaged(false);
            foldersLabel.setVisible(false);
            foldersLabel.setManaged(false);
            fileTable.setVisible(false);
            fileTable.setManaged(false);
            noResultsPane.setVisible(true);
            System.out.println("No search results for query: " + query + ", showing noResultsPane");
        } else if (isSearchMode) {
            // Search mode with results: Show fileTable, hide others
            fileTable.setItems(FXCollections.observableArrayList(documents));
            recentTable.setItems(FXCollections.observableArrayList());
            folderTile.setVisible(false);
            folderTile.setManaged(false);
            recentPane.setVisible(false);
            recentPane.setManaged(false);
            documentsLabel.setVisible(false);
            documentsLabel.setManaged(false);
            foldersLabel.setVisible(false);
            foldersLabel.setManaged(false);
            fileTable.setVisible(true);
            fileTable.setManaged(true);
            noResultsPane.setVisible(false);
            System.out.println("Populated fileTable with " + documents.size() + " search results for query: " + query);
        } else {
            // Default mode: Show all content
            ObservableList<PersonalDocument> recentItems = FXCollections.observableArrayList();
            if (hasResults) {
                documents.sort((doc1, doc2) -> doc2.getUploadDate().compareTo(doc1.getUploadDate()));
                int maxRecent = Math.min(2, documents.size());
                for (int i = 0; i < maxRecent; i++) {
                    recentItems.add(documents.get(i));
                }
            }

            recentTable.setItems(recentItems);
            fileTable.setItems(FXCollections.observableArrayList(documents));
            folderTile.setVisible(true);
            folderTile.setManaged(true);
            recentPane.setVisible(true);
            recentPane.setManaged(true);
            recentPane.setExpanded(userInteractedWithRecentPane ? lastRecentPaneExpanded : false);
            documentsLabel.setVisible(true);
            documentsLabel.setManaged(true);
            foldersLabel.setVisible(true);
            foldersLabel.setManaged(true);
            fileTable.setVisible(true);
            fileTable.setManaged(true);
            noResultsPane.setVisible(false);
            System.out.println("Populated recentTable with " + recentItems.size() + " items and fileTable with " + documents.size() + " items");
        }

        recentTable.scrollTo(0);
        fileTable.scrollTo(0);
        recentTable.getSelectionModel().clearSelection();
        fileTable.getSelectionModel().clearSelection();
    }

    private void setupButtonHoverEffects() {
        for (Button btn : new Button[]{uploadBtn, homeBtn, sharedWithMeBtn, mySharedBtn, recentBtn, starredBtn, trashBtn}) {
            btn.setOnMouseEntered(e -> btn.setCursor(Cursor.HAND));
            btn.setOnMouseExited(e -> btn.setCursor(Cursor.DEFAULT));
        }
    }

    private void setupSearchField() {
        search.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleSearchAction();
            }
        });
    }

    private void updateStorageInfo() {
        double used = 5.14;
        double total = 15.0;
        storageProgress.setProgress(used / total);
        storageLabel.setText(String.format("%.2f Go utilisés sur %.0f Go", used, total));
    }

    public void setClientSocketManager(ClientSocketManager clientSocketManager) {
        this.clientSocketManager = clientSocketManager;
        System.out.println("ClientSocketManager set: " + (clientSocketManager != null ? "Initialized" : "Null"));
    }

    @FXML
    private void handleUploadAction() {
        if (clientSocketManager == null || !clientSocketManager.isConnected()) {
            showAlert("Error", "Not connected to server");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emi/projetintegre/views/UploadDialog.fxml"));
            Scene scene = new Scene(loader.load());
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Upload New File");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(scene);

            UploadDialogController controller = loader.getController();
            controller.setClientSocketManager(clientSocketManager);
            String currentQuery = search.getText().trim();
            controller.setOnUploadSuccess(() -> populateTables(currentQuery));

            dialogStage.showAndWait();
        } catch (IOException e) {
            showAlert("Error", "Failed to open upload dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleHomeAction() {
        search.clear();
        populateTables(null);
        System.out.println("Home action triggered");
    }

    @FXML
    private void handleSharedWithMeAction() {
        System.out.println("Shared with me action triggered");
    }

    @FXML
    private void handleMySharedAction() {
        System.out.println("My shared files action triggered");
    }

    @FXML
    private void handleRecentAction() {
        System.out.println("Recent files action triggered");
    }

    @FXML
    private void handleStarredAction() {
        System.out.println("Starred files action triggered");
    }

    @FXML
    private void handleTrashAction() {
        System.out.println("Trash action triggered");
    }

    @FXML
    private void handleSearchAction() {
        String query = search.getText().trim();
        if (clientSocketManager == null || !clientSocketManager.isConnected()) {
            showAlert("Error", "Not connected to server");
            return;
        }
        populateTables(query);
        System.out.println("Search query: " + query);
    }

    @FXML
    private void handleRecentTableClick(MouseEvent event) {
        PersonalDocument selectedItem = recentTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null && event.getClickCount() == 2) {
            System.out.println("Double-clicked recent file: " + selectedItem.getFileName());
            showAlert("Not Implemented", "Downloading files is not yet implemented.");
        }
    }

    @FXML
    private void handleFileTableClick(MouseEvent event) {
        PersonalDocument selectedItem = fileTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null && event.getClickCount() == 2) {
            System.out.println("Double-clicked file: " + selectedItem.getFileName());
            showAlert("Not Implemented", "Downloading files is not yet implemented.");
        }
    }

    @FXML
    private void handleFolderTileClick(MouseEvent event) {
        if (event.getTarget() instanceof Label label) {
            String folderName = label.getText();
            System.out.println("Clicked folder: " + folderName);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}