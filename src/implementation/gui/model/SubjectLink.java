package implementation.gui.model;

import javafx.application.Platform;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class SubjectLink extends Hyperlink {
    public SubjectLink(String uri, TextField filter) {
        super(uri);
        setOnMouseClicked(mouseEvent -> {
            Platform.runLater(() -> filter.setText(uri));
            KeyEvent trigger = new KeyEvent(KeyEvent.KEY_PRESSED, "", "",
                    KeyCode.ENTER, false, false, false, false);
            Platform.runLater(() -> filter.getOnKeyPressed().handle(trigger));
        });
    }
}
