package implementation;

import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.Element;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import implementation.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class Partition {
    private static Logger logger = Logger.getLogger(Partition.class);
    /**
     * Jena Model containing the RDF Graph in which the search must be done
     */
    private CollectionsModel graph;

    private Model saturatedGraph;
    /**
     * List of clusters that are still partitionable
     */
    private PriorityQueue<Cluster> clusters;

    /**
     * List of clusters that are not partitionable any further and contain at least one answer
     * It's when a cluster is here that it actually represents the actual 'Neighbor concept'
     */
    private List<Cluster> neighbors;

    private Map<String, Var> keys;

    /**
     * Initialize the Partition Structure
     *
     * @param q  Jena Query describing the Node we are trying to find the neighbors of
     * @param md Jena Model containing the RDF Graph in which the search must be done
     */
    public Partition(Query q, Model md, Model mdInf, Map<String, Var> keycodes) {
        graph = new CollectionsModel(md, mdInf);
        saturatedGraph = mdInf;
        clusters = new PriorityQueue<>();
        clusters.add(new Cluster(q, md));
        neighbors = new ArrayList<>();
        keys = keycodes;
    }

    public PriorityQueue<Cluster> getClusters() {
        return clusters;
    }

    public List<Cluster> getNeighbors() {
        return neighbors;
    }

    public CollectionsModel getGraph() {
        return graph;
    }

    /**
     * Applies one iteration of the Partition algorithm
     * @return false if the partitioning is over, true if it can still be iterated
     */
    public boolean iterate() throws PartitionException {
        SingletonStopwatchCollection.resume("iterate");
        SingletonStopwatchCollection.resume("connect");

        Cluster c = clusters.poll();
        Element e = null;
        List<Var> varE = null;
        for (Element element : c.getAvailableQueryElements()) {
            varE = ElementUtils.mentioned(element);
            if (c.connected(varE)) {
                logger.debug(varE + " connected to " + c);
                e = element;
                break;
            } else {
//                logger.debug(varE + " disconnected");
            }
        }
        SingletonStopwatchCollection.stop("connect");
        if (e != null) {
//            logger.debug(e.toString());
            List<Element> list = new ArrayList<>(c.getRelaxQueryElements());
            list.add(e);


            SingletonStopwatchCollection.resume("newans");
            Table ansE = TableUtils.ans(varE, e, graph, c.getMapping());
            SingletonStopwatchCollection.stop("newans");

            SingletonStopwatchCollection.resume("extjoin");
            Table me = TableUtils.simpleJoin(c.getMapping(), ansE);
            SingletonStopwatchCollection.stop("extjoin");

            SingletonStopwatchCollection.resume("projjoin");
            Table piMe = TableUtils.projection(me, c.getProj());
            Table ae = TableUtils.simpleJoin(c.getAnswers(), piMe);
            SingletonStopwatchCollection.stop("projjoin");


            Cluster Ce = new Cluster(c, me, ae);
            Ce.move(e, varE);

            Cluster CeOpp = new Cluster(c, c.getMapping(), TableUtils.difference(c.getAnswers(), ae));
            CeOpp.relax(e, graph, keys);


            SingletonStopwatchCollection.resume("reste");
            boolean ceEmpty = Ce.noAnswers();
            boolean ceOppEmpty = CeOpp.noAnswers();
            if (!ceEmpty) {
                clusters.add(Ce);
                if (Level.DEBUG.isGreaterOrEqual(logger.getLevel())) logger.debug("Ce kept :" + Ce);
            }
//            else logger.debug("Ce eliminated :"+Ce); //TODO Decide whether this is relevant
            if (!ceOppEmpty) {
                clusters.add(CeOpp);
                if (Level.DEBUG.isGreaterOrEqual(logger.getLevel())) logger.debug("CeOpp kept :" + CeOpp);
            }
//            else logger.debug("CeOpp eliminated :"+CeOpp); //TODO Decide whether this is relevant

            logger.info(clusters.size() + ":" + neighbors.size());
//            if (Level.DEBUG.isGreaterOrEqual(logger.getLevel())) logger.debug(clusters);
            SingletonStopwatchCollection.stop("reste");
            SingletonStopwatchCollection.stop("iterate");
            return true;
        } else {
            if (c.getAvailableQueryElements().size() != 0) {
                c.relax(c.getAvailableQueryElements().get(0), graph, keys);
                clusters.add(c);
            } else if (c.getRelaxQueryElements().size() != 0) {
                neighbors.add(c);
            }
            SingletonStopwatchCollection.stop("iterate");
            return clusters.size() != 0;
        }
    }

    /**
     * Applies the Partition algorithm to the end
     * @return true if the Algorithm went fine, false if something went wrong
     */
    public boolean partitionAlgorithm() {
        boolean run = true;
        while (run) {
            try {
                run = iterate();
            } catch (PartitionException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        if (Level.DEBUG.isGreaterOrEqual(logger.getLevel())) {
            res.append("Keys :\n" + keys.toString() + "\n");
        }
        res.append("\t\tClusters :\n");
        PriorityQueue<Cluster> queue = new PriorityQueue<>(neighbors);
        while (!queue.isEmpty()) {
            res.append(queue.poll().toString()).append("\n\n");
        }
        return res.toString();
    }

    /**
     * Creates a String for the query representing the node we are searching the neighbors of
     * If a Query is created from this string and called on the same graph, it should result in the original node
     * @param uri The uri of the element to be represented
     * @param graph The graph to represent it in
     */
    public static String initialQueryString(String uri, Model graph, Map<String, Var> keys) {
        List<Var> x = new ArrayList<>();
        Var neighbor = Var.alloc("Neighbor");
        keys.put(uri, neighbor);
        x.add(neighbor);
        String res = ElementUtils.getSelectStringFrom(x, ElementUtils.describeNode(uri, graph, keys));
        return res;
    }


}
