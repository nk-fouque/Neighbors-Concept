package implementation.gui.model;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * A Button linked to a cluster to copy its results to clipboard
 */
public class CopyButton extends Button {
    /**
     * VBox containing the information to copy
     */
    private VBox copyTarget;

    public CopyButton(VBox texts) {
        super("Copy to clipboard");
        copyTarget = texts;

        this.setOnMouseClicked(mouseEvent -> {
            StringBuilder copy = new StringBuilder();
            for (Node n : copyTarget.getChildren()) {
                if (n instanceof Text) {
                    copy.append(((Text) n).getText());
                }
            }
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(copy.toString());
            clipboard.setContent(content);
            Thread thread = new Thread(() -> {
                Platform.runLater(() -> this.setText("Copied !"));
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> this.setText("Copy to clipboard"));
                Thread.currentThread().interrupt();
            });
            thread.start();
        });
    }
}
