package implementation.gui.controller;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;


public class FieldTyper {
    public static void typeAndTrigger(TextField textField, String s){
        Platform.runLater(() -> textField.setText(s));
        KeyEvent trigger = new KeyEvent(KeyEvent.KEY_PRESSED, "", "",
                KeyCode.ENTER, false, false, false, false);
        Platform.runLater(() -> textField.getOnKeyPressed().handle(trigger));
    }
}
