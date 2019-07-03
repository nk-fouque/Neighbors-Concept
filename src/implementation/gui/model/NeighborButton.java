package implementation.gui.model;

import implementation.Partition;
import javafx.beans.property.BooleanProperty;
import javafx.event.EventHandler;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseEvent;
import org.apache.jena.rdf.model.Model;

public class NeighborButton extends Button {
    private String uri;

    public NeighborButton(String uri, Accordion resultsContainer, Model md, Partition part, BooleanProperty available, BooleanProperty cut) {
        super();
        this.uri = uri;
        this.textProperty().setValue("Find neighbors");
        this.visibleProperty().bind(available);
        this.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                resultsContainer.getPanes().clear();
                TitledPane loading = new TitledPane();
                loading.setText("Loading neighbors for " + uri + " please wait");
                resultsContainer.getPanes().add(loading);
                Runnable algo = new PartitionRun(md, uri, resultsContainer, part, available, cut);
                Thread thread = new Thread(algo);
                thread.start();
            }
        });
    }

    public String getUri() {
        return uri;
    }
}
