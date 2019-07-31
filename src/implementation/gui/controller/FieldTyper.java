package implementation.gui.controller;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Simulating entries in the TextField was easier than programming another method to dop the same thing
 *
 * @author nk-fouque
 */
public class FieldTyper {
    public static void typeAndTrigger(TextField textField, String s){
        type(textField,s);
        enter(textField);
    }

    public static void type(TextField textField, String s){
        Platform.runLater(() -> textField.setText(s));
    }

    public static void enter(TextField textField){
        KeyEvent trigger = new KeyEvent(KeyEvent.KEY_PRESSED, "", "",
                KeyCode.ENTER, false, false, false, false);
        Platform.runLater(() -> textField.getOnKeyPressed().handle(trigger));

    }
}
