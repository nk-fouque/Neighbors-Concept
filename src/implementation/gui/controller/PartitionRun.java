package implementation.gui.controller;

import implementation.algorithms.Cluster;
import implementation.gui.model.ObservablePartition;
import implementation.gui.model.VisualCluster;
import implementation.utils.CollectionsModel;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.text.Text;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.ReasonerRegistry;

import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runnable for back thread running the Partition Algorithm
 */
public class PartitionRun implements Runnable {
    /**
     * RDF Model stored in the controller
     */
    private Model graph;
    /**
     * The Partition stored by the controller
     */
    private ObservablePartition partition;
    /**
     * The (full) uri of the node to apply similarity search
     */
    private String uriTarget;
    /**
     * Accordion to put the resulting clusters in
     */
    private Accordion resultsContainer;
    /**
     * Boolean property to indicate that no other partition should be run at the same time
     */
    private BooleanProperty available;
    /**
     * Atomic Boolean to be watched by the algorithm for anytime function
     */
    private AtomicBoolean cut;
    /**
     * The controller the algorithm has been called from
     */
    private NeighborsController mainController;

    private TitledPane loadingPane;

    /**
     * Base Constructor
     *
     * @param md         @see {@link #graph}
     * @param uri        @see {@link #uriTarget}
     * @param container  @see {@link #resultsContainer}
     * @param available  @see {@link #available}
     * @param cut        @see {@link #cut}
     * @param controller @see {@link #mainController}
     */
    public PartitionRun(Model md, String uri, Accordion container, BooleanProperty available, AtomicBoolean cut, NeighborsController controller, TitledPane loadingPane) {
        super();
        graph = md;
        uriTarget = uri;
        resultsContainer = container;
        this.available = available;
        this.cut = cut;
        mainController = controller;
        this.loadingPane = loadingPane;
    }

    @Override
    public void run() {
        available.setValue(false);
        Model saturated = ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), graph);

        CollectionsModel colMd = new CollectionsModel(graph, saturated);

        partition = new ObservablePartition(colMd, uriTarget);

        Label loadingState = new Label();
        loadingState.textProperty().bind(partition.stateProperty());
        Platform.runLater(() -> loadingPane.setContent(loadingState));
        Platform.runLater(() -> loadingPane.setExpanded(true));

        int algoRun = partition.partitionAlgorithm(cut);
        partition.cut();

        if (algoRun >= 0) {
            Platform.runLater(() -> resultsContainer.getPanes().clear());
            PriorityQueue<Cluster> queue = new PriorityQueue<>(partition.getNeighbors());
            while (!queue.isEmpty()) {
                Cluster c = queue.poll();
                TitledPane cluster = new VisualCluster(c, partition.getGraph());
                Platform.runLater(() -> resultsContainer.getPanes().add(cluster));
            }
            Platform.runLater(() -> resultsContainer.autosize());
        } else {
            Platform.runLater(() -> resultsContainer.getPanes().clear());
            TitledPane error = new TitledPane();
            error.setText("Something went wrong :/");
            error.setContent(new Text("Error details in Java Console"));
            Platform.runLater(() -> resultsContainer.getPanes().add(error));
        }
        available.setValue(true);
        mainController.cutDeactivate();
        Thread.currentThread().interrupt();
    }
}
