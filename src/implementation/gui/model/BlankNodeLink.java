package implementation.gui.model;

import implementation.gui.controller.FieldTyper;
import javafx.application.Platform;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;

public class BlankNodeLink extends Hyperlink {
    public BlankNodeLink(String hash, TextField filter) {
        super("b_");
        setOnMouseClicked(mouseEvent -> {
            FieldTyper.typeAndTrigger(filter,hash);
            Platform.runLater(() -> this.visitedProperty().setValue(false));
        });
    }
}
