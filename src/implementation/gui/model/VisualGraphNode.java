package implementation.gui.model;

import implementation.utils.CollectionsModel;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;

import java.util.List;
import java.util.Map;

public class VisualGraphNode {

    TitledPane predicatesFrom;
    TitledPane predicatesTo;
    GridPane dbPrompt;

//    public VisualGraphNode(String uri, CollectionsModel colMd, TextField textField) {
//        Map<Property, List<RDFNode>> propertiesFrom;
//        Map<Property, List<RDFNode>> propertiesTo;
//
//        predicatesFrom = new TitledPane();
//        predicatesFrom.setText("Vertices from this node");
//        Accordion knowledgeFrom = new Accordion();
//        propertiesFrom = colMd.getTriplesSimple().get(uri);
//        propertiesFrom.keySet().forEach(property -> {
//            TitledPane thisPredicateFrom = new TitledPane();
//            thisPredicateFrom.setText(property.getLocalName());
//            VBox objects = new VBox();
//            propertiesFrom.get(property).forEach(node -> {
//                BorderPane content = new BorderPane();
//                Label vertex = new Label(colMd.getGraph().shortForm(uri) + " " + colMd.getGraph().shortForm(property.getLocalName()) + " " + colMd.getGraph().shortForm(node.toString()));
//                Button objectDetails = new SubjectLink(node.toString(), textField);
//                content.setLeft(vertex);
//                content.setRight(objectDetails);
//                objects.getChildren().add(content);
//            });
//            thisPredicateFrom.setContent(objects);
//            thisPredicateFrom.setExpanded(false);
//            knowledgeFrom.getPanes().add(thisPredicateFrom);
//        });
//        predicatesFrom.setContent(knowledgeFrom);
//        predicatesFrom.setExpanded(false);
//
//        predicatesTo = new TitledPane();
//        predicatesTo.setText("Vertices to this node");
//        Accordion knowledgeTo = new Accordion();
//        propertiesTo = colMd.getTriplesSimpleReversed().get(uri);
//        if (propertiesTo != null) {
//            propertiesTo.keySet().forEach(property -> {
//                TitledPane thisPredicateTo = new TitledPane();
//                thisPredicateTo.setText(property.getLocalName());
//                VBox subjects = new VBox();
//                propertiesTo.get(property).forEach(node -> {
//                    BorderPane content = new BorderPane();
//                    Label vertex = new Label(colMd.getGraph().shortForm(node.toString()) + " " + colMd.getGraph().shortForm(property.getLocalName()) + " " + colMd.getGraph().shortForm(uri));
//                    Button objectDetails = new SubjectLink(node.toString(), textField);
//                    content.setLeft(vertex);
//                    content.setRight(objectDetails);
//                    subjects.getChildren().add(content);
//                });
//                thisPredicateTo.setContent(subjects);
//                thisPredicateTo.setExpanded(false);
//                knowledgeTo.getPanes().add(thisPredicateTo);
//            });
//            predicatesTo.setContent(knowledgeTo);
//            predicatesTo.setExpanded(false);
//        } else {
//            predicatesTo = null;
//        }
//    }

    public VisualGraphNode(String uri, CollectionsModel colMd, TextField textField) {
        dbPrompt = new GridPane();
        int propertiesRow = 0;
        Map<Property, List<RDFNode>> propertiesFrom = colMd.getTriplesSimple().get(uri);
        for (Property property : propertiesFrom.keySet()) {
            dbPrompt.add(new Label("\t" + colMd.getGraph().shortForm(property.getURI())), 0, propertiesRow);
            for (RDFNode node : propertiesFrom.get(property)) {
                Node object;
                if (node.isURIResource()) {
                    object = new SubjectLink(colMd.getGraph().shortForm(node.toString()), textField);
                } else {
                    object = new Label(" " + node.toString());
                }
                dbPrompt.add(object, 1, propertiesRow);
                propertiesRow++;
            }
        }

        Map<Property, List<RDFNode>> propertiesTo = colMd.getTriplesSimpleReversed().get(uri);
        if (propertiesTo != null) {
            for (Property property : propertiesTo.keySet()) {
                dbPrompt.add(new Label("\tis " + colMd.getGraph().shortForm(property.getURI()) + " of"), 0, propertiesRow);
                for (RDFNode node : propertiesTo.get(property)) {
                    Node subjectClickable = new SubjectLink(colMd.getGraph().shortForm(node.toString()), textField);
                    dbPrompt.add(subjectClickable, 1, propertiesRow);
                    propertiesRow++;
                }
            }
        }
        ColumnConstraints constraintLeft = new ColumnConstraints();
        constraintLeft.setHalignment(HPos.RIGHT);
        constraintLeft.setPercentWidth(50);
        ColumnConstraints constraintRight = new ColumnConstraints();
        constraintRight.setPercentWidth(50);
        dbPrompt.getColumnConstraints().add(constraintLeft);
    }

}
