package implementation.gui.controller;

import implementation.algorithms.Cluster;
import implementation.utils.JSONable;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;

public class JSONButton extends Button {
    /**
     * VBox containing the information to copy
     */
    private JSONable copyTarget;

    public <O extends JSONable> JSONButton(O o) {
        super("Copy JSON to clipboard");
        copyTarget = o;

        this.setOnMouseClicked(mouseEvent -> {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(copyTarget.toJson());
            clipboard.setContent(content);
            Thread thread = new Thread(() -> {
                Platform.runLater(() -> this.setText("Copied !"));
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> this.setText("Copy JSON to clipboard"));
                Thread.currentThread().interrupt();
            });
            thread.start();
        });
    }
}
