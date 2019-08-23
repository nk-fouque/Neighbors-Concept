package implementation.algorithms;

import implementation.algorithms.matchTree.MatchTreeRoot;
import implementation.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.E_Equals;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author nk-fouque
 */
public class Cluster extends JSONable implements Comparable<Cluster> {
    private static Logger logger = Logger.getLogger(Cluster.class);
    private Set<Var> proj;
    private Set<Element> relaxQueryElements;
    private int relaxDistance;
    private Set<Element> availableQueryElements;
    private MatchTreeRoot mapping;
    private Table answers;
    private Set<Var> connectedVars;
    private Set<Element> removedQueryElements;
    private int extensionDistance;
    private int id;

    /**
     * The head of the query
     * Normally only one variable but represented as a list for theoretical use as a query head
     */
    public Set<Var> getProj() {
        return proj;
    }

    /**
     * The Variables that are connected to this Cluster
     */
    public Set<Var> getConnectedVars() {
        return connectedVars;
    }

    /**
     * The body of the relaxed Query
     */
    public Set<Element> getRelaxQueryElements() {
        return relaxQueryElements;
    }

    /**
     * The query elements that have yet to be tested in this cluster
     */
    public Set<Element> getAvailableQueryElements() {
        return availableQueryElements;
    }

    /**
     * Query elements that have been tested and thrown away/relaxed, to avoid putting them twice
     */
    public Set<Element> getRemovedQueryElements() {
        return removedQueryElements;
    }

    /**
     * The number of relaxations done to obtain this cluster
     */
    public int getRelaxDistance() {
        return relaxDistance;
    }

    /**
     * The size of the extension of this Cluster's query (i.e. the number of answers
     */
    public int getExtensionDistance() {
        return extensionDistance;
    }

    /**
     * The Match-set containing all the answers to the relaxed query
     */
    public Table getMatchSet() {
        return mapping.getMatchSet();
    }

    /**
     * The list containing all answers held by this cluster i.e. not in any cluster with a lower relaxation distance
     */
    public Table getAnswers() {
        return answers;
    }

    /**
     * This Cluster's Match-Tree
     *
     * @see implementation.algorithms.matchTree.MatchTreeNode
     */
    public MatchTreeRoot getMatchTree() {
        return mapping;
    }

    /**
     * Creates the initial Cluster for the Partition Algorithm from the Query qry and the RDF Graph graph
     */
    Cluster(Set<Element> elements, Set<Var> vars, CollectionsModel graph, int id) {
        this.id = id;
        this.proj = new HashSet<>(vars);
        this.relaxQueryElements = new HashSet<>();
        this.relaxDistance = 0;
        this.availableQueryElements = new HashSet<>(elements);
        this.removedQueryElements = new HashSet<>();
        mapping = new MatchTreeRoot(getProj(), graph);
        answers = mapping.getMatchSet();
        extensionDistance = answers.size();
        this.connectedVars = new HashSet<>(vars);
    }

    /**
     * Creates a cluster with the same values as an other but different Mapping ext Answers
     */
    Cluster(Cluster c, MatchTreeRoot Me, Table Ae, int extensionDistance, int id) {
        this.proj = new HashSet<>(c.getProj());
        this.relaxQueryElements = new HashSet<>();
        this.relaxQueryElements.addAll(c.getRelaxQueryElements());
        this.relaxDistance = c.relaxDistance;
        this.availableQueryElements = new HashSet<>();
        this.availableQueryElements.addAll(c.getAvailableQueryElements());
        this.removedQueryElements = new HashSet<>();
        this.removedQueryElements.addAll(c.getRemovedQueryElements());
        this.mapping = Me;
        this.answers = Ae;
        this.extensionDistance = extensionDistance;
        this.connectedVars = new HashSet<>(c.connectedVars);
        this.id = id;
    }

    /**
     * @return the difference between this Cluster's relax distance and the other Cluster
     */
    public int compareTo(Cluster other) {
        int res = (getExtensionDistance() - other.getExtensionDistance());
        if (res == 0) res = getRelaxDistance() - other.getRelaxDistance();
        return res;
    }

    /**
     * @param vars A list of Variables
     * @return true if this cluster is connected to an element mentioning these variables
     */
    public boolean connected(Set<Var> vars) {
        boolean connect = false;
        for (Var v : vars) {
            if (this.connectedVars.contains(v)) {
                connect = true;
                break;
            }
        }
        return connect;
    }

    /**
     * Substract an element from the available query elements to mark it as used and adds it to the elements distinctive of this Cluster's neighbors
     *
     * @param element The Element to be moved
     * @param vars    The Jena Variables mentioned to the element
     */
    public void move(Element element, Set<Var> vars) throws PartitionException {
        boolean removed = availableQueryElements.remove(element);
        if (!removed) {
            throw new PartitionException("Could not move :" + element.toString());
        } else {
            relaxQueryElements.add(element);
            connectedVars.addAll(vars);
        }
    }

