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

/**
 * Visualization for the details of a node
 *
 * @author nk-fouque
 */
public class VisualGraphNode {
    GridPane dbPrompt;

    /**
     * Base constructor
     * @param uri the uri of the node to describe
     * @param colMd The model in which to describe
     * @param textField the filter textfield to use for navigation
     */
    public VisualGraphNode(String uri, CollectionsModel colMd, TextField textField) {
        dbPrompt = new GridPane();
        int propertiesRow = 0;
        Map<Property, List<RDFNode>> propertiesFrom = colMd.getTriplesSimple().get(uri);
        for (Property property : propertiesFrom.keySet()) {
            dbPrompt.add(new Label("\t" + colMd.shortform(property.getURI())), 0, propertiesRow);
            for (RDFNode node : propertiesFrom.get(property)) {
                Node object;
                if (node.isURIResource()) {
                    object = new SubjectLink(colMd.shortform(node.toString()), textField);
                } else if (node.isAnon()) {
                    object = new BlankNodeLink(node.toString(),textField);
                } else {
                    object = new Label(" " + colMd.shortform(node.toString()));
                }
                dbPrompt.add(object, 1, propertiesRow);
                propertiesRow++;
            }
        }

        Map<Property, List<RDFNode>> propertiesTo = colMd.getTriplesSimpleReversed().get(uri);
        if (propertiesTo != null) {
            for (Property property : propertiesTo.keySet()) {
                dbPrompt.add(new Label("\tis " + colMd.shortform(property.getURI()) + " of"), 0, propertiesRow);
                for (RDFNode node : propertiesTo.get(property)) {
                        SubjectLink subjectClickable = new SubjectLink(colMd.shortform(node.toString()), textField);
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
