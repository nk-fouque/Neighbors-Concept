package implementation;

import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.table.TableN;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import implementation.utils.*;

import java.io.ByteArrayOutputStream;
import java.util.*;

public class Cluster implements Comparable<Cluster> {
    private static Logger logger = Logger.getLogger(Cluster.class);
    private List<Var> proj;
    private List<Element> relaxQueryElements;
    private int relaxDistance;
    private List<Element> availableQueryElements;
    private Table mapping;
    private Table answers;
    private List<Var> connectedVars;
    private List<Element>
            removedQueryElements;

    /**
     * The head of the query
     */
    public List<Var> getProj() {
        return proj;
    }

    /**
     * The body of the relaxed Query
     */
    public List<Element> getRelaxQueryElements() {
        return relaxQueryElements;
    }

    /**
     * The relaxation distance of this cluster, expressed as the extensional distance
     */
    public int getRelaxDistance() {
        return relaxDistance;
    }

    /**
     * The query elements that have yet to be tested in this cluster
     */
    public List<Element> getAvailableQueryElements() {
        return availableQueryElements;
    }

    /**
     * The Match-set containing all the answers to the relaxed queries
     */
    public Table getMapping() {
        return mapping;
    }

    /**
     * The list containing all answers held by this cluster i.e. not in any cluster with a lower relaxation distance
     */
    public Table getAnswers() {
        return answers;
    }

    /**
     * Query elements that have been tested and thrown away/relaxed, to avoid putting them twice
     */
    public List<Element> getRemovedQueryElements() {
        return removedQueryElements;
    }

    /**
     * Creates the initial Cluster for the Partition Algorithm from the Query qry and the RDF Graph graph
     */
    public Cluster(Query qry, Model graph) {
        this.proj = qry.getProjectVars();
        this.relaxQueryElements = new ArrayList<>();
        this.relaxDistance = 0;
        this.availableQueryElements = new ArrayList<>();
        List<Element> list = (((ElementGroup) qry.getQueryPattern()).getElements());
        for (Element e : list) {
            if (e instanceof ElementPathBlock) {
                List<TriplePath> triplelist = (((ElementPathBlock) e).getPattern().getList());
                for (TriplePath t : triplelist) {
                    ElementPathBlock element = new ElementPathBlock();
                    element.addTriple(t);
                    availableQueryElements.add(element);
                }
            } else if (e instanceof ElementFilter) {
                availableQueryElements.add(e);
            }
        }
        availableQueryElements = ListUtils.removeDuplicates(availableQueryElements);
        this.removedQueryElements = new ArrayList<>();
        this.mapping = new TableN();
        this.answers = new TableN();
        ResIterator data = graph.listSubjects();
        data.forEachRemaining((Resource resource) -> {
            for (Var var : getProj()) {
                mapping.addBinding(BindingFactory.binding(var, resource.asNode()));
                answers.addBinding(BindingFactory.binding(var, resource.asNode()));
            }
        });
        this.connectedVars = qry.getProjectVars();
    }

    /**
     * Creates a cluster with the same values as an other but different Mapping ext Answers
     */
    public Cluster(Cluster c, Table Me, Table Ae) {
        this.proj = new ArrayList<>(c.getProj());
        this.relaxQueryElements = new ArrayList<>();
        this.relaxQueryElements.addAll(c.getRelaxQueryElements());
        this.relaxDistance = c.relaxDistance;
        this.availableQueryElements = new ArrayList<>();
        this.availableQueryElements.addAll(c.getAvailableQueryElements());
        this.removedQueryElements = new ArrayList<>();
        this.removedQueryElements.addAll(c.getRemovedQueryElements());
        this.mapping = Me;
        this.answers = Ae;
        this.connectedVars = new ArrayList<>();
        this.connectedVars.addAll(c.connectedVars);
        connectedVars = ListUtils.removeDuplicates(connectedVars);
    }

    /**
     * @return the difference between this Cluster's relax distance and the other Cluster
     */
    public int compareTo(Cluster other) {
        return (getRelaxDistance() - other.getRelaxDistance());
    }

    /**
     * Substract an element from the available query elements to mark it as used and adds it to the elements distinctive of this Cluster's neighbors
     *
     * @param element The Element to be moved
     * @param vars    The Jena Variables mentioned to the element
     */
    public void move(Element element, List<Var> vars) throws PartitionException {
        boolean removed = availableQueryElements.remove(element);
        if (!removed) {
            throw new PartitionException("Could not move :" + element.toString());
        } else {
            relaxQueryElements.add(element);
            connectedVars.addAll(vars);
        }
    }