    /**
     * @return true if this cluster has nothing in its answers
     */
    boolean noAnswers() {
        return (this.getAnswers().size() == 0);
    }

    /**
     * Substracts an element from the available query elements to mark it as used and increases the relax distance of this Cluster by one
     *
     * @param element The element to relax
     * @throws PartitionException
     */
    public void relax(Element element, CollectionsModel graph, int descriptionDepth) throws PartitionException {
        boolean removed = availableQueryElements.remove(element);
        if (!removed) {
            throw new PartitionException("Could not relax " + element.toString() + " : Element not found when trying to remove");
        } else {
            Set<Element> list = new HashSet<>();
            if (element instanceof ElementFilter) {
                logger.debug(element + " is filter");
                ElementFilter e = (ElementFilter) element;
                if (descriptionDepth > 1) {

                    list = ElementUtils.relaxFilter(e, graph, descriptionDepth);
                }

            } else {
                ElementPathBlock e = (ElementPathBlock) element;
                TriplePath t = e.getPattern().get(0);
                if (t.getPredicate().equals(RDF.type.asNode())) {
                    logger.debug(element + " is class");
                    list = ElementUtils.relaxClass(t, graph);
                } else {
                    logger.debug(element + " is triple pattern");
                    list = ElementUtils.relaxProperty(t, graph);
                }
            }
            list.removeAll(relaxQueryElements);
            list.removeAll(removedQueryElements);
            availableQueryElements.addAll(list);
            removedQueryElements.add(element);
            relaxDistance++;
        }
    }

    /**
     * @return The string for the relaxed Query corresponding to this Cluster
     */
    public String queryString() {
        return ElementUtils.getSelectStringFrom(this.proj, relaxQueryElements);
    }

    /**
     * @return the answers as a string formatted by Jena
     */
    public String answersString() {
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        ResultSetFormatter.out(baos2, getAnswers().toResultSet());
        return baos2.toString();
    }

    @Override
    public String toString() {
        String res = "Cluster n°" + id + "\n";
        res += "Extentional Distance : " + relaxDistance + "\n";
        res += "Elements : " + relaxQueryElements + "\n";
        if (Level.DEBUG.isGreaterOrEqual(logger.getLevel())) {
            res += "Debug available: " + availableQueryElements + "\n";
            res += "Debug removed : " + removedQueryElements + "\n";
            res += "Connected variables : " + connectedVars + "\n";
        }
        res += "Query :" + queryString() + "\n";
        if (Level.TRACE.isGreaterOrEqual(logger.getLevel())) res += "Mapping :\n" + mapping.toString() + "\n";
        res += "Answers :\n" + answersString() + "\n";
        return res;
    }

    /**
     * Same as {@link #toString()} but uses {@link #answersListString(CollectionsModel)} to write answers
     * More efficient because it doesn't use a ResultSet
     *
     * @param colMd the model to use for prefixes
     */
    public String toString(CollectionsModel colMd) {
        String res = "Cluster n°" + id + "\n";
        res += "Number of relaxation : " + relaxDistance + "\n";
        res += "Extensional Distance : " + extensionDistance + "\n";
        res += "Elements : " + relaxQueryElements + "\n";
        if (Level.DEBUG.isGreaterOrEqual(logger.getLevel())) {
            res += "Debug available: " + availableQueryElements + "\n";
            res += "Debug removed : " + removedQueryElements + "\n";
            res += "Connected variables : " + connectedVars + "\n";
            res += "Query :" + queryString() + "\n";
        }
        if (Level.DEBUG.isGreaterOrEqual(logger.getLevel())) res += "Mapping :\n" + mapping.toString() + "\n";
        res += "Answers :\n" + answersListString(colMd) + "\n";
        return res;
    }

    @Override
    public String toJson() {
        String res = "{\n";
        res += "\"id\":" + id + ",\n";
        res += "\"number_of_relaxation\":" + relaxDistance + ",\n";
        res += "\"extensional Distance\":" + extensionDistance + ",\n";
        res += "\"elements\":" + SetUtils.toSimpleJson(relaxQueryElements) + ",\n";
        if (Level.DEBUG.isGreaterOrEqual(logger.getLevel())) {
            res += "\"elements_available\":" + SetUtils.toSimpleJson(availableQueryElements) + ",\n";
            res += "\"elements_removed\":" + SetUtils.toSimpleJson(removedQueryElements) + ",\n";
            res += "\"connected_variables\": " + SetUtils.toSimpleJson(connectedVars) + ",\n";
        }
        if (Level.DEBUG.isGreaterOrEqual(logger.getLevel())) res += "\"match_tree\" :\n" + mapping.toJson() + ",\n";
        res += "\"answers\":" + SetUtils.toSimpleJson(new HashSet(getAnswersList()));
        res += "\n}";
        return res;
    }

