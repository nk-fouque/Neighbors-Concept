package implementation.gui.model;

import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import org.apache.jena.rdf.model.Model;

public class VisualCandidate extends BorderPane {
    public VisualCandidate(String uri, Model md, NeighborButton button) {
        super();
        Label text = new Label("  " + md.shortForm(uri));
        text.getStyleClass().add("sparklis-blue");
        setLeft(text);
        setRight(button);
    }
}
