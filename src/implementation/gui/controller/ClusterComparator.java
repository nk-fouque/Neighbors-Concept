package implementation.gui.controller;

import implementation.algorithms.Cluster;

import java.util.Comparator;

public class ClusterComparator implements Comparator<Cluster>{
    private final String mode;

    public ClusterComparator(String mode){
        this.mode = mode;
    }

    @Override
    public int compare(Cluster o1, Cluster o2) {
        Comparator<Cluster> comparator;
        switch (mode){
            case "Number of Relaxations" : {
                comparator = Comparator.comparingInt(Cluster::getRelaxDistance)
                        .thenComparingInt(Cluster::getExtensionDistance)
                        .thenComparingInt(cluster -> (-1 * cluster.getRelaxQueryElements().size()));
                break;
            }
            case "Extensional Distance" : {
                comparator = Comparator.comparingInt(Cluster::getExtensionDistance)
                        .thenComparingInt(Cluster::getRelaxDistance)
                        .thenComparingInt(cluster -> (-1 * cluster.getRelaxQueryElements().size()));
                break;
            }
            default: comparator = Cluster::compareTo;
        }
        return comparator.compare(o1,o2);
    }
}
