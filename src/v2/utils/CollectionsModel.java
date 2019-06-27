package v2.utils;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionsModel {

    Model graph;
    Model saturatedGraph;
    Map<String, List<RDFNode>> subClassOf=new HashMap<>();
    Map<String, List<RDFNode>> subPropertyOf=new HashMap<>();
    Map<String, Map<Property,List<RDFNode>>> triples=new HashMap<>();
    Map<String, Map<Property,List<RDFNode>>> triplesReversed=new HashMap<>();
    Map<String, Map<RDFNode,List<RDFNode>>> predicates = new HashMap<>();
    Map<String, Map<RDFNode,List<RDFNode>>> predicatesReversed = new HashMap<>();

    Map<String, Map<Property,List<RDFNode>>> triplesSimple=new HashMap<>();
    public CollectionsModel(Model md, Model mdInf){
        graph = md;
        saturatedGraph = mdInf;
        StmtIterator iter = mdInf.listStatements();
        iter.forEachRemaining(stmt -> {
            Map<Property,List<RDFNode>> propertiesFrom = triples.computeIfAbsent(stmt.getSubject().toString(),(m) -> new HashMap<>());
            List thatPropertyFrom = propertiesFrom.computeIfAbsent(stmt.getPredicate(),(l) -> new ArrayList<>());
            thatPropertyFrom.add(stmt.getObject());

            Map<Property,List<RDFNode>> propertiesTo = triplesReversed.computeIfAbsent(stmt.getObject().toString(),(m) -> new HashMap<>());
            List thatPropertyTo = propertiesTo.computeIfAbsent(stmt.getPredicate(),(l) -> new ArrayList<>());
            thatPropertyTo.add(stmt.getSubject());

            Map<RDFNode,List<RDFNode>> properties = predicates.computeIfAbsent(stmt.getPredicate().toString(),(m) -> new HashMap<>());
            List thatProperty = properties.computeIfAbsent(stmt.getSubject(),(l) -> new ArrayList<>());
            thatProperty.add(stmt.getObject());

            Map<RDFNode,List<RDFNode>> propertiesReversed = predicatesReversed.computeIfAbsent(stmt.getPredicate().toString(),(m) -> new HashMap<>());
            List thatPropertyReversed = propertiesReversed.computeIfAbsent(stmt.getObject(),(l) -> new ArrayList<>());
            thatPropertyReversed.add(stmt.getSubject());
        });

        iter = md.listStatements();
        iter.forEachRemaining(stmt -> {
            if (stmt.getPredicate().equals(RDFS.subClassOf)){
                List<RDFNode> list = subClassOf.computeIfAbsent(stmt.getSubject().toString(),(l) -> new ArrayList<>());
                list.add(stmt.getObject());
            } else if (stmt.getPredicate().equals(RDFS.subPropertyOf)){
                List<RDFNode> list = subPropertyOf.computeIfAbsent(stmt.getSubject().toString(),(l) -> new ArrayList<>());
                list.add(stmt.getObject());
            }
            Map<Property,List<RDFNode>> propertiesFrom = triplesSimple.computeIfAbsent(stmt.getSubject().toString(),(m) -> new HashMap<>());
            List thatPropertyFrom = propertiesFrom.computeIfAbsent(stmt.getPredicate(),(l) -> new ArrayList<>());
            thatPropertyFrom.add(stmt.getObject());
        });
    }

    public Model getGraph() {
        return graph;
    }

    public Model getSaturatedGraph() {
        return saturatedGraph;
    }

    @Override
    public String toString() {
        return(subClassOf+"\n\n"+subPropertyOf+"\n\n"+triples+"\n\n"+triplesReversed+
                "\n\n"+predicates+"\n\n"+predicatesReversed);
    }
}
