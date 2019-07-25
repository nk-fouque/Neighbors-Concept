package implementation.utils.elements;

import implementation.utils.CollectionsModel;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.table.TableN;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.vocabulary.RDF;

import java.util.HashSet;
import java.util.Set;

public class ClassElement extends QueryElement{
    public ClassElement(ElementPathBlock elementPathBlock, CollectionsModel colMd){
        element = elementPathBlock;
        model = colMd;
    }

    @Override
    Table answer() {
        Table res = new TableN();
        logger.debug("is pathblock");
        Triple triple = ((ElementPathBlock) element).getPattern().get(0).asTriple();
        Var subjVar = (Var)triple.getSubject();
        // Subject is always a variable because triples are obtained from describeNode
        Property predicate = RDF.type;
        Node object = triple.getObject();
        logger.debug("predicate : " + predicate);
        if (object.isURI()) {
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

    @Override
    public Set<QueryElement> relax(int maxDepth){
        logger.debug("relaxing class"+element);
        Set<QueryElement> res = new HashSet<>();
        Triple triple = ((ElementPathBlock) element).getPattern().get(0).asTriple();
        NodeIterator iter = model.subClassesOf(triple.getObject());
        Set<Element> elements = new HashSet<>();
        iter.forEachRemaining(node -> {
            ElementPathBlock pathBlock = new ElementPathBlock();
            pathBlock.addTriple(Triple.create(triple.getSubject(), RDF.type.asNode(), node.asNode()));
            elements.add(pathBlock);
        });
        for(Element element : elements){
            res.add(QueryElement.create(element,model,getDepth()));
        }
        logger.debug("relaxed to"+res);
        return res;
    }
}
