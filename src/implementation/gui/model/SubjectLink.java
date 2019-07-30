package implementation.gui.model;

import implementation.gui.controller.FieldTyper;
import javafx.application.Platform;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;

/**
 * A hyperlink using the filter textfield to navigate to the node
 *
 * @author nk-fouque
 */
public class SubjectLink extends Hyperlink {
    public SubjectLink(String uri, TextField filter) {
        super(uri);
        setOnMouseClicked(mouseEvent -> {
            FieldTyper.typeAndTrigger(filter,uri);
            Platform.runLater(() -> this.visitedProperty().setValue(false));
        });
    }
}
