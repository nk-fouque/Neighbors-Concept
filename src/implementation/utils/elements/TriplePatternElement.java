package implementation.utils.elements;

import implementation.utils.CollectionsModel;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.table.TableN;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.syntax.ElementPathBlock;

public class TriplePatternElement extends QueryElement {
    public TriplePatternElement(ElementPathBlock elementPathBlock, CollectionsModel colMd){
        element = elementPathBlock;
        model = colMd;
    }

    @Override
    protected Table answer() {
        Table res = new TableN();
        logger.debug("is pathblock");
        Var subjVar = (Var) ((ElementPathBlock) element).getPattern().get(0).getSubject();
        // Subject is always a variable because triples are obtained from describeNode
        Property predicate = new PropertyImpl(((ElementPathBlock) element).getPattern().get(0).getPredicate().toString());
        Node object = ((ElementPathBlock) element).getPattern().get(0).getObject();
        logger.debug("predicate : " + predicate);
        if (object.isVariable()) {
            // Object can be something other than a Variable if we are describing classes by their members and subclasses
            Var objVar = (Var) object;
            logger.debug("against variable");
            ResIterator iterSubj = model.getSaturatedGraph().listSubjectsWithProperty(predicate);

            iterSubj.forEachRemaining(subj -> {
                NodeIterator iterobj = model.getGraph().listObjectsOfProperty(subj, predicate);
                iterobj.forEachRemaining(obj -> {
                    BindingHashMap bind = new BindingHashMap();
                    bind.add(subjVar, subj.asNode());
                    bind.add(objVar, obj.asNode());
                    res.addBinding(bind);
                });
            });
        } else if (object.isURI()) {
            logger.debug("object is uri");
            RDFNode objNode = new ResourceImpl(object.getURI());
            ResIterator iterSubj = model.getSaturatedGraph().listSubjectsWithProperty(predicate, objNode);
            iterSubj.forEachRemaining(subj -> {
                BindingHashMap bind = new BindingHashMap();
                bind.add(subjVar, subj.asNode());
                res.addBinding(bind);
            });
        }
        return res;
    }
}
