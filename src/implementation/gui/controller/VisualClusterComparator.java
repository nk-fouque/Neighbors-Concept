package implementation.gui.controller;

import implementation.algorithms.Cluster;
import implementation.gui.model.VisualCluster;

import java.util.Comparator;

public class VisualClusterComparator implements Comparator<VisualCluster> {
    private final String mode;

    public VisualClusterComparator(String mode){
        this.mode = mode;
    }

    @Override
    public int compare(VisualCluster o1, VisualCluster o2) {
        Comparator<Cluster> comparator = new ClusterComparator(mode);
        return comparator.compare(o1.cluster,o2.cluster);
    }
}
