package implementation.gui.model;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.TitledPane;
import javafx.scene.text.Text;

import java.util.Map;

/**
 * TitledPane Summarizing the PrefixMapping
 *
 * @author nk-fouque
 */
public class VisualPrefixes extends TitledPane {

    public VisualPrefixes(Map<String, String> prefixes, BooleanProperty visible) {
        super();
        this.visibleProperty().bind(visible);
        this.setText("Prefixes Used");
        this.setContent(new Text(mappingFormat(prefixes)));
        this.setExpanded(false);
    }

    private String mappingFormat(Map<String, String> prefixes) {
        StringBuilder res = new StringBuilder("\n");
        prefixes.keySet().forEach(key ->
                res
                        .append(key)
                        .append(": = ")
                        .append(prefixes.get(key))
                        .append("\n"));
        return res.toString();
    }
}
