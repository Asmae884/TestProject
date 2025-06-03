package homedash;
import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class EmptyTrash {
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
	    private ImageView emptyimg;

	    @FXML
	    private TilePane folderTile;

	    @FXML
	    private HBox h1;

	    @FXML
	    private VBox headerBox;

	    @FXML
	    private Button homeBtn;

	    @FXML
	    private Text lb1;

	    @FXML
	    private Text lb2;

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


