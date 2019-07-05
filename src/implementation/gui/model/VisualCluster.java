package implementation.gui.model;

import implementation.Cluster;
import implementation.utils.CollectionsModel;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

public class VisualCluster extends TitledPane {
    private Cluster cluster;

    public VisualCluster(Cluster c, CollectionsModel md){
        super();
        cluster=c;
        this.setText("Extensional distance : "+c.getExtensionDistance());
        if (c.getAvailableQueryElements().size() != 0) {
            this.textFillProperty().setValue(Paint.valueOf("#cd7777"));
        } else {
            this.textFillProperty().setValue(Paint.valueOf("#484848"));
        }

        // A HBox containing several VBox is like a BorderPane but it's better in case more information become useful and we need more columns
        HBox box = new HBox();
        this.setContent(box);

        // Left side of the Pane
        VBox texts = new VBox();
        Text similitude = new Text("Similitude : \n" + c.relaxQueryElementsString(md));
        Text neighbors = new Text("\nNeighbors : \n" + c.answersListString(md));
        texts.getChildren().addAll(similitude,neighbors);
        texts.autosize();

        // Right Side of the Pane
        VBox gadgets = new VBox();
        CopyButton copy = new CopyButton(texts);
        Label extensional = new Label("Extensional distance : "+c.getExtensionDistance());
        Label intensional = new Label("Intensional similitude : "+c.getRelaxQueryElements().size());
        Label relax = new Label("Number of relaxations : "+c.getRelaxDistance());
        gadgets.getChildren().addAll(copy,extensional,intensional,relax);
        gadgets.autosize();

        // Whole Pane
        box.getChildren().add(texts);
        box.getChildren().add(gadgets);
        box.setMargin(texts,new Insets(10));
        box.setMargin(gadgets,new Insets(10));
        box.autosize();

        this.autosize();
    }
}
