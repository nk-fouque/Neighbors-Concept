package implementation.gui.controller;

import implementation.algorithms.Cluster;
import implementation.gui.model.VisualCluster;
import implementation.gui.model.VisualError;
import implementation.utils.PartitionException;
import javafx.application.Platform;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PartitionAdditional implements Runnable {

    NeighborsController mainController;
    Collection<Cluster> clusters;
    VBox resultsContainer;
    AtomicBoolean cut;

    public PartitionAdditional(NeighborsController controller, Collection<Cluster> clusters, VBox container, AtomicBoolean cut){
        this.mainController = controller;
        this.clusters = clusters;
        resultsContainer = container;
        this.cut = cut;
    }

    @Override
    public void run() {
        mainController.partitionAvailable.setValue(false);

        Platform.runLater(() -> resultsContainer.getChildren().clear());

        int algoRun = -2;
        try {
            algoRun = mainController.partition.targetedFurtherPartitioning(clusters,cut);
            mainController.partition.cut();
        } catch (PartitionException e) {
            Platform.runLater(() -> resultsContainer.getChildren().clear());
            Platform.runLater(() -> resultsContainer.getChildren().add(VisualError.standardError(e)));
        }

        if (algoRun >= 0) {
            Platform.runLater(() -> resultsContainer.getChildren().clear());

            Comparator<Cluster> clusterComparator = new ClusterComparator(mainController.sortMode.getValue());
            PriorityQueue<Cluster> queue = new PriorityQueue<>(clusterComparator);
            queue.addAll(mainController.partition.getNeighbors());
            while (!queue.isEmpty()) {
                Cluster c = queue.poll();
                TitledPane cluster = new VisualCluster(c, mainController.partition.getGraph(), mainController.filterSubjectsField, mainController);
                Platform.runLater(() -> resultsContainer.getChildren().add(cluster));
            }
            Platform.runLater(() -> resultsContainer.autosize());
        } else {
            Platform.runLater(() -> resultsContainer.getChildren().clear());
            Platform.runLater(() -> resultsContainer.getChildren().add(VisualError.partitionError()));
        }
        mainController.partitionAvailable.setValue(true);
        mainController.cutDeactivate();
        Thread.currentThread().interrupt();
    }
}
