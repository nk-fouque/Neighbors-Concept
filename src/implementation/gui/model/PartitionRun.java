package implementation.gui.model;

import implementation.Cluster;
import implementation.Partition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.text.Text;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.sparql.core.Var;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static implementation.gui.controller.NeighborsController.clusterVisual;

public class PartitionRun implements Runnable {
    private Model graph;
    private Partition partition;
    private String uriTarget;
    private Accordion resultsContainer;
    private BooleanProperty available;
    private AtomicBoolean cut;

    public PartitionRun(Model md, String uri, Accordion container, Partition p, BooleanProperty available, AtomicBoolean cut) {
        super();
        graph = md;
        uriTarget = uri;
        resultsContainer = container;
        partition = p;
        this.available = available;
        this.cut = cut;
    }

    @Override
    public void run() {
        available.setValue(false);
        Model saturated = ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), graph);

        Map<String, Var> keys = new HashMap<>();
        String QueryString = Partition.initialQueryString(uriTarget, graph, keys);

        Query q = QueryFactory.create(QueryString);
        QueryExecution qe = QueryExecutionFactory.create(q, saturated);
        ResultSetFormatter.out(System.out, qe.execSelect(), q);

        partition = new Partition(q, graph, saturated, keys);

        int algoRun = partition.partitionAlgorithm(cut);
        partition.cut();

        if (algoRun>=0) {
            Platform.runLater(() -> resultsContainer.getPanes().clear());
            PriorityQueue<Cluster> queue = new PriorityQueue<>(partition.getNeighbors());
            while (!queue.isEmpty()) {
                Cluster c = queue.poll();
                TitledPane cluster = clusterVisual(c);
                cluster.prefWidthProperty().bind(resultsContainer.widthProperty());
                Platform.runLater(() -> resultsContainer.getPanes().add(cluster));
            }
            Platform.runLater(() -> resultsContainer.autosize());

        } else {
            Platform.runLater(() -> resultsContainer.getPanes().clear());
            TitledPane error = new TitledPane();
            error.setText("Something went wrong :/");
            Platform.runLater(() -> resultsContainer.getPanes().add(error));
        }
        available.setValue(true);
        Thread.currentThread().interrupt();
    }
}
