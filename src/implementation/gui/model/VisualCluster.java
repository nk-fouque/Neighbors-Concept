package implementation.gui.model;

import implementation.algorithms.Cluster;
import implementation.utils.CollectionsModel;
import implementation.utils.elements.QueryElement;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import java.util.Set;

/**
 * TitledPane Tailored to a Cluster
 */
public class VisualCluster extends TitledPane {

    /**
     * The final cluster resulting in this answer
     * TODO : Get more informations from the cluster
     */
    private Cluster cluster;

    public VisualCluster(Cluster c, CollectionsModel md) {
        super();
        cluster = c;
        this.setText("Extensional distance : " + c.getExtensionDistance());
        Set<QueryElement> remaining = c.getAvailableQueryElements();
        boolean finished = true;
        if (remaining.size() != 0) {
            for (QueryElement e : remaining) {
                if (c.connected(e.mentionedVars())) {
                    finished = false;
                    break;
                }
            }
        }

        if (finished)
            this.textFillProperty().setValue(Paint.valueOf("#484848"));
        else
            this.textFillProperty().setValue(Paint.valueOf("#cd7777"));


        // A HBox containing several VBox is like a BorderPane but it's better in case more information become useful and we need more columns
        HBox box = new HBox();
        this.setContent(box);

        // Left side of the Pane
        VBox texts = new VBox();
        Text similitude = new Text("Similitude : \n" + c.relaxQueryElementsString(md));
        Text neighbors = new Text("\nNeighbors : \n" + c.answersListString(md));
        texts.getChildren().addAll(similitude, neighbors);
        texts.autosize();

        // Right Side of the Pane
        VBox gadgets = new VBox();
        CopyButton copy = new CopyButton(texts);
        Label extensional = new Label("Extensional distance : " + c.getExtensionDistance());
        Label intensional = new Label("Intensional similitude : " + c.getRelaxQueryElements().size());
        Label relax = new Label("Number of relaxations : " + c.getRelaxDistance());
        gadgets.getChildren().addAll(copy, extensional, intensional, relax);
        gadgets.autosize();

        // Whole Pane
        box.getChildren().add(texts);
        box.getChildren().add(gadgets);
        HBox.setMargin(texts, new Insets(10));
        HBox.setMargin(gadgets, new Insets(10));
        box.autosize();

        this.autosize();
    }

    /**
     * A Button linked to a cluster to copy its results to clipboard
     */
    private class CopyButton extends Button {
        /**
         * VBox containing the information to copy
         */
        private VBox copyTarget;

        private CopyButton(VBox texts) {
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
}
