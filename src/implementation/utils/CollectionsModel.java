package implementation.utils;

import org.apache.jena.rdf.model.*;
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

    private Map<String, List<RDFNode>> subClassOf = new HashMap<>();
    private Map<String, List<RDFNode>> subPropertyOf = new HashMap<>();
    private Map<String, Map<Property, List<RDFNode>>> triples = new HashMap<>();
    private Map<String, Map<Property, List<RDFNode>>> triplesReversed = new HashMap<>();
    private Map<String, Map<RDFNode, List<RDFNode>>> predicates = new HashMap<>();
    private Map<String, Map<RDFNode, List<RDFNode>>> predicatesReversed = new HashMap<>();

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

        StmtIterator iter = saturatedGraph.listStatements();
        iter.forEachRemaining(stmt -> {
            Map<Property, List<RDFNode>> propertiesFrom = triples.computeIfAbsent(stmt.getSubject().toString(), (m) -> new HashMap<>());
            List<RDFNode> thatPropertyFrom = propertiesFrom.computeIfAbsent(stmt.getPredicate(), (l) -> new ArrayList<>());
            thatPropertyFrom.add(stmt.getObject());

            Map<Property, List<RDFNode>> propertiesTo = triplesReversed.computeIfAbsent(stmt.getObject().toString(), (m) -> new HashMap<>());
            List<RDFNode> thatPropertyTo = propertiesTo.computeIfAbsent(stmt.getPredicate(), (l) -> new ArrayList<>());
            thatPropertyTo.add(stmt.getSubject());

            Map<RDFNode, List<RDFNode>> properties = predicates.computeIfAbsent(stmt.getPredicate().toString(), (m) -> new HashMap<>());
            List<RDFNode> thatProperty = properties.computeIfAbsent(stmt.getSubject(), (l) -> new ArrayList<>());
            thatProperty.add(stmt.getObject());

            Map<RDFNode, List<RDFNode>> propertiesReversed = predicatesReversed.computeIfAbsent(stmt.getPredicate().toString(), (m) -> new HashMap<>());
            List<RDFNode> thatPropertyReversed = propertiesReversed.computeIfAbsent(stmt.getObject(), (l) -> new ArrayList<>());
            thatPropertyReversed.add(stmt.getSubject());
        });

        iter = graph.listStatements();
        iter.forEachRemaining(stmt -> {
            if (stmt.getPredicate().equals(RDFS.subClassOf)) {
                List<RDFNode> list = subClassOf.computeIfAbsent(stmt.getSubject().toString(), (l) -> new ArrayList<>());
                list.add(stmt.getObject());
            } else if (stmt.getPredicate().equals(RDFS.subPropertyOf)) {
                List<RDFNode> list = subPropertyOf.computeIfAbsent(stmt.getSubject().toString(), (l) -> new ArrayList<>());
                list.add(stmt.getObject());
            }
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

    @Override
    public String toString() {
        return (subClassOf + "\n\n" + subPropertyOf + "\n\n" + triples + "\n\n" + triplesReversed +
                "\n\n" + predicates + "\n\n" + predicatesReversed +
                "\n\n" + triplesSimple + "\n\n" + triplesSimpleReversed);
    }

    public Map<String, List<RDFNode>> getSubClassOf() {
        return subClassOf;
    }

    public Map<String, List<RDFNode>> getSubPropertyOf() {
        return subPropertyOf;
    }

    public Map<String, Map<Property, List<RDFNode>>> getTriples() {
        return triples;
    }

    public Map<String, Map<Property, List<RDFNode>>> getTriplesReversed() {
        return triplesReversed;
    }

    public Map<String, Map<RDFNode, List<RDFNode>>> getPredicates() {
        return predicates;
    }

    public Map<String, Map<RDFNode, List<RDFNode>>> getPredicatesReversed() {
        return predicatesReversed;
    }

    public Map<String, Map<Property, List<RDFNode>>> getTriplesSimple() {
        return triplesSimple;
    }

    public Map<String, Map<Property, List<RDFNode>>> getTriplesSimpleReversed() {
        return triplesSimpleReversed;
    }
}
