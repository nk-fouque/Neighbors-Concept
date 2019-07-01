package implementation.gui.model;

import implementation.Cluster;
import implementation.Partition;
import javafx.event.EventHandler;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.sparql.core.Var;

import java.util.HashMap;
import java.util.Map;

import static implementation.gui.controller.NeighborsController.clusterVisual;

public class NeighborButton extends Button {
    private String uri;
    private Partition partition;
    private Model graph;

    public NeighborButton(String uri, Accordion resultsContainer,Model md,Partition part){
        super();
        this.uri = uri;
        this.textProperty().setValue("Find neighbors of : "+getUri());
        this.graph = md;
        this.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                resultsContainer.getPanes().clear();
                Model saturated = ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), graph);

                Map<String, Var> keys = new HashMap<>();
                String QueryString = Partition.initialQueryString(getUri(), graph, keys);

                // Printing the result just to show that we find it back
                Query q = QueryFactory.create(QueryString);
                QueryExecution qe = QueryExecutionFactory.create(q, saturated);
                ResultSetFormatter.out(System.out, qe.execSelect(), q);

                // Creation of the Partition
                partition = new Partition(q, graph, saturated, keys);
                partition.partitionAlgorithm();
                for(Cluster c : partition.getNeighbors()){
                    resultsContainer.getPanes().add(clusterVisual(c));
                }
                resultsContainer.autosize();
            }
        });
    }

    public String getUri() {
        return uri;
    }
}
