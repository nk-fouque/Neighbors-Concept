package implementation.gui.model;

import implementation.utils.CollectionsModel;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;

import java.util.List;
import java.util.Map;

public class VisualGraphNode {

    TitledPane predicatesFrom;
    TitledPane predicatesTo;

    public VisualGraphNode(String uri, CollectionsModel colMd, TextField textField) {
        Map<Property, List<RDFNode>> propertiesFrom;
        Map<Property, List<RDFNode>> propertiesTo;

        predicatesFrom = new TitledPane();
        predicatesFrom.setText("Vertices from this node");
        Accordion knowledgeFrom = new Accordion();
        propertiesFrom = colMd.getTriplesSimple().get(uri);
        propertiesFrom.keySet().forEach(property -> {
            TitledPane thisPredicateFrom = new TitledPane();
            thisPredicateFrom.setText(property.getLocalName());
            VBox objects = new VBox();
            propertiesFrom.get(property).forEach(node -> {
                BorderPane content = new BorderPane();
                Label vertex = new Label(colMd.getGraph().shortForm(uri) + " " + colMd.getGraph().shortForm(property.getLocalName()) + " " + colMd.getGraph().shortForm(node.toString()));
                Button objectDetails = new DetailsButton(node.toString(), textField);
                content.setLeft(vertex);
                content.setRight(objectDetails);
                objects.getChildren().add(content);
            });
            thisPredicateFrom.setContent(objects);
            thisPredicateFrom.setExpanded(false);
            knowledgeFrom.getPanes().add(thisPredicateFrom);
        });
        predicatesFrom.setContent(knowledgeFrom);
        predicatesFrom.setExpanded(false);

        predicatesTo = new TitledPane();
        predicatesTo.setText("Vertices to this node");
        Accordion knowledgeTo = new Accordion();
        propertiesTo = colMd.getTriplesSimpleReversed().get(uri);
        if (propertiesTo != null) {
            propertiesTo.keySet().forEach(property -> {
                TitledPane thisPredicateTo = new TitledPane();
                thisPredicateTo.setText(property.getLocalName());
                VBox subjects = new VBox();
                propertiesTo.get(property).forEach(node -> {
                    BorderPane content = new BorderPane();
                    Label vertex = new Label(colMd.getGraph().shortForm(node.toString()) + " " + colMd.getGraph().shortForm(property.getLocalName()) + " " + colMd.getGraph().shortForm(uri));
                    Button objectDetails = new DetailsButton(node.toString(), textField);
                    content.setLeft(vertex);
                    content.setRight(objectDetails);
                    subjects.getChildren().add(content);
                });
                thisPredicateTo.setContent(subjects);
                thisPredicateTo.setExpanded(false);
                knowledgeTo.getPanes().add(thisPredicateTo);
            });
            predicatesTo.setContent(knowledgeTo);
            predicatesTo.setExpanded(false);
        } else {
            predicatesTo = null;
        }
    }

    private class DetailsButton extends Button {
        private DetailsButton(String uri, TextField filter) {
            super("Set filter to " + uri);
            setOnMouseClicked(mouseEvent -> {
                Platform.runLater(() -> filter.setText(uri));
            });
        }
    }

}
