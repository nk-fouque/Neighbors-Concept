package implementation.utils;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.SelectorImpl;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.vocabulary.RDFS;

import java.util.*;

/**
 * A representation of an RDF Graph using several {@link HashMap} to accelerate accesses
 *
 * @author nk-fouque
 */
public class CollectionsModel {

    private Model graph;
    private Model saturatedGraph;

    private Map<String, Map<Property, List<RDFNode>>> triples = new HashMap<>();
    private Map<String, Map<Property, List<RDFNode>>> triplesReversed = new HashMap<>();

    private Map<Element, Table> ans = new HashMap<>();
    private Map<String, Var> keys = new HashMap<>();
    private int nextKey = 1;
    private Map<Element, Integer> depth = new HashMap<>();

    /**
     * @param md    The model to get informations from
     * @param mdInf If set to null, will use the basic inference reasoner to expand it
     */
    public CollectionsModel(Model md, Model mdInf) {
        graph = md;
        saturatedGraph = Objects.requireNonNullElseGet(mdInf, () -> ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), md));

        StmtIterator iter = saturatedGraph.listStatements();
        iter.forEachRemaining(stmt -> {
            Map<Property, List<RDFNode>> propertiesFrom = triples.computeIfAbsent(stmt.getSubject().toString(), m -> new HashMap<>());
            List<RDFNode> thatPropertyFrom = propertiesFrom.computeIfAbsent(stmt.getPredicate(), (l) -> new ArrayList<>());
            thatPropertyFrom.add(stmt.getObject());
            Map<Property, List<RDFNode>> propertiesTo = triplesReversed.computeIfAbsent(stmt.getObject().toString(), m -> new HashMap<>());
            List<RDFNode> thatPropertyTo = propertiesTo.computeIfAbsent(stmt.getPredicate(), (l) -> new ArrayList<>());
            thatPropertyTo.add(stmt.getSubject());
        });
    }

    /**
     * @return The RDF Graph in its Jena {@link Model} form
     */
    public Model getGraph() {
        return graph;
    }

    /**
     * @return The RDF Graph with applied inference reasoning
     */
    public Model getSaturatedGraph() {
        return saturatedGraph;
    }

    /**
     * @return An iterator on resources that are subclasses of the one in parameter
     */
    public NodeIterator subClassesOf(Node node) {
        return getGraph().listObjectsOfProperty(new ResourceImpl(node.getURI()), RDFS.subClassOf);
    }

    /**
     * @return An iterator on resources that are subproperties of the one in parameter
     */
    public NodeIterator subPropertiesOf(Node node) {
        return getGraph().listObjectsOfProperty(new ResourceImpl(node.getURI()), RDFS.subPropertyOf);
    }

    /**
     * @return An iterator on Statements that have the one in parameter as subject
     */
    public StmtIterator triplesFrom(Resource resource) {
        return getSaturatedGraph().listStatements(new SelectorImpl(resource, null, (RDFNode) null));
    }

    /**
     * @return An iterator on Statements that have the one in parameter as object
     */
    public StmtIterator triplesTo(RDFNode node) {
        return getSaturatedGraph().listStatements(new SelectorImpl(null, null, node));
    }

    /**
     * Same as {@link #triplesFrom(Resource)} but without inference reasoning
     */
    public StmtIterator simpleTriplesFrom(Resource resource) {
        return getGraph().listStatements(new SelectorImpl(resource, null, (RDFNode) null));
    }

    /**
     * Same as {@link #triplesTo(RDFNode)} (Resource)} but without inference reasoning
     */
    public StmtIterator simpleTriplesTo(RDFNode node) {
        return getGraph().listStatements(new SelectorImpl(null, null, node));
    }

    public Map<String, Map<Property, List<RDFNode>>> getTriples() {
        return triples;
    }

    public Map<String, Map<Property, List<RDFNode>>> getTriplesReversed() {
        return triplesReversed;
    }

    /**
     * @param element
     * @return All the answers to a query containing the element as only selector
     */
    public Table ans(Element element) {
        Table res = ans.getOrDefault(element, null);
        return res;
    }

    /**
     * Adds an element and the corresponding table of answers to the model
     */
    public void addAns(Element element, Table table) {
        ans.put(element, table);
    }

    public Map<String, Var> getKeys() {
        return keys;
    }

    public Var varKey(String uri) {
        if (keys.containsKey(uri)) {
            return keys.get(uri);
        } else {
            Var key = keys.computeIfAbsent(uri, var -> Var.alloc("x" + nextKey));
            nextKey++;
            return key;
        }
    }

    public int getDepth(Element element) {
        return depth.get(element);
    }

    public int setDepth(Element element, int i) {
        depth.putIfAbsent(element, i);
        return depth.get(element);
    }

    @Override
    public String toString() {
        return ("\n\n" + triples + "\n\n" + triplesReversed);
    }

    public String shortform(String s){
        String res = getGraph().shortForm(s);
        if (res.contains("^^")){
            String[] temp = res.split("\\^\\^",2);
            String val = temp[0];
            String type = getGraph().shortForm(temp[1]);
            res = (val+"^^"+type);
        }
        return res;
    }

}