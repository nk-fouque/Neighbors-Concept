package implementation.gui.model;

import implementation.algorithms.Partition;
import implementation.gui.controller.NeighborsController;
import implementation.gui.controller.PartitionRun;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import org.apache.jena.rdf.model.Model;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Button linked to a uri to start the Similarity Search on it
 */
public class NeighborButton extends Button {
    /**
     * The (full) uri of the node to apply the Partition algorithm on
     */
    private String uri;

    public NeighborButton(String uri, Accordion resultsContainer, Model graph, Partition partition, BooleanProperty available, AtomicBoolean cut, NeighborsController controller, int timeLimit) {
        super();
        this.uri = uri;
        this.textProperty().setValue("Find neighbors");
        this.visibleProperty().bind(available);
        this.setOnMouseClicked(mouseEvent -> {
            resultsContainer.getPanes().clear();
            TitledPane loading = new TitledPane();
            loading.setText("Loading neighbors for " + uri + " please wait");
            resultsContainer.getPanes().add(loading);
            Runnable algo = new PartitionRun(graph, uri, resultsContainer, partition, available, cut, controller);
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
                    if (!thread.isInterrupted()) {
                        controller.cutActivate();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        return res;
    }

    public String getUri() {
        return uri;
    }
}
