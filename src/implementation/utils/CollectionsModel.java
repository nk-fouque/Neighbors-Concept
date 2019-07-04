package implementation.utils;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDFS;

import java.util.*;

/**
 * Shouldn't be useful after the implementation of LazyJoins
 */
public class CollectionsModel {

    private Model graph;
    private Model saturatedGraph;
    Map<String, List<RDFNode>> subClassOf = new HashMap<>();
    Map<String, List<RDFNode>> subPropertyOf = new HashMap<>();
    Map<String, Map<Property, List<RDFNode>>> triples = new HashMap<>();
    Map<String, Map<Property, List<RDFNode>>> triplesReversed = new HashMap<>();
    Map<String, Map<RDFNode, List<RDFNode>>> predicates = new HashMap<>();
    Map<String, Map<RDFNode, List<RDFNode>>> predicatesReversed = new HashMap<>();

    Map<String, Map<Property, List<RDFNode>>> triplesSimple = new HashMap<>();

    public CollectionsModel(Model md, Model mdInf) {
        graph = md;
        saturatedGraph = mdInf;
        StmtIterator iter = mdInf.listStatements();
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

        iter = md.listStatements();
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
        });
    }

    public Model getGraph() {
        return graph;
    }

    public Model getSaturatedGraph() {
        return saturatedGraph;
    }

    /**
     * For debugging purposes, makes the model lighter by assigning only one object to every properties of a subject
     * TODO Remove this function completely
     */
    public void downSizing() {
        HashMap<String, Map<Property, List<RDFNode>>> triplesDown = new HashMap<>();
        for (String s : triples.keySet()) {
            for (Property p : triples.get(s).keySet()) {
                List<RDFNode> list = Collections.singletonList(triples.get(s).get(p).get(0));
                Map<Property, List<RDFNode>> map1 = new HashMap<>();
                map1.put(p, list);
                triplesDown.put(s, map1);
            }
        }
        triples = triplesDown;
        // TODO More complicated to downsize triplesReversed but it's not used for now so i'm leaving it since it might not be relevant by the time i delete it

        HashMap<String, Map<Property, List<RDFNode>>> triplesSimplesDown = new HashMap<>();
        for (String s : triplesSimple.keySet()) {
            for (Property p : triplesSimple.get(s).keySet()) {
                List<RDFNode> list = Collections.singletonList(triplesSimple.get(s).get(p).get(0));
                Map<Property, List<RDFNode>> map = new HashMap<>();
                map.put(p, list);
                triplesSimplesDown.put(s, map);
            }
        }
        triplesSimple = triplesSimplesDown;

        HashMap<String, Map<RDFNode, List<RDFNode>>> predicatesDown = new HashMap<>();
        HashMap<String, Map<RDFNode, List<RDFNode>>> predicatesReversedDown = new HashMap<>();
        for (String s : predicates.keySet()) {
            if (!(s.contains("subClassOf") || s.contains("domain") || s.contains("range") || s.contains("subPropertyOf"))) {
                for (RDFNode subj : predicates.get(s).keySet()) {
                    RDFNode obj = predicates.get(s).get(subj).get(0);
                    List<RDFNode> listObj = Collections.singletonList(obj);
                    predicatesDown.computeIfAbsent(s, m -> new HashMap<>());
                    predicatesDown.get(s).put(subj, listObj);

                    List<RDFNode> listSubj = Collections.singletonList(subj);
                    predicatesReversedDown.computeIfAbsent(s, m -> new HashMap<>());
                    predicatesReversedDown.get(s).put(obj, listSubj);
                }
            } else {
                predicatesDown.put(s, predicates.get(s));
                predicatesReversedDown.put(s, predicatesReversed.get(s));
            }
        }
        predicates = predicatesDown;
        predicatesReversed = predicatesReversedDown;

    }

    @Override
    public String toString() {
        return (subClassOf + "\n\n" + subPropertyOf + "\n\n" + triples + "\n\n" + triplesReversed +
                "\n\n" + predicates + "\n\n" + predicatesReversed);
    }
}
