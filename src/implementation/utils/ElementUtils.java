package implementation.utils;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_Equals;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprFunction;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Several static methods to do various things with Jena {@link Element}
 *
 * @author nk-fouque
 */
public class ElementUtils {
    private static Logger logger = Logger.getLogger(ElementUtils.class);

    /**
     * Creates a String for the SPARQL Query given Jena Variables and Elements
     *
     * @param vars     The List of Jena Variables to be put in the head of the Query
     * @param elements The List of Jena Elements to be put in the body of the Query
     * @return The String for the SPARQL Query
     */
    public static String getSelectStringFrom(Set<Var> vars, Set<Element> elements) {
        StringBuilder queryString = new StringBuilder("SELECT DISTINCT");
        for (Var v : vars) {
            queryString.append(" ").append(v.toString());
        }
        queryString.append(" WHERE {");
        for (Element element : elements) {
            queryString.append("\n").append(element.toString()).append(".");
        }
        queryString.append("}");
        return queryString.toString();
    }



    /**
     * TODO REMOVE
     * Relaxes a element
     *
     * @param element The element to relax
     * @param model  The model in which to describe what has to be relaxed
     * @return A list of all the new Query elements obtained from relaxing the element (relax(e))
     */
    public static Set<Element> relaxFilter(ElementFilter element, CollectionsModel model, int descriptionDepth) {
        ExprFunction f = element.getExpr().getFunction();
        Set<Element> list = new HashSet<>();
        if (f instanceof E_Equals) {
            for (Expr expr : f.getArgs()) {
                if (expr instanceof NodeValueNode) {
                    logger.info("Relaxing : " + expr);
                    list.addAll(describeNode((expr).toString().replaceAll("<", "").replaceAll(">", ""), model));
                } else {
                    logger.info(expr + " not NodeValueNode");
                }
            }
        } else {
            logger.info(f + " not E_Equals");
        }

        Set<Element> res = new HashSet<>();
        for (Element e : list) {
            if (!(e instanceof ElementFilter)) {
                if (descriptionDepth > 2) {
                    // TODO For now any number>2 = infinite
                    res.add(e);
                }
            }
        }
        logger.info("relaxed to " + res);
        return res;
    }

    /**
     * @param uri          The uri of the Node to be described
     * @param model        The Model to use to describe the Node
     * @return A list of Query elements (triple pattern and Filters) describing the Nodes known properties
     */
    public static Set<Element> describeNode(String uri, CollectionsModel model) {
        final Set<Element> res = new HashSet<>();
        Resource node = new ResourceImpl(uri);
        StmtIterator triplesFrom = model.simpleTriplesFrom(node);
        triplesFrom.forEachRemaining(statement -> {
            Property property = statement.getPredicate();
            RDFNode object = statement.getObject();
            if (property.equals(RDF.type)) {
                ElementPathBlock pathBlock = new ElementPathBlock();
                pathBlock.addTriple(Triple.create(model.varKey(uri), property.asNode(), object.asNode()));
                res.add(pathBlock);
            } else {
                Var var;
                if (object.isURIResource()) {
                    var = model.varKey(object.asResource().getURI());
                } else {
                    var = model.varKey(object.asLiteral().toString());
                }
                ElementPathBlock triple = new ElementPathBlock();
                triple.addTriple(Triple.create(model.varKey(uri), property.asNode(), var));
                res.add(triple);
                ElementFilter filter = new ElementFilter(new E_Equals(new ExprVar(var), new NodeValueNode(object.asNode())));
                res.add(filter);
            }
        });

        StmtIterator triplesTo = model.simpleTriplesTo(node);
        triplesTo.forEachRemaining(statement -> {
            Resource subject = statement.getSubject();
            Property property = statement.getPredicate();
            if (!property.equals(RDF.type)) {
                Var var;
                if (subject.isURIResource()) {
                    var = model.varKey(subject.asResource().getURI());
                } else {
                    var = model.varKey(subject.asLiteral().toString());
                }
                ElementPathBlock triple = new ElementPathBlock();
                triple.addTriple(Triple.create(var, property.asNode(), model.varKey(uri)));
                res.add(triple);
                ElementFilter filter = new ElementFilter(new E_Equals(new ExprVar(var), new NodeValueNode(subject.asNode())));
                res.add(filter);
            }
        });
        return res;
    }

    /**
     * TODO REMOVE
     * RDF:type is to be relaxed differently than other properties
     *
     * @param triple A triple pattern with predicate RDF:type
     * @param model  The model in which to search for successors
     * @return A list of all the new Query elements obtained from relaxing the triple pattern (relax(e))
     */
    public static Set<Element> relaxClass(TriplePath triple, CollectionsModel model) {
        logger.info("Relaxing " + triple);
        NodeIterator iter = model.subClassesOf(triple.getObject());
        Set<Element> res = new HashSet<>();
        iter.forEachRemaining(node -> {
            ElementPathBlock pathBlock = new ElementPathBlock();
            pathBlock.addTriple(Triple.create(triple.getSubject(), RDF.type.asNode(), node.asNode()));
            res.add(pathBlock);
        });
        logger.info("Relaxed to " + res);
        return res;
    }

    /**
     * TODO REMOVE
     * @param triple A triple pattern
     * @param model  The model in which to search for successors
     * @return A list of all the new Query elements obtained from relaxing the triple pattern (relax(e))
     */
    public static Set<Element> relaxProperty(TriplePath triple, CollectionsModel model) {
        logger.info("Relaxing " + triple);
        NodeIterator iter = model.subPropertiesOf(triple.getPredicate());
        Set<Element> res = new HashSet<>();
        iter.forEachRemaining(node -> {
            ElementPathBlock pathBlock = new ElementPathBlock();
            pathBlock.addTriple(Triple.create(triple.getSubject(), node.asNode(), triple.getObject()));
            res.add(pathBlock);
        });
        logger.info("Relaxed to " + res);
        return res;
    }

    /**
     * TODO REMOVE
     * Simulates answering a to a Query containing a single Element
     *
     * @param element The element to answer
     * @param model   The graph to answer the element in
     * @return The match-set of the query
     * @see Table#toResultSet()
     */
    public static Table ans(Element element, CollectionsModel model) {
        return null;
    }
}