    /**
     * @param vars A list of Variables
     * @return true if this cluster is connected to an element mentioning these variables
     */
    public boolean connected(List<Var> vars) {
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
     * Substracts an element from the available query elements to mark it as used and increases the relax distance of this Cluster by one
     *
     * @param element The element to relax
     * @throws PartitionException
     */
    public void relax(Element element, CollectionsModel graph, Map<String, Var> keys) throws PartitionException {
        SingletonStopwatchCollection.resume("relax");
        boolean removed = availableQueryElements.remove(element);
        if (!removed) {
            throw new PartitionException("Could not relax " + element.toString() + " : Element not found when trying to remove");
        } else {
            List<Element> list = new ArrayList<>();
            if (element instanceof ElementFilter) {
                ElementFilter e = (ElementFilter) element;
                list = ElementUtils.relaxFilter(e, graph, keys);
            } else {
                ElementPathBlock e = (ElementPathBlock) element;
                TriplePath t = e.getPattern().get(0);
                if (t.getPredicate().equals(RDF.type.asNode())) {
                    list = ElementUtils.relaxClass(t, graph);
                } else {
                    list = ElementUtils.relaxProperty(t, graph);
                }
            }
            list.removeAll(availableQueryElements);
            list.removeAll(relaxQueryElements);
            list.removeAll(removedQueryElements);
            availableQueryElements.addAll(list);
            removedQueryElements.add(element);
            relaxDistance++;
        }
        SingletonStopwatchCollection.stop("relax");
    }

    /**
     * @return true if this cluster has nothing in its answers
     */
    boolean noAnswers() {
        return (this.getAnswers().size() == 0);
    }

    /**
     * @return The string for the relaxed Query corresponding to this Cluster
     */
    public String queryString() {
        return ElementUtils.getSelectStringFrom(this.proj, relaxQueryElements);
    }

    /**
     * @return the mapping as a string formatted by Jena
     * @deprecated Usually the mappings are way to big for output streams
     */
    @Deprecated
    public String mappingString() {
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        ResultSetFormatter.out(baos1, getMapping().toResultSet());
        return baos1.toString();
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
        String res = "Extentional Distance : " + relaxDistance + "\n";
        res += "Elements : " + relaxQueryElements + "\n";
        if (Level.DEBUG.isGreaterOrEqual(logger.getLevel())) {
            res += "Debug available: " + availableQueryElements + "\n";
            res += "Debug removed : " + removedQueryElements + "\n";
            res += "Connected variables : " + connectedVars + "\n";
        }
        res += "Query :" + queryString() + "\n";
        if (Level.TRACE.isGreaterOrEqual(logger.getLevel())) res += "Mapping :\n" + mappingString() + "\n";
        res += "Answers :\n" + answersString() + "\n";
        return res;
    }

    /**
     * Same as {@link #toString()} but uses {@link #answersListString(CollectionsModel)} to write answers
     * @param col
     * @return
     */
    public String toString(CollectionsModel col) {
        String res = "Extentional Distance : " + relaxDistance + "\n";
        res += "Elements : " + relaxQueryElements + "\n";
        if (Level.DEBUG.isGreaterOrEqual(logger.getLevel())) {
            res += "Debug available: " + availableQueryElements + "\n";
            res += "Debug removed : " + removedQueryElements + "\n";
            res += "Connected variables : " + connectedVars + "\n";
        }
        res += "Query :" + queryString() + "\n";
        if (Level.TRACE.isGreaterOrEqual(logger.getLevel())) res += "Mapping :\n" + mappingString() + "\n";
        res += "Answers :\n" + answersListString(col) + "\n";
        return res;
    }

    /**
     * @return This cluster's answers as a Java List
     */
    public List<Node> getAnswersList() {
        Iterator<Binding> iter = answers.rows();
        List<Node> res = new ArrayList<>();
        iter.forEachRemaining((Binding b) -> res.add(b.get(Var.alloc("Neighbor"))));
        return res;
    }

    /**
     * Not sure if this is useful
     * @param col The model in which to search prefix mappings
     * @return
     */
    public String answersListString(CollectionsModel col){
        List<String> res = new ArrayList<>();
        getAnswersList().forEach(n -> res.add(col.getGraph().shortForm(n.toString())));
        return res.toString().replace("[","|\t ").replace("]","\t\t|").replaceAll(", ","\t\t|\n|\t");
    }

}
