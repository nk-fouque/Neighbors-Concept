package implementation.gui.model;

import implementation.Cluster;
import implementation.Partition;
import implementation.utils.PartitionException;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.sparql.core.Var;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import static implementation.gui.controller.NeighborsController.clusterVisual;

public class PartitionRun implements Runnable {
    private Model graph;
    private Partition partition;
    private String uriTarget;
    private Accordion resultsContainer;
    private BooleanProperty available;
    private BooleanProperty cut;

    public PartitionRun(Model md, String uri, Accordion container, Partition p, BooleanProperty available,BooleanProperty cut){
        super();
        graph=md;
        uriTarget=uri;
        resultsContainer=container;
        partition=p;
        this.available=available;
        this.cut=cut;
    }

    @Override
    public void run() {
        available.setValue(false);
        Model saturated = ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), graph);

        Map<String, Var> keys = new HashMap<>();
        String QueryString = Partition.initialQueryString(uriTarget, graph, keys);

        // Printing the result just to show that we find it back
        Query q = QueryFactory.create(QueryString);
        QueryExecution qe = QueryExecutionFactory.create(q, saturated);
        ResultSetFormatter.out(System.out, qe.execSelect(), q);

        // Creation of the Partition
        partition = new Partition(q, graph, saturated, keys);

        boolean run = !cut.get();

        while (run){
            try {
                run = (!cut.get()) && partition.iterate();
            } catch (PartitionException e) {
                e.printStackTrace();
            } catch (OutOfMemoryError oom){
                run = false;
            }
        }
        partition.cut();

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                resultsContainer.getPanes().clear();
            }
        });
        PriorityQueue<Cluster> queue = new PriorityQueue<>(partition.getNeighbors());
        while(!queue.isEmpty()){
            Cluster c = queue.poll();
            TitledPane cluster = clusterVisual(c);
            cluster.prefWidthProperty().bind(resultsContainer.widthProperty());
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    resultsContainer.getPanes().add(cluster);
                }
            });
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                resultsContainer.autosize();
            }
        });
        available.setValue(true);
    }
}