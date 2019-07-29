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
 * Pane visualizing a "candidate for partition" and the button to
 */
public class VisualCandidate extends BorderPane {
    private BooleanProperty detailsOnScreen = new SimpleBooleanProperty(false);

    public VisualCandidate(String uri, CollectionsModel colMd, Button button, TextField textField) {
        super();
        Label text = new Label(colMd.getGraph().shortForm(uri));
        BorderPane topPane = new BorderPane();

        topPane.setLeft(button);
        topPane.setCenter(text);

        Button detailsButton = new Button("More Details");
        topPane.setRight(detailsButton);

        setTop(topPane);

        detailsButton.setOnMouseClicked(mouseEvent -> {
            if (!detailsOnScreen.getValue()) {
                this.promptDetails(detailsButton, uri, colMd, textField);
            } else {
                this.clearDetails(detailsButton);
            }
        });

        setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
    }

    private void promptDetails(Button details, String uri, CollectionsModel colMd, TextField textField) {
        Thread thread = new Thread(() -> {
            VisualGraphNode visualDetails = new VisualGraphNode(uri, colMd, textField);
            Platform.runLater(() -> setBottom(visualDetails.dbPrompt));
            Platform.runLater(() -> details.setText("Less Details"));
            detailsOnScreen.setValue(true);
            Thread.currentThread().interrupt();
        });
        thread.start();
    }

    private void clearDetails(Button details) {
        Platform.runLater(() -> setBottom(null));
        Platform.runLater(() -> details.setText("More Details"));
        detailsOnScreen.setValue(false);
    }
}
