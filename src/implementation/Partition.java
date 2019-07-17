package implementation;

import implementation.matchTrees.MatchTreeRoot;
import implementation.utils.CollectionsModel;
import implementation.utils.ElementUtils;
import implementation.utils.PartitionException;
import implementation.utils.TableUtils;
import implementation.utils.profiling.stopwatches.SingletonStopwatchCollection;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.Element;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Partition {
    private static Logger logger = Logger.getLogger(Partition.class);
    /**
     * Jena Model containing the RDF Graph in which the search must be done
     */
    private CollectionsModel graph;

    /**
     * List of clusters that are still partitionable
     */
    private ArrayList<Cluster> clusters;

    /**
     * List of clusters that are not partitionable any further and contain at least one answer
     * It's when a cluster is here that it represents the actual 'Neighbor concept'
     */
    private List<Cluster> neighbors;

    private Map<String, Var> keys;

    public Partition(CollectionsModel colMd, String uriTarget) {
        graph = colMd;
        keys = new HashMap<>();

        String querySting = initialQueryString(uriTarget, colMd, keys);
        Query q = QueryFactory.create(querySting);
        clusters = new ArrayList<>();
        clusters.add(new Cluster(q, colMd));
        neighbors = new ArrayList<>();
    }

    public List<Cluster> getClusters() {
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
     *
     * @return false if the partitioning is over, true if it can still be iterated
     */
    public boolean iterate() throws PartitionException, OutOfMemoryError {
        SingletonStopwatchCollection.resume("iterate");
        SingletonStopwatchCollection.resume("connect");

        Cluster c = clusters.remove(0);
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


            MatchTreeRoot me = new MatchTreeRoot(c.getMatchTree());
            SingletonStopwatchCollection.resume("newans");
            try {
                me = me.lazyJoin(e, graph, c.getConnectedVars());
            } catch (OutOfMemoryError err) {
                clusters.add(c);
                SingletonStopwatchCollection.stop("iterate");
                throw err;
            } finally {
                SingletonStopwatchCollection.stop("newans");
            }

            SingletonStopwatchCollection.resume("projjoin");
            Table piMe;
            Table ae;
            try {
                piMe = TableUtils.projection(me.getMatchSet(), c.getProj());
                if (Level.TRACE.isGreaterOrEqual(logger.getLevel())) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ResultSet rs = piMe.toResultSet();
                    ResultSetFormatter.out(baos, rs);
                    logger.trace(baos.toString());
                }
                ae = TableUtils.simpleJoin(c.getAnswers(), piMe);
                if (Level.TRACE.isGreaterOrEqual(logger.getLevel())) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ResultSetFormatter.out(baos, ae.toResultSet());
                    logger.trace(baos.toString());
                }
            } catch (OutOfMemoryError err) {
                clusters.add(c);
                SingletonStopwatchCollection.stop("iterate");
                throw err;
            } finally {
                SingletonStopwatchCollection.stop("projjoin");
            }
            int extensionDistance = piMe.size();


            SingletonStopwatchCollection.resume("reste");
            Cluster Ce = new Cluster(c, me, ae, extensionDistance);
            Ce.move(e, varE);

            Cluster CeOpp = new Cluster(c, c.getMatchTree(), TableUtils.difference(c.getAnswers(), ae), c.getExtensionDistance());
            CeOpp.relax(e, graph, keys);


            boolean ceEmpty = Ce.noAnswers();
            boolean ceOppEmpty = CeOpp.noAnswers();
            if (!ceEmpty) {
                clusters.add(Ce);
                if (Level.TRACE.isGreaterOrEqual(logger.getLevel())) logger.trace("Ce kept :" + Ce);
            }
//            else logger.trace("Ce eliminated :"+Ce);
            if (!ceOppEmpty) {
                clusters.add(CeOpp);
                if (Level.TRACE.isGreaterOrEqual(logger.getLevel())) logger.trace("CeOpp kept :" + CeOpp);
            }
//            else logger.trace("CeOpp eliminated :"+CeOpp);

            logger.info(clusters.size() + ":" + neighbors.size() + " - " + c.getRelaxDistance());
//            if (Level.TRACE.isGreaterOrEqual(logger.getLevel())) logger.trace(clusters);
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
     *
     * @param cut AtomicBoolean to observe, when it is set to false, the algorithm stops and cuts
     * @return 0 if the algorithm went to the end correctly, 1 if the algorithm encountered a memory limit, -1 if it encountered an unexpected error
     */
    public int partitionAlgorithm(AtomicBoolean cut) {
        boolean run = true;
        boolean stop = false;
        while (run && !stop) {
            try {
                run = iterate();
            } catch (OutOfMemoryError mem) {
                mem.printStackTrace();
                return 1;
            } catch (PartitionException e) {
                e.printStackTrace();
                return -1;
            }
            if (cut != null) {
                stop = cut.get();
            }
        }
        if (cut.get()) return 2;
        else return 0;
    }

    public void cut() {
        this.neighbors.addAll(clusters);
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        if (Level.DEBUG.isGreaterOrEqual(logger.getLevel())) {
            res.append("Keys :\n").append(keys.toString()).append("\n");
        }
        res.append("\t\tClusters :\n");
        PriorityQueue<Cluster> queue = new PriorityQueue<>(neighbors);
        while (!queue.isEmpty()) {
            res.append(queue.poll().toString(graph)).append("\n\n");
        }
        return res.toString();
    }

    public String initialQueryString(String uri, CollectionsModel colMd, Map<String, Var> keys) {
        Var neighbor = Var.alloc("Neighbor");
        keys.put(uri, neighbor);

        List<Var> x = new ArrayList<>();
        x.add(neighbor);

        String res = ElementUtils.getSelectStringFrom(x, ElementUtils.describeNode(uri, colMd, keys));
        return res;
    }


}
