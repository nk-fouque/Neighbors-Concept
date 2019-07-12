package implementation.utils;

import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.table.TableN;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.engine.binding.BindingUtils;
import org.apache.jena.sparql.engine.join.QueryIterHashJoin;
import org.apache.jena.sparql.expr.E_Equals;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.log4j.Logger;

import java.util.*;

public class TableUtils {
    private static Logger logger = Logger.getLogger(TableUtils.class);

    /**
     * Uses Jena's iterator join to join two tables
     */
    public static TableN simpleJoin(Table left, Table right) {
//        System.out.println("Joining : \n"+left+" and \n"+right);
        return new TableN(QueryIterHashJoin.create(left.iterator(null), right.iterator(null), null));
    }

    /**
     * Algebric table projection
     *
     * @param table The table to project
     * @param vars  A list of variables to do the projection on
     */
    public static TableN projection(Table table, List<Var> vars) {
        TableN res = new TableN();
        QueryIterator iter = table.iterator(null);
        Binding b;
        while (iter.hasNext()) {
            b = iter.nextBinding();
            for (Var var : vars) {
                res.addBinding(BindingFactory.binding(var, b.get(var)));
            }
        }
        return removeDuplicates(res);
    }

    /**
     * Algebric Table difference
     *
     * @param left
     * @param right
     * @return The left table from which all elements appearing in the right table have been removed
     */
    public static TableN difference(Table left, Table right) {
        TableN res = new TableN();
        Iterator<Binding> iterLeft = left.rows();
        List<Binding> listLeft = new ArrayList<>();
        while (iterLeft.hasNext()) {
            listLeft.add(iterLeft.next());
        }
        Iterator<Binding> iterRight = right.rows();
        List<Binding> listRight = new ArrayList<>();
        while (iterRight.hasNext()) {
            listRight.add(iterRight.next());
        }

        for (Binding b : listLeft) {
            if (!listRight.contains(b)) {
                res.addBinding(b);
            }
        }
        return res;
    }

    /**
     * @param table
     * @return The same Table from which the duplicate lines have been removed
     */
    public static TableN removeDuplicates(Table table) {
        Set<Binding> temp = new HashSet<>();
        Iterator<Binding> iter = table.rows();
        while (iter.hasNext()) {
            temp.add(iter.next());
        }
        TableN res = new TableN();
        for (Binding b : temp) {
            res.addBinding(b);
        }
        return res;
    }

