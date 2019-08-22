package implementation.gui.model;

import implementation.algorithms.Cluster;
import implementation.gui.controller.NeighborsController;
import implementation.gui.controller.PartitionAdditional;
import implementation.utils.CollectionsModel;
import implementation.utils.ElementUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import org.apache.jena.sparql.syntax.Element;

import java.util.Collections;
import java.util.Set;

/**
 * TitledPane Tailored to a Cluster
 *
 * @author nk-fouque
 */
public class VisualCluster extends TitledPane implements Comparable{

    /**
     * The final cluster resulting in this answer
     * TODO : More informations could be extracted from the cluster, open to suggestions
     */
    public Cluster cluster;

    public VisualCluster(Cluster c, CollectionsModel colMd, TextField filterField, NeighborsController mainController) {
        super();
        cluster = c;
        this.setText("Extensional distance : " + c.getExtensionDistance());
        Set<Element> remaining = c.getAvailableQueryElements();
        boolean finished = true;
        if (remaining.size() != 0) {
            for (Element e : remaining) {
                if (c.connected(ElementUtils.mentioned(e))) {
                    finished = false;
                    break;
                }
            }
        }
        if (finished)
            this.textFillProperty().setValue(Paint.valueOf("#484848"));
        else
            this.textFillProperty().setValue(Paint.valueOf("#cd7777"));

        // A VBox containing several HBox is like a BorderPane but it's better in case more information become useful and we need more columns/lines and in case we don't want them aligned
        VBox box = new VBox(10);
        this.setContent(box);

        // Bottom of the Pane
        HBox texts = new HBox(20);
        Text similitude = new Text("Similitude : \n" + c.relaxQueryElementsString(colMd));
        VBox neighbors = new VBox();
        neighbors.getChildren().add(new Label("Neighbors"));
        c.answersListString(colMd).lines().forEach(s -> {
            neighbors.getChildren().add(new SubjectLink(s, filterField));
        });
        texts.getChildren().addAll(neighbors, similitude);
        texts.autosize();

        // Top of the Pane
        HBox gadgets = new HBox(10);
        Label extensional = new Label("Extensional distance : " + c.getExtensionDistance());
        Label intensional = new Label("Intensional similitude : " + c.getRelaxQueryElements().size());
        Label relax = new Label("Number of relaxations : " + c.getRelaxDistance());
        CopyButton copy = new CopyButton(texts);
        gadgets.getChildren().addAll(extensional, intensional, relax, copy);
        if (!finished) {
            Button further = new Button();
            further.setText("Further partition this cluster");
            further.setOnMouseClicked(mouseEvent -> {
                mainController.furtherPartition(Collections.singleton(cluster));
            });
            gadgets.getChildren().add(further);
        }
        gadgets.autosize();

        // Whole Pane
        box.getChildren().add(gadgets);
        box.getChildren().add(texts);
        HBox.setMargin(texts, new Insets(10));
        HBox.setMargin(gadgets, new Insets(10));
        box.autosize();

        this.autosize();
        this.setExpanded(false);
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }

    public int compareTo(VisualCluster other){
        return cluster.compareTo(other.cluster);
    }

    /**
     * A Button linked to a cluster to copy its results to clipboard
     */
    private class CopyButton extends Button {
        /**
         * VBox containing the information to copy
         */
        private HBox copyTarget;

        private CopyButton(HBox texts) {
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
