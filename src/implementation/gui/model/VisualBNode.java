package implementation.gui.model;

import implementation.utils.CollectionsModel;
import javafx.scene.control.TextField;

public class VisualBNode extends VisualCandidate {
    /**
     * Base constructor
     *
     * @param uri               The uri of the node to be visualized
     * @param colMd             The Model in which the node is to be described
     * @param selectedNodeField The field to update when the node is selected
     * @param filterField       The field to update with the filters for navigation
     */
    public VisualBNode(String uri, CollectionsModel colMd, TextField selectedNodeField, TextField filterField) {
        super(uri, colMd, selectedNodeField, filterField);
        topPane.setRight(null);
    }
}
