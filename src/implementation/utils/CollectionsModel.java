package implementation.utils;

import implementation.algorithms.Cluster;
import implementation.utils.elements.QueryElement;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.SelectorImpl;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.core.Var;
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

    private Map<String, Map<Property, List<RDFNode>>> triplesSimple = new HashMap<>();
    private Map<String, Map<Property, List<RDFNode>>> triplesSimpleReversed = new HashMap<>();

    private Map<QueryElement, Integer> depth = new HashMap<>();
    private Map<QueryElement, Table> ans = new HashMap<>();
    private Map<String, Var> keys = new HashMap<>();
    private int nextKey = 1;

    private Map<Cluster,Map<QueryElement,Integer>> usages = new HashMap<>();

    /**
     * @param md    The model to get informations from
     * @param mdInf If set to null, will use the basic inference reasoner to expand it
     */
    public CollectionsModel(Model md, Model mdInf) {
        graph = md;
        saturatedGraph = Objects.requireNonNullElseGet(mdInf, () -> ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), md));

        StmtIterator iter = graph.listStatements();
        iter.forEachRemaining(stmt -> {
            Map<Property, List<RDFNode>> propertiesFrom = triplesSimple.computeIfAbsent(stmt.getSubject().toString(), m -> new HashMap<>());
            List<RDFNode> thatPropertyFrom = propertiesFrom.computeIfAbsent(stmt.getPredicate(), (l) -> new ArrayList<>());
            thatPropertyFrom.add(stmt.getObject());
            Map<Property, List<RDFNode>> propertiesTo = triplesSimpleReversed.computeIfAbsent(stmt.getObject().toString(), m -> new HashMap<>());
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
        return getGraph().listObjectsOfProperty(new ResourceImpl(node.toString()),RDFS.subClassOf);
    }

    /**
     * @return An iterator on resources that are subproperties of the one in parameter
     */
    public NodeIterator subPropertiesOf(Node node) {
        return getGraph().listObjectsOfProperty(new ResourceImpl(node.toString()),RDFS.subPropertyOf);
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

    public Map<String, Map<Property, List<RDFNode>>> getTriplesSimple() {
        return triplesSimple;
    }

    public Map<String, Map<Property, List<RDFNode>>> getTriplesSimpleReversed() {
        return triplesSimpleReversed;
    }

    /**
     * @param element
     * @return All the answers to a query containing the element as only selector
     */
    public Table ans(QueryElement element) {
        Table res = ans.getOrDefault(element, null);
        return res;
    }

    /**
     * Adds an element and the corresponding table of answers to the model
     */
    public void addAns(QueryElement element, Table table) {
        ans.put(element, table);
    }

    public Map<String, Var> getKeys() {
        return keys;
    }

    public Var varKey(String uri){
        if (keys.containsKey(uri)){
            return keys.get(uri);
        } else {
            Var key = keys.computeIfAbsent(uri,var -> Var.alloc("x"+nextKey));
            nextKey++;
            return key;
        }
    }

    @Override
    public String toString() {
        return ("\n\n" + triplesSimple + "\n\n" + triplesSimpleReversed);
    }

    public Map<QueryElement, Integer> getDepth() {
        return depth;
    }

    public void setDepth(QueryElement element, Integer depth) {
        this.depth.putIfAbsent(element, depth);
    }

    public void copy(Cluster original,Cluster copy){

    }

    public Map<Cluster, Map<QueryElement, Integer>> getUsages() {
        return usages;
    }

    public int getUsage(QueryElement qe,Cluster c){
        Map<QueryElement,Integer> usageMap = usages.getOrDefault(c,null);
        if (usageMap!=null){
            return usageMap.getOrDefault(qe,0);
        } else {
            return 0;
        }
    }

    public void use(QueryElement qe, Cluster c){
        Map<QueryElement,Integer> usageMap = usages.computeIfAbsent(c, m -> new HashMap<>());
        int uses = usageMap.getOrDefault(qe,0);
        usageMap.put(qe,uses+1);
    }
}