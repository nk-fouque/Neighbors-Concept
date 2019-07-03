package implementation.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class NeighborsInterface extends Application {
    public static Stage stage = null;
    public static Scene scene = null;

    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getLogger("org.apache.jena").setLevel(Level.INFO);
        Logger.getLogger("implementation.Partition").setLevel(Level.INFO);
        Logger.getLogger("implementation.Cluster").setLevel(Level.INFO);
        Logger.getLogger("implementation.utils").setLevel(Level.OFF);
        launch(args);
    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource("view/neighborsInterface.fxml"));
        Scene scene = new Scene(root);
        this.scene = scene;
        primaryStage.setScene(scene);
        primaryStage.setTitle("Neighbors Concept");
        primaryStage.show();
        primaryStage.centerOnScreen();

    }
}
