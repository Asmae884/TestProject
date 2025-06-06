package com.emi.projetintegre.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.emi.projetintegre.client.ClientSocketManager;
import com.emi.projetintegre.models.PersonalDocument;

public class TrashController {

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
    @FXML private TableView<PersonalDocument> trashTable;
    @FXML private TableColumn<PersonalDocument, String> fileNameColumn;
    @FXML private TableColumn<PersonalDocument, String> fileTypeColumn;
    @FXML private TableColumn<PersonalDocument, String> fileModifiedColumn;
    @FXML private TableColumn<PersonalDocument, String> fileSizeColumn;
    @FXML private TableColumn<PersonalDocument, Void> fileMenuColumn;
    @FXML private StackPane noResultsPane;

    private ClientSocketManager clientSocketManager;

    @FXML
    public void initialize() {
        System.out.println("TrashController initialize: clientSocketManager=" + 
                           (clientSocketManager != null ? "Initialized, Connected=" + clientSocketManager.isConnected() : "Null"));
        setupTableColumns();
        setupButtonHoverEffects();
        setupSearchField();
        updateStorageInfo();

        // Reset view when search field is cleared
        search.textProperty().addListener((_, _, newVal) -> {
            if (newVal.trim().isEmpty()) {
                populateTables(null); // Reset to default view
            }
        });
    }

    public void refreshTablesAfterLogin() {
        populateTables(null);
    }

    public void handleSelectAllAction(ActionEvent event) {
        trashTable.getSelectionModel().selectAll();
    }

    public void handleDeleteAction(ActionEvent event) {
        ObservableList<PersonalDocument> selectedDocuments = trashTable.getSelectionModel().getSelectedItems();
        if (selectedDocuments.isEmpty()) {
            showAlert("Warning", "No documents selected for permanent deletion.");
            return;
        }

        List<Integer> docIDs = selectedDocuments.stream()
                .map(PersonalDocument::getDocID)
                .collect(Collectors.toList());

        boolean success = clientSocketManager.permanentDeleteDocuments(docIDs);
        if (success) {
            showAlert("Success", "Selected documents have been permanently deleted.");
            populateTables(search.getText().trim()); // Refresh tables
        } else {
            showAlert("Error", "Failed to permanently delete selected documents. Please try again.");
        }
    }

    public void handleRestoreAction(ActionEvent event) {
        ObservableList<PersonalDocument> selectedDocuments = trashTable.getSelectionModel().getSelectedItems();
        if (selectedDocuments.isEmpty()) {
            showAlert("Warning", "No documents selected for restoration.");
            return;
        }

        List<Integer> docIDs = selectedDocuments.stream()
                .map(PersonalDocument::getDocID)
                .collect(Collectors.toList());

        boolean success = clientSocketManager.restoreDocuments(docIDs);
        if (success) {
            showAlert("Success", "Selected documents have been restored successfully.");
            populateTables(search.getText().trim()); // Refresh tables
        } else {
            showAlert("Error", "Failed to restore selected documents. Please try again.");
        }
    }

