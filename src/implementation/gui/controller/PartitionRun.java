package implementation.gui.controller;

import implementation.algorithms.Cluster;
import implementation.algorithms.Partition;
import implementation.gui.model.ObservablePartition;
import implementation.gui.model.VisualCluster;
import implementation.utils.CollectionsModel;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.apache.jena.rdf.model.Model;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runnable for back thread running the Partition Algorithm
 *
 * @author nk-fouque
 */
public class PartitionRun implements Runnable {
    /**
     * RDF Model stored in the controller
     */
    private Model graph;
    /**
     * The (full) uri of the node to apply similarity search
     */
    private String uriTarget;
    /**
     * VBox to put the resulting clusters in
     */
    private VBox resultsContainer;
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

    /**
     * The pane to write loading informations in
     */
    private TitledPane loadingPane;

    /**
     * The depth for the partition
     */
    private int depth;

    private StringProperty sortMode;

    /**
     * Base Constructor
     *
     * @param md         {@link #graph}
     * @param uri        {@link #uriTarget}
     * @param container  {@link #resultsContainer}
     * @param available  {@link #available}
     * @param cut        {@link #cut}
     * @param controller {@link #mainController}
     */
    public PartitionRun(Model md, String uri, VBox container, BooleanProperty available, AtomicBoolean cut, NeighborsController controller, TitledPane loadingPane, Spinner<Integer> descriptionDepth) {
        super();
        graph = md;
        uriTarget = uri;
        resultsContainer = container;
        this.available = available;
        this.cut = cut;
        mainController = controller;
        this.loadingPane = loadingPane;
        depth = descriptionDepth.getValue().intValue();
        sortMode = new SimpleStringProperty();
        sortMode.bind(controller.sortMode.valueProperty());
    }

    @Override
    public void run() {

        available.setValue(false);

        CollectionsModel colMd = mainController.colMd;
        mainController.partition = new ObservablePartition(colMd, uriTarget, depth);
        ObservablePartition partition = mainController.partition;

        Label loadingState = new Label();
        loadingState.textProperty().bind(partition.stateProperty());
        Platform.runLater(() -> mainController.finalState.textProperty().bind(partition.getNbNeighbors()));
        Platform.runLater(() -> loadingPane.setContent(loadingState));
        Platform.runLater(() -> loadingPane.setExpanded(true));

        int algoRun = partition.completePartitioning(cut);
        partition.cut();

        if (algoRun >= 0) {
            Comparator<Cluster> comparator = new ClusterComparator(sortMode.get());

            Platform.runLater(() -> resultsContainer.getChildren().clear());
            PriorityQueue<Cluster> queue = new PriorityQueue<>(comparator);
            queue.addAll(partition.getNeighbors());
            while (!queue.isEmpty()) {
                Cluster c = queue.poll();
                TitledPane cluster = new VisualCluster(c, partition.getGraph(), mainController.filterSubjectsField);
                Platform.runLater(() -> resultsContainer.getChildren().add(cluster));
            }
            Platform.runLater(() -> resultsContainer.autosize());
        } else {
            Platform.runLater(() -> resultsContainer.getChildren().clear());
            TitledPane error = new TitledPane();
            error.setText("Something went wrong :/");
            error.setContent(new Text("Error details in Java Console"));
            Platform.runLater(() -> resultsContainer.getChildren().add(error));
        }
        available.setValue(true);
        mainController.cutDeactivate();
        Thread.currentThread().interrupt();
    }
}
