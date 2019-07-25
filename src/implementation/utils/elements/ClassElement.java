package implementation.utils.elements;

import implementation.utils.CollectionsModel;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.vocabulary.RDF;

import java.util.HashSet;
import java.util.Set;

public class ClassElement extends TriplePatternElement{
    public ClassElement(ElementPathBlock elementPathBlock, CollectionsModel colMd){
        super(elementPathBlock,colMd);
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
