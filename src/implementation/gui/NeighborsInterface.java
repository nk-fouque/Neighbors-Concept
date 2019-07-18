package implementation.gui;

import implementation.NeighborsImplementation;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.BasicConfigurator;

public class NeighborsInterface extends Application {
    public static Stage stage = null;
    public static Scene scene = null;
    public static BooleanProperty exit = new SimpleBooleanProperty(false);

    /**
     * Classical JavaFX setup with added configurators for Jena and Partition Algorithm
     */
    public static void main(String[] args) {
        BasicConfigurator.configure();
        NeighborsImplementation.myLogsLevels("silent");
        launch(args);
    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource("view/neighborsInterface.fxml"));
        Scene scene = new Scene(root);
        NeighborsInterface.scene = scene;
        primaryStage.setScene(scene);
        primaryStage.setTitle("Neighbors Concept");
        primaryStage.show();
        primaryStage.centerOnScreen();
        primaryStage.setOnCloseRequest(windowEvent -> {
            exit.setValue(true);
        });

    }
}
