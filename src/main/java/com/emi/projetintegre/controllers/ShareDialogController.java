package com.emi.projetintegre.controllers;

import com.emi.projetintegre.client.ClientSocketManager;
import com.emi.projetintegre.models.Client;
import com.emi.projetintegre.models.PersonalDocument;
import com.emi.projetintegre.models.User;
import com.emi.projetintegre.models.UserPermission;
import com.emi.projetintegre.models.AccessLevel;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;

import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ShareDialogController implements Initializable {
    private boolean isTableInitializing = false; // Add flag to track initialization

    @FXML
    private Label fileNameLabel;
    @FXML
    private ComboBox<String> searchUserField;
    @FXML
    private Button addUserButton;
    @FXML
    private TableView<UserPermission> permissionsTable;
    @FXML
    private TableColumn<UserPermission, String> userColumn;
    @FXML
    private TableColumn<UserPermission, Boolean> readColumn;
    @FXML
    private TableColumn<UserPermission, Boolean> writeColumn;
    @FXML
    private TableColumn<UserPermission, Boolean> downloadColumn;
    @FXML
    private TableColumn<UserPermission, Boolean> deleteColumn;
    @FXML
    private Button cancelButton;
    @FXML
    private Button validateButton;

    private PersonalDocument document;
    private ClientSocketManager clientSocketManager;
    private ObservableList<UserPermission> userPermissions = FXCollections.observableArrayList();
    private ObservableList<User> availableUsers = FXCollections.observableArrayList();
    private ObservableList<String> filteredUserLogins = FXCollections.observableArrayList();
    private ObservableList<UserPermission> initialPermissions = FXCollections.observableArrayList(); // Track initial state
    private ObservableList<UserPermission> permissionsToDelete = FXCollections.observableArrayList(); //for supprimer
    private boolean isInitialized = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configure table columns
        userColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUser().getLogin()));
        readColumn.setCellValueFactory(cellData -> new SimpleBooleanProperty(cellData.getValue().canRead()));
        writeColumn.setCellValueFactory(cellData -> new SimpleBooleanProperty(cellData.getValue().canWrite()));
        downloadColumn.setCellValueFactory(cellData -> new SimpleBooleanProperty(cellData.getValue().canDownload()));
        deleteColumn.setCellValueFactory(cellData -> new SimpleBooleanProperty(cellData.getValue().canDelete()));

        // Set up checkbox cells
        readColumn.setCellFactory(_ -> new CheckBoxTableCell<UserPermission, Boolean>() {
            @Override
            public void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    setDisable(true); // Make read checkbox non-editable
                }
            }
        });

		// First, modify the cell value factories to use property bindings
		writeColumn.setCellValueFactory(cellData -> {
		    UserPermission permission = cellData.getValue();
		    return new SimpleBooleanProperty(permission.canWrite());
		});
		
		downloadColumn.setCellValueFactory(cellData -> {
		    UserPermission permission = cellData.getValue();
		    return new SimpleBooleanProperty(permission.canDownload());
		});
		
		deleteColumn.setCellValueFactory(cellData -> {
		    UserPermission permission = cellData.getValue();
		    return new SimpleBooleanProperty(permission.canDelete());
		});
		
		// Then modify the cell factories
		writeColumn.setCellFactory(_ -> new CheckBoxTableCell<UserPermission, Boolean>() {
		    private CheckBox checkBox;
		    
		    @Override
		    public void updateItem(Boolean item, boolean empty) {
		        super.updateItem(item, empty);
		        if (empty || getTableRow() == null || getTableRow().getItem() == null) {
		            setGraphic(null);
		            return;
		        }
		        
		        UserPermission perm = getTableRow().getItem();
		        if (checkBox == null) {
		            checkBox = new CheckBox();
		        }
		        
		        checkBox.setSelected(perm.canWrite());
		        checkBox.setOnAction(event -> {
		            if (!isTableInitializing) {
		                AccessLevel newLevel = determineNewAccessLevel(
		                    true,
		                    checkBox.isSelected(),
		                    perm.canDownload(),
		                    perm.canDelete()
		                );
		                perm.setAccessLevel(newLevel);
		                System.out.println("Updated " + perm.getUser().getLogin() + 
		                    " - Write: " + checkBox.isSelected() + 
		                    ", AccessLevelId: " + perm.getAccessLevelId());
		                permissionsTable.refresh();
		            }
		        });
		        
		        setGraphic(checkBox);
		    }
		});
		
		downloadColumn.setCellFactory(_ -> new CheckBoxTableCell<UserPermission, Boolean>() {
		    private CheckBox checkBox;
		    
		    @Override
		    public void updateItem(Boolean item, boolean empty) {
		        super.updateItem(item, empty);
		        if (empty || getTableRow() == null || getTableRow().getItem() == null) {
		            setGraphic(null);
		            return;
		        }
		        
		        UserPermission perm = getTableRow().getItem();
		        if (checkBox == null) {
		            checkBox = new CheckBox();
		        }
		        
		        checkBox.setSelected(perm.canDownload());
		        checkBox.setOnAction(_ -> {
		            if (!isTableInitializing) {
		                AccessLevel newLevel = determineNewAccessLevel(
		                    true,
		                    perm.canWrite(),
		                    checkBox.isSelected(),
		                    perm.canDelete()
		                );
		                perm.setAccessLevel(newLevel);
		                System.out.println("Updated " + perm.getUser().getLogin() + 
		                    " - Download: " + checkBox.isSelected() + 
		                    ", AccessLevelId: " + perm.getAccessLevelId());
		                permissionsTable.refresh();
		            }
		        });
		        
		        setGraphic(checkBox);
		    }
		});
		
		deleteColumn.setCellFactory(_ -> new CheckBoxTableCell<UserPermission, Boolean>() {
		    private CheckBox checkBox;
		    
		    @Override
		    public void updateItem(Boolean item, boolean empty) {
		        super.updateItem(item, empty);
		        if (empty || getTableRow() == null || getTableRow().getItem() == null) {
		            setGraphic(null);
		            return;
		        }
		        
		        UserPermission perm = getTableRow().getItem();
		        if (checkBox == null) {
		            checkBox = new CheckBox();
		        }
		        
		        checkBox.setSelected(perm.canDelete());
		        checkBox.setOnAction(_ -> {
		            if (!isTableInitializing) {
		                AccessLevel newLevel = determineNewAccessLevel(
		                    true,
		                    perm.canWrite(),
		                    perm.canDownload(),
		                    checkBox.isSelected()
		                );
		                perm.setAccessLevel(newLevel);
		                System.out.println("Updated " + perm.getUser().getLogin() + 
		                    " - Delete: " + checkBox.isSelected() + 
		                    ", AccessLevelId: " + perm.getAccessLevelId());
		                permissionsTable.refresh();
		            }
		        });
		        
		        setGraphic(checkBox);
		    }
		});

        // Make table editable
        permissionsTable.setEditable(true);

        // Bind table data
        permissionsTable.setItems(userPermissions);

        // Set up context menu for right-click delete
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteMenuItem = new MenuItem("Supprimer");
        deleteMenuItem.setOnAction(_ -> {
            UserPermission selectedUser = permissionsTable.getSelectionModel().getSelectedItem();
            if (selectedUser != null) {
                removePermission(selectedUser);
            }
        });
        contextMenu.getItems().add(deleteMenuItem);

        // Show context menu on right-click
        permissionsTable.setRowFactory(_ -> {
            TableRow<UserPermission> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });

        // Configure button actions
        addUserButton.setOnAction(_ -> {
            try {
                addSelectedUser();
            } catch (NoSuchAlgorithmException e) {
                showAlert("Erreur lors de l'ajout de l'utilisateur: " + e.getMessage());
                e.printStackTrace();
            }
        });
        cancelButton.setOnAction(_ -> {
            // Restore any removed permissions since we're canceling
            userPermissions.addAll(permissionsToDelete);
            permissionsToDelete.clear();
            closeDialog();
        });

        validateButton.setOnAction(_ -> savePermissions());

        // Set up search field with filtering
        setupSearchField();
    }

    private AccessLevel determineNewAccessLevel(boolean read, boolean write, boolean download, boolean delete) {
        read = true; // Enforce read permission
        int accessLevelId = determineAccessLevelId(read, write, download, delete);
        System.out.println("Calculated accessLevelId: " + accessLevelId);
        try {
            return AccessLevel.fromId(accessLevelId);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid access level ID: " + accessLevelId);
            return AccessLevel.READ_ONLY; // Default to READ_ONLY on error
        }
    }

    private int determineAccessLevelId(boolean read, boolean write, boolean download, boolean delete) {
        if (read && write && download && delete) return 7;  // ALL_PERMISSIONS
        if (read && write && delete) return 6;             // READ_WRITE_DELETE
        if (read && write && download) return 5;           // READ_WRITE_DOWNLOAD
        if (read && delete) return 4;                      // READ_DELETE
        if (read && download) return 3;                    // READ_DOWNLOAD
        if (read && write) return 2;                       // READ_WRITE
        return 1;                                          // READ_ONLY
    }

    public void setDocument(PersonalDocument document) {
        System.out.println("setDocument called with document: " + (document != null ? document.getFileName() : "null"));
        this.document = document;
        if (document != null && fileNameLabel != null) {
            fileNameLabel.setText("Partager le fichier: " + document.getFileName());
        }
        initializeIfReady();
    }

    public void setClientSocketManager(ClientSocketManager clientSocketManager) {
        System.out.println("setClientSocketManager called with clientSocketManager: " + (clientSocketManager != null ? "not null" : "null"));
        this.clientSocketManager = clientSocketManager;
        if (clientSocketManager != null) {
            availableUsers.setAll(clientSocketManager.getListUsersShare(this.document.getDocID()));
            updateFilteredUsers("");
        }
        initializeIfReady();
    }

    private void initializeIfReady() {
        if (document != null && clientSocketManager != null && !isInitialized) {
            System.out.println("Initializing controller for document: " + document.getFileName());
            loadExistingPermissions();
            isInitialized = true;
        } else {
            System.out.println("Cannot initialize yet: document=" + document + ", clientSocketManager=" + clientSocketManager);
        }
    }

    private void loadExistingPermissions() {
        userPermissions.clear();
        initialPermissions.clear();
        loadPermissions();
        initialPermissions.addAll(userPermissions); // Store initial state
    }

    private void loadPermissions() {
        if (document == null || clientSocketManager == null) {
            showAlert("Erreur: Document ou connexion au serveur non défini.");
            return;
        }
        try {
            isTableInitializing = true; // Prevent listener triggers during loading
            List<UserPermission> permissions = clientSocketManager.getDocumentPermissions(document.getDocID());
            System.out.println("Loaded " + permissions.size() + " permissions for docID: " + document.getDocID());
            for (UserPermission perm : permissions) {
                System.out.println("User: " + perm.getUser().getLogin() +
                        ", AccessLevelId: " + perm.getAccessLevelId() +
                        ", AccessLevel: " + perm.getAccessLevel() +
                        ", Read: " + perm.canRead() +
                        ", Write: " + perm.canWrite() +
                        ", Download: " + perm.canDownload() +
                        ", Delete: " + perm.canDelete());
            }
            userPermissions.setAll(permissions);
            permissionsTable.refresh();
            isTableInitializing = false; // Re-enable listeners
        } catch (Exception e) {
            showAlert("Erreur lors du chargement des permissions depuis le serveur: " + e.getMessage());
            System.err.println("Error loading permissions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addSelectedUser() throws NoSuchAlgorithmException {
        String username = searchUserField.getValue();
        if (username == null || username.trim().isEmpty()) {
            showAlert("Veuillez sélectionner un nom d'utilisateur.");
            return;
        }
        if (userExists(username)) {
            showAlert("Cet utilisateur existe déjà dans la liste.");
            return;
        }
        User selectedClient = availableUsers.stream()
                .filter(user -> user.getLogin() != null && user.getLogin().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
        if (selectedClient == null) {
            showAlert("Utilisateur non trouvé dans la liste des utilisateurs disponibles.");
            return;
        }

        UserPermission newPermission = new UserPermission(
                new User(selectedClient.getUserID(), username, null, null, null),
                AccessLevel.READ_ONLY,
                document
        );
        userPermissions.add(newPermission);
        searchUserField.setValue(null);
        permissionsTable.refresh();
    }

    private boolean userExists(String username) {
        return userPermissions.stream()
                .anyMatch(perm -> perm.getUser().getLogin().equalsIgnoreCase(username));
    }

    private void removePermission(UserPermission permission) {
        if (permission != null) {
            userPermissions.remove(permission);
            permissionsToDelete.add(permission);
            availableUsers.add(permission.getUser());
            updateFilteredUsers("");
        }
    }

	private void setupSearchField() {
	    searchUserField.setItems(filteredUserLogins);
	    searchUserField.setEditable(true);
	
	    // Simple text change listener
	    searchUserField.getEditor().textProperty().addListener((_, _, newValue) -> {
	        updateFilteredUsers(newValue);
	        addUserButton.setDisable(newValue == null || newValue.trim().isEmpty() || 
	            !filteredUserLogins.contains(newValue.trim()));
	    });
	}

	private void updateFilteredUsers(String searchText) {
	    filteredUserLogins.clear();
	    List<String> availableLogins = availableUsers.stream()
	        .map(User::getLogin)
	        .filter(login -> login != null && 
	            !userPermissions.stream()
	                .anyMatch(perm -> perm.getUser().getLogin().equalsIgnoreCase(login)))
	        .collect(Collectors.toList());

	    if (searchText == null || searchText.trim().isEmpty()) {
	        filteredUserLogins.addAll(availableLogins);
	    } else {
	        filteredUserLogins.addAll(
	            availableLogins.stream()
	                .filter(login -> login.toLowerCase().contains(searchText.toLowerCase()))
	                .collect(Collectors.toList())
	        );
	    }
	}

	private void savePermissions() { // add supprimer
	    if (document == null || clientSocketManager == null) {
	        showAlert("Erreur: Document ou connexion au serveur non défini.");
	        return;
	    }
	
	    System.out.println("Saving permissions for: " + document.getFileName());
	    boolean anyFailures = false;
	
	    // First, process deletions
	    for (UserPermission perm : permissionsToDelete) {
	        try {
	            boolean success = clientSocketManager.removeShare(document.getDocID(), perm.getUser().getUserID());
	            if (!success) {
	                anyFailures = true;
	                System.err.println("Failed to remove share for user: " + perm.getUser().getLogin());
	                showAlert("Erreur lors de la suppression de la permission pour " + perm.getUser().getLogin());
	                userPermissions.add(perm); // Revert the removal if it failed
	            } else {
	                System.out.println("Successfully removed permission for user: " + perm.getUser().getLogin());
	            }
	        } catch (Exception e) {
	            anyFailures = true;
	            System.err.println("Error removing permission for user " + perm.getUser().getLogin() + ": " + e.getMessage());
	            e.printStackTrace();
	            userPermissions.add(perm); // Revert the removal on error
	            showAlert("Erreur lors de la suppression de la permission pour " + perm.getUser().getLogin() + ": " + e.getMessage());
	        }
	    }
	
	    // Then process updates for remaining permissions
	    for (UserPermission perm : userPermissions) {
	        System.out.printf("Processing %s (ID: %d) - Read: %b, Write: %b, Download: %b, Delete: %b, AccessLevelId: %d%n",
	                perm.getUser().getLogin(), perm.getUser().getUserID(), perm.canRead(), perm.canWrite(),
	                perm.canDownload(), perm.canDelete(), perm.getAccessLevelId());
	
	        try {
	            boolean success = clientSocketManager.shareDocument(
	                document.getDocID(), 
	                perm.getUser().getUserID(), 
	                perm.getAccessLevelId()
	            );
	
	            if (!success) {
	                anyFailures = true;
	                System.err.println("Server returned false for sharing document with user: " + perm.getUser().getLogin());
	                showAlert("Erreur lors de la mise à jour des permissions pour " + perm.getUser().getLogin());
	            } else {
	                System.out.println("Successfully updated permissions for user: " + perm.getUser().getLogin());
	            }
	        } catch (Exception e) {
	            anyFailures = true;
	            System.err.println("Error updating permissions for user " + perm.getUser().getLogin() + ": " + e.getMessage());
	            e.printStackTrace();
	            showAlert("Erreur lors de la mise à jour des permissions pour " + perm.getUser().getLogin() + ": " + e.getMessage());
	        }
	    }
	
	    if (!anyFailures) {
	        closeDialog();
	    } else {
	        permissionsTable.refresh(); // Refresh to show any reverted changes
	    }
	}
	
    private void closeDialog() {
        if (cancelButton.getScene() != null && cancelButton.getScene().getWindow() != null) {
            cancelButton.getScene().getWindow().hide();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}