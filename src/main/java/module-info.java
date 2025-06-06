module com.emi.projetintegre {
	// transitive ensures dependencies inherit it
    requires transitive javafx.graphics;
	
    // Modules JavaFX requis
    requires javafx.controls;
    requires javafx.fxml;
    
    // Modules pour votre fonctionnalité
    requires java.sql;
    requires org.bouncycastle.provider; // Pour le cryptage
    requires org.slf4j;
	requires java.desktop; // Pour les logs
    
    // Ouverture des packages pour JavaFX/FXML
    opens com.emi.projetintegre to javafx.fxml;
    opens com.emi.projetintegre.controllers to javafx.fxml;
    opens com.emi.projetintegre.models to javafx.base;
    
    // Export des packages principaux
    exports com.emi.projetintegre;
    exports com.emi.projetintegre.controllers;
    exports com.emi.projetintegre.services;
}