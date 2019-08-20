package implementation.gui.model;

import implementation.algorithms.Partition;
import implementation.utils.CollectionsModel;
import implementation.utils.PartitionException;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Partition with some observable properties for the interface
 *
 * @author nk-fouque
 */
public class ObservablePartition extends Partition {

    private int clusterSize;
    private int answerSize;
    StringProperty nbNeighbors;
    StringProperty state;

    /**
     * @param colMd     A preexisting CollectionsModel in which to describe the node and search for its neighbors
     * @param uriTarget The full length uri of the node to describe
     */
    public ObservablePartition(CollectionsModel colMd, String uriTarget, int descriptionDepth) {
        super(colMd, uriTarget, descriptionDepth);
        clusterSize = 1;
        state = new SimpleStringProperty();
        nbNeighbors = new SimpleStringProperty();
    }

    @Override
    public boolean oneStepPartitioning() throws PartitionException, OutOfMemoryError {
        boolean res = super.oneStepPartitioning();
        if ((getClusters().size() != clusterSize) || (getNeighbors().size() != answerSize)) {
            clusterSize = getClusters().size();
            answerSize = getNeighbors().size();
            int totalSize = clusterSize + answerSize;
            Platform.runLater(() ->
                    state.setValue(totalSize + " clusters found: " + clusterSize + " processing, " + answerSize + " ready.")
            );
        }
        if (!res) {
            Platform.runLater(() ->
                    nbNeighbors.setValue(answerSize + " Clusters found")
            );
        }
        return res;
    }


    public StringProperty stateProperty() {
        return state;
    }

    public StringProperty getNbNeighbors() {
        return nbNeighbors;
    }
}
