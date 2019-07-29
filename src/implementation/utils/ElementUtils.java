package implementation.utils;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.table.TableN;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.expr.*;
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
     * @return The variables used in this element
     */
    public static Set<Var> mentioned(Element element) {
        Set<Var> varE = new HashSet<>();
        if (element instanceof ElementFilter) {
            varE.addAll(((ElementFilter) element).getExpr().getVarsMentioned());
        } else // if (element instanceof ElementPathBlock)
        {
            varE.addAll((new E_Exists(element)).getVarsMentioned());
        }
        return varE;
    }

    /**
     * Relaxes a filter
     *
     * @param filter The filter to relax
     * @param model  The model in which to describe what has to be relaxed
     * @return A list of all the new Query elements obtained from relaxing the filter (relax(e))
     */
    public static Set<Element> relaxFilter(ElementFilter filter, CollectionsModel model, int descriptionDepth) {
        ExprFunction f = filter.getExpr().getFunction();
        Set<Element> list = new HashSet<>();
        if (f instanceof E_Equals) {
            for (Expr expr : f.getArgs()) {
                if (expr instanceof NodeValueNode) {
                    logger.info("Relaxing : " + expr);
                    list.addAll(describeNode((expr).toString().replaceAll("<", "").replaceAll(">", ""), model, model.getDepth(filter) + 1));
                } else {
                    logger.info(expr + " not NodeValueNode");
                }
            }
        } else {
            logger.info(f + " not E_Equals");
        }

        Set<Element> res = new HashSet<>();
        for (Element e : list) {
            if (!(e instanceof ElementFilter) || model.getDepth(filter) < descriptionDepth) {
                res.add(e);
            }
        }
        logger.info("relaxed to " + res);
        return res;
    }

    /**
     * @param uri   The uri of the Node to be described
     * @param model The Model to use to describe the Node
     * @return A list of Query elements (triple pattern and Filters) describing the Nodes known properties
     */
    public static Set<Element> describeNode(String uri, CollectionsModel model, int depth) {
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
                } else if (object.isLiteral()) {
                    var = model.varKey(object.asLiteral().toString());
                } else {
                    var = model.varKey(object.toString());
                }
                ElementPathBlock triple = new ElementPathBlock();
                triple.addTriple(Triple.create(model.varKey(uri), property.asNode(), var));
                res.add(triple);
                ElementFilter filter = new ElementFilter(new E_Equals(new ExprVar(var), new NodeValueNode(object.asNode())));
                model.setDepth(filter, depth);
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
                } else if (subject.isLiteral()) {
                    var = model.varKey(subject.asLiteral().toString());
                } else {
                    var = model.varKey(subject.toString());
                }
                ElementPathBlock triple = new ElementPathBlock();
                triple.addTriple(Triple.create(var, property.asNode(), model.varKey(uri)));
                res.add(triple);
                ElementFilter filter = new ElementFilter(new E_Equals(new ExprVar(var), new NodeValueNode(subject.asNode())));
                model.setDepth(filter, depth);
                res.add(filter);
            }
        });
        return res;
    }

    /**
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
            logger.info(node.toString());
            ElementPathBlock pathBlock = new ElementPathBlock();
            pathBlock.addTriple(Triple.create(triple.getSubject(), RDF.type.asNode(), node.asNode()));
            res.add(pathBlock);
        });
        logger.info("Relaxed to " + res);
        return res;
    }

    /**
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
     * Simulates answering a to a Query containing a single Element
     *
     * @param element The element to answer
     * @param model   The graph to answer the element in
     * @return The match-set of the query
     * @see Table#toResultSet()
     */
    public static Table ans(Element element, CollectionsModel model) {
        logger.debug("answering :" + element);
        Table knownAns = model.ans(element);
        if (knownAns != null) {
            return knownAns;
        } else {
            final Table res = new TableN();
            if (element instanceof ElementFilter) {
                logger.debug("is filter");
                Expr expr = ((ElementFilter) element).getExpr();
                if (expr instanceof E_Equals) {
                    Var var = ((ExprVar) (((E_Equals) expr).getArg1())).asVar();
                    Node node = ((NodeValue) (((E_Equals) expr).getArg2())).asNode();
                    BindingHashMap bind = new BindingHashMap();
                    bind.add(var, node);
                    res.addBinding(bind);
                }
            } else if (element instanceof ElementPathBlock) {
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
            }
            model.addAns(element, res);
            return res;
        }
    }
}

