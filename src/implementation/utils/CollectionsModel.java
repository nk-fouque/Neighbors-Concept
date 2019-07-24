package implementation.utils;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.SelectorImpl;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.sparql.algebra.Table;
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

    private Map<String, Map<Property, List<RDFNode>>> triplesSimple = new HashMap<>();
    private Map<String, Map<Property, List<RDFNode>>> triplesSimpleReversed = new HashMap<>();

    private Map<Element, Table> ans = new HashMap<>();

    /**
     * @param md    The model to get informations from
     * @param mdInf If set to null, will use the basic inference reasoner to expand it
     */
    public CollectionsModel(Model md, Model mdInf) {
        graph = md;
        saturatedGraph = Objects.requireNonNullElseGet(mdInf, () -> ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), md));

        StmtIterator iter = graph.listStatements();
        iter.forEachRemaining(stmt -> {
            Map<Property, List<RDFNode>> propertiesFrom = triplesSimple.computeIfAbsent(stmt.getSubject().toString(), (m) -> new HashMap<>());
            List<RDFNode> thatPropertyFrom = propertiesFrom.computeIfAbsent(stmt.getPredicate(), (l) -> new ArrayList<>());
            thatPropertyFrom.add(stmt.getObject());
            Map<Property, List<RDFNode>> propertiesTo = triplesSimpleReversed.computeIfAbsent(stmt.getObject().toString(), (m) -> new HashMap<>());
            List<RDFNode> thatPropertyTo = propertiesTo.computeIfAbsent(stmt.getPredicate(), (l) -> new ArrayList<>());
            thatPropertyTo.add(stmt.getSubject());
        });
    }

    public Table ans(Element element){
        Table res = ans.getOrDefault(element, null);
        return res;
    }

    public boolean addAns(Element element,Table table){
        ans.put(element,table);
        return true;
    }

    /**
     * @return The RDF Graph in its Jena {@link Model} form
     */
    public Model getGraph() {
        return graph;
    }

    public Model getSaturatedGraph() {
        return saturatedGraph;
    }

    @Override
    public String toString() {
        return ("\n\n" + triplesSimple + "\n\n" + triplesSimpleReversed);
    }

    public ResIterator subClassesOf(Node object) {
        return getGraph().listSubjectsWithProperty(RDFS.subClassOf, new ResourceImpl(object.toString()));
    }

    public ResIterator subPropertiesOf(Node property) {
        return getGraph().listSubjectsWithProperty(RDFS.subPropertyOf, new ResourceImpl(property.toString()));
    }

    public StmtIterator triplesFrom(Resource resource) {
        return getSaturatedGraph().listStatements(new SelectorImpl(resource, null, (RDFNode) null));
    }

    public StmtIterator triplesTo(RDFNode node) {
        return getSaturatedGraph().listStatements(new SelectorImpl(null, null, node));
    }

    public StmtIterator simpleTriplesFrom(Resource resource) {
        return getSaturatedGraph().listStatements(new SelectorImpl(resource, null, (RDFNode) null));
    }

    public StmtIterator simpleTriplesTo(RDFNode node) {
        return getSaturatedGraph().listStatements(new SelectorImpl(null, null, node));
    }

    public Map<String, Map<Property, List<RDFNode>>> getTriplesSimple() {
        return triplesSimple;
    }

    public Map<String, Map<Property, List<RDFNode>>> getTriplesSimpleReversed() {
        return triplesSimpleReversed;
    }
}
