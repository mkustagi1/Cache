package cache.util;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;

/**
 *
 * @author Manjunath Kustagi
 */
public class ChartLegend implements Initializable {

    @FXML
    private AnchorPane chartLegend;

    @FXML
    private ToggleButton nrButton;

    @FXML
    private ToggleButton trButton;

    @FXML
    private ToggleButton mayrButton;
    
    @FXML
    private ToggleButton bartelButton;
    
    @FXML
    private ToggleButton cageButton;
    
    public ToggleButton getNRButton(){
        return nrButton;
    }
    
    public ToggleButton getTRButton(){
        return trButton;
    }
    
    public ToggleButton getMayrButton(){
        return mayrButton;
    }

    public ToggleButton getBartelButton(){
        return bartelButton;
    }
    
    public ToggleButton getCageButton(){
        return cageButton;
    }

    public ReadOnlyBooleanProperty nrButtonSelectedProperty() {
        return nrButton.selectedProperty();
    }
    
    public ReadOnlyBooleanProperty trButtonSelectedProperty() {
        return trButton.selectedProperty();
    }
    
    public ReadOnlyBooleanProperty mayrButtonSelectedProperty() {
        return mayrButton.selectedProperty();
    }

    public ReadOnlyBooleanProperty bartelButtonSelectedProperty() {
        return bartelButton.selectedProperty();
    }

    public ReadOnlyBooleanProperty cageButtonSelectedProperty() {
        return cageButton.selectedProperty();
    }

    public ChartLegend() {
    }

    @FXML
    private void nrButtonAction(ActionEvent event) {
        
    }
    
    @FXML
    private void trButtonAction(ActionEvent event) {
        
    }

    @FXML
    private void mayrButtonAction(ActionEvent event) {
        
    }

    @FXML
    private void bartelButtonAction(ActionEvent event) {
        
    }

    @FXML
    private void cageButtonAction(ActionEvent event) {
        
    }    

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }
}
