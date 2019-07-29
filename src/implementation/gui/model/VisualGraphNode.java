package implementation.gui.model;

import implementation.utils.CollectionsModel;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;

import java.util.List;
import java.util.Map;

public class VisualGraphNode {

    GridPane dbPrompt;

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
        dbPrompt.getColumnConstraints().add(constraintLeft);
    }

}
