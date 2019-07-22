package implementation.gui.model;

import implementation.algorithms.Partition;
import implementation.utils.CollectionsModel;
import implementation.utils.PartitionException;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ObservablePartition extends Partition {

    private int nbClusters;
    StringProperty state;

    /**
     * @param colMd     A preexisting CollectionsModel in which to describe the node and search for its neighbors
     * @param uriTarget The full length uri of the node to describe
     */
    public ObservablePartition(CollectionsModel colMd, String uriTarget, int descriptionDepth) {
        super(colMd, uriTarget,descriptionDepth);
        nbClusters = 1;
        state = new SimpleStringProperty();
    }

    @Override
    public boolean iterate() throws PartitionException, OutOfMemoryError {
        boolean res = super.iterate();
        int size = getClusters().size();
        if (size != nbClusters) {
            nbClusters = getClusters().size();
            Platform.runLater(() ->
                    state.setValue(nbClusters + " clusters found")
            );
        }
        return res;
    }


    public StringProperty stateProperty() {
        return state;
    }
}
