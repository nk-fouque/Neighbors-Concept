package implementation.gui.controller;

import implementation.Cluster;
import implementation.Partition;
import implementation.gui.model.VisualCluster;
import implementation.utils.CollectionsModel;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.text.Text;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.ReasonerRegistry;

import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class PartitionRun implements Runnable {
    private Model graph;
    private Partition partition;
    private String uriTarget;
    private Accordion resultsContainer;
    private BooleanProperty available;
    private AtomicBoolean cut;

    private NeighborsController mainController;

    public PartitionRun(Model md, String uri, Accordion container, Partition p, BooleanProperty available, AtomicBoolean cut,NeighborsController controller) {
        super();
        graph = md;
        uriTarget = uri;
        resultsContainer = container;
        partition = p;
        this.available = available;
        this.cut = cut;
        mainController = controller;
    }

    @Override
    public void run() {
        available.setValue(false);
        Model saturated = ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), graph);

        CollectionsModel colMd = new CollectionsModel(graph, saturated);

        partition = new Partition(colMd, uriTarget);

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
