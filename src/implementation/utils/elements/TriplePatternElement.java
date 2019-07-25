package implementation.utils.elements;

import implementation.utils.CollectionsModel;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
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
import org.apache.jena.sparql.expr.E_Equals;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementPathBlock;

import java.util.HashSet;
import java.util.Set;

public class TriplePatternElement extends QueryElement {
    public TriplePatternElement(ElementPathBlock elementPathBlock, CollectionsModel colMd) {
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

    @Override
    public Set<FilterElement> separate() {
        Set<FilterElement> res = new HashSet<>();
        Triple triple = ((ElementPathBlock) element).getPattern().get(0).asTriple();
        Node subject = triple.getSubject();
        Var varSubj;
        if (!subject.isVariable()) {
            if (subject.isURI()) {
                varSubj = model.varKey(subject.getURI());
            } else {
                varSubj = model.varKey(subject.toString());
            }
            ElementFilter filter = new ElementFilter(new E_Equals(new ExprVar(varSubj), new NodeValueNode(subject)));

            res.add(new FilterElement(filter,model));
        } else {
            varSubj = (Var) subject;
        }

        Node object = triple.getObject();
        Var varObj;
        if (!object.isVariable()) {
            if (object.isURI()) {
                varObj = model.varKey(object.getURI());
            } else {
                varObj = model.varKey(object.toString());
            }
            ElementFilter filter = new ElementFilter(new E_Equals(new ExprVar(varObj), new NodeValueNode(object)));
            res.add(new FilterElement(filter,model));
        } else {
            varObj = (Var) object;
        }

        ElementPathBlock eFiltered = new ElementPathBlock();
        eFiltered.addTriple(Triple.create(varSubj, triple.getPredicate(), varObj));
        element = eFiltered;

        return res;
    }
}
