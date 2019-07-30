package implementation.gui.model;

import implementation.utils.CollectionsModel;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * Pane visualizing a potential nodes to be partitioned
 * Able to display details about the node for navigation
 *
 * @author nk-fouque
 */
public class VisualCandidate extends BorderPane {
    private BooleanProperty detailsOnScreen = new SimpleBooleanProperty(false);

    /**
     * Base constructor
     * @param uri The uri of the node to be visualized
     * @param colMd The Model in which the node is to be described
     * @param selectedNodeField The field to update when the node is selected
     * @param filterField The field to update with the filters for navigation
     */
    public VisualCandidate(String uri, CollectionsModel colMd,TextField selectedNodeField, TextField filterField) {
        super();
        Label text = new Label(colMd.shortform(uri));
        BorderPane topPane = new BorderPane();


        Button button = new Button();
        button.textProperty().setValue("Select this node");
        button.setOnMouseClicked(mouseEvent -> {
            selectedNodeField.setText(uri);
            selectedNodeField.autosize();
        });
        topPane.setRight(button);

        topPane.setCenter(text);

        Button detailsButton = new Button("More Details");
        topPane.setLeft(detailsButton);

        setTop(topPane);

        detailsButton.setOnMouseClicked(mouseEvent -> {
            if (!detailsOnScreen.getValue()) {
                this.displayDetails(detailsButton, uri, colMd, filterField);
            } else {
                this.clearDetails(detailsButton);
            }
        });

        setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
    }

    /**
     * Displays a details about the node
     */
    private void displayDetails(Button details, String uri, CollectionsModel colMd, TextField textField) {
        Thread thread = new Thread(() -> {
            VisualGraphNode visualDetails = new VisualGraphNode(uri, colMd, textField);
            Platform.runLater(() -> setBottom(visualDetails.dbPrompt));
            Platform.runLater(() -> details.setText("Less Details"));
            detailsOnScreen.setValue(true);
            Thread.currentThread().interrupt();
        });
        thread.start();
    }

    /**
     * Hides details about the node
     */
    private void clearDetails(Button details) {
        Platform.runLater(() -> setBottom(null));
        Platform.runLater(() -> details.setText("More Details"));
        detailsOnScreen.setValue(false);
    }
}