    /**
     * Applies ext(M,e,G)
     * if e is a filter, ext(M,e,G) is just a selection on M where the filter is satisfied
     * if e is a triple pattern, ext(M,e,G) := [ M (join) ans(var(e) <-- {e},G) ]
     *
     * @param mapping The Table representing the mapping M
     * @param element The Element e
     * @param model   The CollectionsModel containing the graph G
     * @return
     * @deprecated Less efficient than a {@link ElementUtils#ans(Element, CollectionsModel)} combines with a {@link #simpleJoin(Table, Table)} so useless, but it took me so much time to write this that I do'nt have the heart to delete it
     */
    @Deprecated
    public static Table ext(Table mapping, Element element, CollectionsModel model) {
        logger.debug("answering : " + element);
        Table res = new TableN();
        if (element instanceof ElementFilter) {
            logger.debug("is filter");
            Expr expr = ((ElementFilter) element).getExpr();
            if (expr instanceof E_Equals) {
                Var var = ((ExprVar) ((E_Equals) expr).getArg1()).asVar();
                Node node = ((NodeValue) ((E_Equals) expr).getArg2()).asNode();
                Iterator<Binding> iter = mapping.rows();
                iter.forEachRemaining((b) -> {
                    Node bindNode = b.get(var);
                    if (bindNode.equals(node)) {
                        res.addBinding(b);
                        logger.debug("matched :" + b);
                    } else {
                        logger.debug("did not match : " + b);
                    }
                });
            }
        } else if (element instanceof ElementPathBlock) {
            logger.debug("is pathblock");
            Var subjVar = (Var) ((ElementPathBlock) element).getPattern().get(0).getSubject();
            // Subject is always a variable because triples are obtained from describeNode
            Node predicate = ((ElementPathBlock) element).getPattern().get(0).getPredicate();
            Node object = ((ElementPathBlock) element).getPattern().get(0).getObject();
            logger.debug("predicate : " + predicate);
            if (object.isVariable()) {
            // Object can be something other than a Variable if we are describing classes by their members and subclasses
                logger.debug("against variable");
                Var objVar = (Var) object;
                if (mapping.getVars().contains(subjVar)) {
                    logger.debug("subject connected");
                    if (mapping.getVars().contains(objVar)) {
                        logger.debug("object connected");
                        Iterator<Binding> iter = mapping.rows();
                        iter.forEachRemaining((b) -> {
                            if (b.get(subjVar).isURI() && b.get(objVar).isURI()) {
                                RDFNode bindSubject = new ResourceImpl(b.get(subjVar).getURI());
                                RDFNode bindObject = new ResourceImpl(b.get(objVar).getURI());
                                if (model.predicates.get(predicate.toString()).keySet().contains(bindSubject)) {
                                    logger.debug("subject in this predicate");
                                    if (model.predicates.get(predicate.toString()).get(bindSubject).contains(bindObject)) {
                                        res.addBinding(b);
                                        logger.debug("matched :" + b);
                                    } else {
                                        logger.debug("did not match : " + b);
                                    }
                                } else logger.debug("not in this predicate");
                            } else logger.debug("not uri");
                        });
                    } else { // If the subject is the only connected variable
                        logger.debug("object disconnected");
                        Iterator<Binding> iter = mapping.rows();
                        iter.forEachRemaining((b) -> {
                            RDFNode bindSubject = new ResourceImpl(b.get(subjVar).getURI());
                            logger.debug("found " + bindSubject + " at " + subjVar + " in " + b);
                            if (model.predicates.get(predicate.toString()).keySet().contains(bindSubject)) {
                                logger.debug("subject in this predicate");
                                for (RDFNode obj : model.predicates.get(predicate.toString()).get(bindSubject)) {
                                    logger.debug("found " + obj);
                                    BindingHashMap bind2 = new BindingHashMap(b);
                                    bind2.add(objVar, obj.asNode());
                                    logger.debug("new binding : " + bind2);
                                    res.addBinding(bind2);
                                }
                            } else logger.debug("not in this predicate");
                        });
                    }
                } else { // If the object is the only connected variable
                    logger.debug("subject disconnected");
                    if (mapping.getVars().contains(objVar)) {
                        logger.debug("object connected");
                        Iterator<Binding> iter = mapping.rows();
                        iter.forEachRemaining((b) -> {
                            RDFNode bindObject = new ResourceImpl(b.get(objVar).getURI());
                            logger.debug("found " + bindObject + " at " + objVar + " in " + b);
                            if (model.predicatesReversed.get(predicate.toString()).keySet().contains(bindObject)) {
                                for (RDFNode subj : model.predicatesReversed.get(predicate.toString()).get(bindObject)) {
                                    logger.debug("found " + subj);
                                    BindingHashMap bind2 = new BindingHashMap(b);
                                    bind2.add(subjVar, subj.asNode());
                                    res.addBinding(bind2);
                                }
                            } else logger.debug("not in this predicate");
                        });
                    }
                    // No need for else because there has to be at least one that's connected so for now it's always true
                    // TODO If one day we can relax predicates as filters, this will change
                }
            } else {
                logger.debug("object is not variable");
                if (object.isURI()) {
                    logger.debug("object is uri");
                    RDFNode objNode = new ResourceImpl(object.getURI());
                    if (mapping.getVars().contains(subjVar)) {
                        logger.debug(subjVar + " connected");
                        Iterator<Binding> iter = mapping.rows();
                        iter.forEachRemaining((b) -> {
                            RDFNode bindSubject = new ResourceImpl(b.get(subjVar).getURI());
                            logger.debug("found " + bindSubject + " at " + subjVar + " in " + b);
                            if (model.predicates.get(predicate.toString()).keySet().contains(bindSubject)) {
                                if (model.predicates.get(predicate.toString()).get(bindSubject).contains(objNode)) {
                                    res.addBinding(b);
                                    logger.debug("found " + objNode + " in predicates");
                                }
                            } else logger.debug("not in this predicate");
                        });
                    } // this should always be true or else there wouldn't be a connected variable
                    // TODO Same as above
                }
            }
        }
        return res;
    }
}
