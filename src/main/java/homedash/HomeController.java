package homedash;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class HomeController {


    @FXML
    private Accordion acc;

    @FXML
    private AnchorPane anchor1;

    @FXML
    private Button upload, b2, b3, b4, b5, decrypte, delete, download, encrypt, open, share;

    @FXML
    private BorderPane bord1, bp1;

    @FXML
    private Label dt, files, l4, l7, l9, last, nm, nom, pres, size, space, sz, sz2, time, title;

    @FXML
    private HBox h1, hb10, hb3, hb5, hb6;

    @FXML
    private ImageView im10, im12, im2, im3, im4, im6, im9, logo;

    @FXML
    private Pane im5, p1, p2, p3, p4, p6, p8, p10, p12;

    @FXML
    private VBox vb1, vb4, vb10;

    @FXML
    private ProgressBar progress;

    @FXML
    private TextField search;
   
    private File selectedFile; // Le fichier sélectionné

    // Action pour "Déposer un fichier"
    @FXML
    private void handleUploadFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier à déposer");
        Stage stage = new Stage();
        selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                File destination = new File("fichiers", selectedFile.getName());
                destination.getParentFile().mkdirs();
                Files.copy(selectedFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Insérer dans la base de données
                DatabaseManager.insertFile(selectedFile.getName(), destination.getAbsolutePath(), selectedFile.length());

                System.out.println("✅ Fichier déposé et enregistré !");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Action pour "Crypter le fichier"
    @FXML
    private void handleEncryptFile(ActionEvent event) {
        if (selectedFile != null) {
            File encryptedFile = new File("fichiers", "encrypted_" + selectedFile.getName());
            try {
                CryptoUtils.encryptFile(selectedFile, encryptedFile);
                System.out.println("🔒 Fichier crypté avec succès !");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ Veuillez d'abord déposer un fichier !");
        }
    }

    // Action pour "Décrypter le fichier"
    @FXML
    private void handleDecryptFile(ActionEvent event) {
        if (selectedFile != null) {
            File decryptedFile = new File("fichiers", "decrypted_" + selectedFile.getName());
            try {
                CryptoUtils.decryptFile(selectedFile, decryptedFile);
                System.out.println("🔓 Fichier décrypté avec succès !");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ Veuillez d'abord déposer un fichier !");
        }
    }
}