    public String toJson(CollectionsModel colMd) {
        String res = "{\n";
        res += "\"id\":" + id + ",\n";
        res += "\"number_of_relaxation\":" + relaxDistance + ",\n";
        res += "\"extensional Distance\":" + extensionDistance + ",\n";
        res += "\"elements\":" + SetUtils.toSimpleJson(relaxQueryElements) + ",\n";
        if (Level.DEBUG.isGreaterOrEqual(logger.getLevel())) {
            res += "\"elements_available\":" + SetUtils.toSimpleJson(availableQueryElements) + ",\n";
            res += "\"elements_removed\":" + SetUtils.toSimpleJson(removedQueryElements) + ",\n";
            res += "\"connected_variables\": " + SetUtils.toSimpleJson(connectedVars) + ",\n";
        }
        if (Level.DEBUG.isGreaterOrEqual(logger.getLevel())) res += "\"match_tree\" :\n" + mapping.toJson() + ",\n";
        res += "\"answers\":" + answersListJson(colMd);
        res += "\n}";
        return res;
    }

    /**
     * @return This cluster's answers as a List of RDF Nodes
     */
    public List<Node> getAnswersList() {
        Iterator<Binding> iter = answers.rows();
        List<Node> res = new ArrayList<>();
        iter.forEachRemaining((Binding b) -> res.add(b.get(Var.alloc("Neighbor"))));
        return res;
    }

    /**
     * Not sure if this is useful
     *
     * @param colMd The model in which to search prefix mappings
     */
    public String answersListString(CollectionsModel colMd) {
        List<String> list = new ArrayList<>();
        AtomicInteger blankCounter = new AtomicInteger();
        getAnswersList().forEach(n -> {
            if (!n.isBlank()) {
                list.add(colMd.shortform(n.toString()));
            } else {
                blankCounter.getAndIncrement();
            }
        });
        String res = list.toString().replace("[", "").replace("]", "").replaceAll(", ", "\n");
        if (blankCounter.get() != 0) res += "\n" + blankCounter + " blank nodes";
        return res;
    }

    public String answersListJson(CollectionsModel colMd) {
        List<String> list = new ArrayList<>();
        getAnswersList().forEach(n -> {
            if (!n.isBlank()) {
                list.add(colMd.shortform(n.toString()));
            }
        });
        String res = list.toString().replace("[", "[\"").replace("]", "\"]").replaceAll(", ", "\",\"");
        return res;
    }

    /**
     * @param colMd The CollectionsModel in which to search prefix mappings
     */
    public String relaxQueryElementsString(CollectionsModel colMd) {
        List<String[]> pathBlocks = new ArrayList<>();
        Map<String, String> filters = new HashMap<>();
        for (Element e : relaxQueryElements) {
            if (e instanceof ElementPathBlock) {
                TriplePath t = ((ElementPathBlock) e).getPattern().get(0);
                String s0 = colMd.shortform(t.getSubject().toString()
                        .replaceAll(">", "")
                        .replaceAll("<", ""));
                String s1 = colMd.shortform(t.getPredicate().toString()
                        .replaceAll(">", "")
                        .replaceAll("<", ""))
                        .replaceAll("rdf:type", "a")
                        .replaceAll("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "a");
                String s2 = colMd.shortform(t.getObject().toString()
                        .replaceAll(">", "")
                        .replaceAll("<", ""));
                pathBlocks.add(new String[]{s0, s1, s2});
            } else if (e instanceof ElementFilter) {
                Expr expr = ((ElementFilter) e).getExpr();
                if (expr instanceof E_Equals) {
                    String var = (((E_Equals) expr).getArg1()).toString();
                    String node = (((E_Equals) expr).getArg2()).toString().replaceAll(">", "").replaceAll("<", "");
                    filters.put(var, colMd.shortform(node));
                }
            }
        }
        Comparator<String[]> order = Comparator
                .<String[]>comparingInt(strings -> {
                    if (strings[0].contains("Neighbor")) return -2;
                    else return -1;
                })
                .thenComparingInt(strings -> {
                    if (strings[2].contains("Neighbor")) return -2;
                    else return -1;
                })
                .thenComparingInt(strings -> (StringUtils.countMatches(strings[0], "?") + StringUtils.countMatches(strings[2], "?")))
                .thenComparing(strings -> strings[1])
                .thenComparing(strings -> strings[2]);
        pathBlocks.sort(order);
        List<String> res = new ArrayList<>();
        for (String[] strings : pathBlocks) {
            String s = "";
            if (filters.containsKey(strings[0])) {
                s += filters.get(strings[0]);
            } else {
                s += strings[0];
            }
            s += " ";
            if (filters.containsKey(strings[1])) {
                s += filters.get(strings[1]);
            } else {
                s += strings[1];
            }
            s += " ";
            if (filters.containsKey(strings[2])) {
                s += filters.get(strings[2]);
            } else {
                s += strings[2];
            }
            res.add(s);
        }
        return res.toString()
                .replace("[", "")
                .replace("]", "")
                .replaceAll(", ", "\n");
    }

}
