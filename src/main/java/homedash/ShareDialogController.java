package homedash;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.Stage;

public class ShareDialogController {

    private Fichier fichier; // Fichier actuellement sélectionné

    @FXML
    private Button addUserButton;

    @FXML
    private Button cancelButton;

    @FXML
    private TableColumn<UtilisateurPermission, Boolean> deleteColumn;

    @FXML
    private Label fileNameLabel;

    @FXML
    private TableView<UtilisateurPermission> permissionsTable;

    @FXML
    private TableColumn<UtilisateurPermission, Boolean> readColumn;

    @FXML
    private TextField searchUserField;

    @FXML
    private TableColumn<UtilisateurPermission, Boolean> uploadColumn;

    @FXML
    private TableColumn<UtilisateurPermission, String> userColumn;

    @FXML
    private Button validateButton;

    @FXML
    private TableColumn<UtilisateurPermission, Boolean> writeColumn;

    @FXML
    public void initialize() {
    	permissionsTable.setEditable(true);

        // Initialisation des colonnes de la table
        userColumn.setCellValueFactory(cellData -> cellData.getValue().nomUtilisateurProperty());
        
        readColumn.setCellValueFactory(cellData -> cellData.getValue().lireProperty());
        readColumn.setCellFactory(CheckBoxTableCell.forTableColumn(readColumn));

        writeColumn.setCellValueFactory(cellData -> cellData.getValue().ecrireProperty());
        writeColumn.setCellFactory(CheckBoxTableCell.forTableColumn(writeColumn));

        uploadColumn.setCellValueFactory(cellData -> cellData.getValue().chargerProperty());
        uploadColumn.setCellFactory(CheckBoxTableCell.forTableColumn(uploadColumn));

        deleteColumn.setCellValueFactory(cellData -> cellData.getValue().supprimerProperty());
        deleteColumn.setCellFactory(CheckBoxTableCell.forTableColumn(deleteColumn));
    }

    public void initData(Fichier fichier) {
        this.fichier = fichier; // Récupérer le fichier sélectionné
        fileNameLabel.setText(fichier.getNom());
    }

    @FXML
    private void ajouterUtilisateur(ActionEvent event) {
        String nomUtilisateur = searchUserField.getText().trim();
        if (!nomUtilisateur.isEmpty()) {
            if (UtilisateurDAO.userExists(nomUtilisateur)) {
                // Vérifier si l'utilisateur est déjà dans la table
                boolean alreadyExists = permissionsTable.getItems().stream()
                        .anyMatch(p -> p.getNomUtilisateur().equals(nomUtilisateur));

                if (!alreadyExists) {
                    UtilisateurPermission permission = new UtilisateurPermission(
                            nomUtilisateur, true, false, false, false);
                    permissionsTable.getItems().add(permission);
                    searchUserField.clear();
                } else {
                    // Afficher un message d'erreur (optionnel)
                    System.out.println("Utilisateur déjà ajouté");
                }
            } else {
                // Afficher un message d'erreur (optionnel)
                System.out.println("Utilisateur non trouvé");
            }
        }
    }


    
    @FXML
    private void validerPartage(ActionEvent event) {
        permissionsTable.getSelectionModel().clearSelection();
        int ownerId = Session.getUserId(); // L'utilisateur connecté est le propriétaire

        for (UtilisateurPermission permission : permissionsTable.getItems()) {
            PartageDAO.partagerFichier(fichier.getNom(), permission, ownerId);
        }

        Stage stage = (Stage) validateButton.getScene().getWindow();
        stage.close();
    }

}