    private void setupTableColumns() {
        // File Table Columns
        fileNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFileName()));
        fileTypeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNumberType()));
        fileModifiedColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getUploadDate().toLocalDate().toString()));
        fileSizeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
            formatSize(cellData.getValue().getSize())));

        // Setup cell factory for name column
        fileNameColumn.setCellFactory(_ -> {
            TableCell<PersonalDocument, String> cell = new TableCell<PersonalDocument, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                    }
                }
            };

            cell.setOnMouseClicked(event -> {
                if (!cell.isEmpty()) {
                    PersonalDocument doc = (PersonalDocument) cell.getTableRow().getItem();
                    if (doc != null) {
                        if (event.getClickCount() == 1) {
                            System.out.println("Single clicked on: " + doc.getFileName());
                            cell.getTableView().getSelectionModel().select(cell.getIndex());
                        } else if (event.getClickCount() == 2) {
                            System.out.println("Double clicked on: " + doc.getFileName());
                            openFileViewer(doc);
                        }
                    }
                }
            });

            return cell;
        });

        // Setup Menu Column
        setupMenuColumn(fileMenuColumn);

        // Ensure selection model allows multiple selection for delete/restore
        trashTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        trashTable.setMouseTransparent(false);
    }

    private void setupMenuColumn(TableColumn<PersonalDocument, Void> menuColumn) {
        if (menuColumn == null) {
            System.err.println("Error: menuColumn is null in setupMenuColumn");
            return;
        }
        menuColumn.setPrefWidth(50);
        menuColumn.setStyle("-fx-alignment: CENTER;");
        menuColumn.setCellFactory(_ -> new TableCell<PersonalDocument, Void>() {
            private final Button menuButton = new Button("⋮");

            {
                menuButton.setStyle("-fx-font-size: 14; -fx-background-color: transparent; -fx-text-fill: black;");
                menuButton.setOnAction(event -> {
                    PersonalDocument doc = getTableView().getItems().get(getIndex());
                    showFileActionMenu(doc, menuButton);
                    event.consume();
                });
                menuButton.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        event.consume();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(menuButton);
                    menuButton.setVisible(true);
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
        trashTable.getSelectionModel().clearSelection();
        trashTable.getSortOrder().clear();

        ObservableList<PersonalDocument> documents = FXCollections.observableArrayList();
        if (clientSocketManager == null || !clientSocketManager.isConnected()) {
            showAlert("Connection Error", "Not connected to the server. Please check your connection and try again.");
            return;
        }

        documents = clientSocketManager.getListDeletedDocuments(query);
        boolean isSearchMode = query != null && !query.trim().isEmpty();
        boolean hasResults = !documents.isEmpty();

        if (isSearchMode && !hasResults) {
            // No results: Show noResultsPane, hide trashTable
            trashTable.setItems(FXCollections.observableArrayList());
            trashTable.setVisible(false);
            trashTable.setManaged(false);
            noResultsPane.setVisible(true);
            System.out.println("No search results for query: " + query + ", showing noResultsPane");
        } else {
            // Show trashTable with results
            trashTable.setItems(FXCollections.observableArrayList(documents));
            trashTable.setVisible(true);
            trashTable.setManaged(true);
            noResultsPane.setVisible(false);
            System.out.println("Populated trashTable with " + documents.size() + " deleted documents for query: " + (query == null ? "null" : query));
        }

        trashTable.scrollTo(0);
        trashTable.getSelectionModel().clearSelection();
    }

    private void setupButtonHoverEffects() {
        for (Button btn : new Button[]{uploadBtn, sharedWithMeBtn, mySharedBtn, recentBtn, starredBtn, trashBtn, homeBtn}) {
            btn.setOnMouseEntered(_ -> btn.setCursor(Cursor.HAND));
            btn.setOnMouseExited(_ -> btn.setCursor(Cursor.DEFAULT));
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
        showAlert("Info", "Upload is disabled in Trash view.");
    }

    @FXML
    private void handleHomeAction() throws IOException {
        try {
            System.out.println("Loading Home.fxml");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emi/projetintegre/views/Home.fxml"));
            Parent root = loader.load();
            
            HomeController homeController = loader.getController();
            System.out.println("Setting ClientSocketManager");
            homeController.setClientSocketManager(clientSocketManager);
            homeController.refreshTablesAfterLogin();
            
            System.out.println("Showing Home scene");
            Stage stage = (Stage) homeBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Home");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Failed to load Home page");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSharedWithMeAction() throws IOException {
        System.out.println("Shared With Me action triggered");
        try {
            System.out.println("Loading ShareWithMe.fxml");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emi/projetintegre/views/SharedWithMe.fxml"));
            Parent root = loader.load();
            
            // Pass the clientSocketManager to the Home controller
            SharedWithMeController sharedWithMeController = loader.getController();
            System.out.println("Setting ClientSocketManager");
            sharedWithMeController.setClientSocketManager(clientSocketManager);
            System.out.println("Refreshing tables after login");
            sharedWithMeController.refreshTablesAfterLogin();
            
            System.out.println("Showing Share With Me scene");
            Stage stage = (Stage) sharedWithMeBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Share With Me");
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible de charger la page des fichiers partagés avec moi");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMySharedAction() {
        showAlert("Info", "My Shared Files view not yet implemented");
    }

    @FXML
    private void handleRecentAction() {
        showAlert("Info", "Recent Files view not yet implemented");
    }

    @FXML
    private void handleStarredAction() {
        showAlert("Info", "Starred Files view not yet implemented");
    }

    @FXML
    private void handleTrashAction() {
        populateTables(null);
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

    private void openFileViewer(PersonalDocument document) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/emi/projetintegre/views/FileViewer.fxml"));
            Scene scene = new Scene(loader.load());
            FileViewerController controller = loader.getController();
            controller.setClientSocketManager(clientSocketManager);
            controller.setDocument(document);

            Stage stage = new Stage();
            stage.setTitle("View File: " + document.getFileName());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            showAlert("Error", "Failed to open file viewer: " + e.getMessage());
            e.printStackTrace();
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