package implementation.gui.model;

import implementation.gui.controller.NeighborsController;
import implementation.gui.controller.PartitionRun;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import org.apache.jena.rdf.model.Model;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Button linked to a uri to start the Similarity Search on it
 * <p>
 * TODO : Ultimately, this should just change a parameter somewhere and the partition will be run by something else
 */
public class NeighborButton extends Button {
    /**
     * The (full) uri of the node to apply the Partition algorithm on
     */
    public StringProperty uri;

    public NeighborButton(String uri, Accordion resultsContainer, Model graph, BooleanProperty available, AtomicBoolean cut, NeighborsController controller, int timeLimit, int descriptionDepth) {
        super();
        this.uri = new SimpleStringProperty(uri);
        this.textProperty().setValue("Find neighbors");
        this.visibleProperty().bind(available);
        this.setOnMouseClicked(mouseEvent -> {
            resultsContainer.getPanes().clear();
            TitledPane loading = new TitledPane();
            loading.setText("Loading neighbors for " + this.uri.get() + " please wait");
            resultsContainer.getPanes().add(loading);
            Runnable algo = new PartitionRun(graph, this.uri.get(), resultsContainer, available, cut, controller, loading, descriptionDepth);
            Thread thread = new Thread(algo);
            if (timeLimit > 0) {
                Thread timeOut = timeOut(timeLimit, controller, thread);
                timeOut.start();
            }
            thread.start();
        });
    }

    private Thread timeOut(int timeLimit, NeighborsController controller, Thread thread) {
        Thread res = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(timeLimit * 1000);
                    controller.cutActivate();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        return res;
    }

    public StringProperty getUri() {
        return uri;
    }
}
