package implementation.gui.model;

import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import org.apache.jena.rdf.model.Model;

/**
 * Pane visualizing a "candidate for partition" and the button to
 */
public class VisualCandidate extends BorderPane {
    public VisualCandidate(String uri, Model md, NeighborButton button) {
        super();
        Label text = new Label("  " + md.shortForm(uri));
        setLeft(text);
        setRight(button);
    }
}
