package implementation.utils;

import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.table.TableN;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.*;
import org.apache.jena.sparql.engine.join.QueryIterHashJoin;
import org.apache.jena.sparql.expr.E_Equals;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
     *
     * @param vars
     * @param element
     * @param model
     * @param mapping
     * @return
     */
    public static Table ans(List<Var> vars, Element element, CollectionsModel model, Table mapping) {
        logger.debug("answering : " + element);
        List<Binding> solutionsList = new ArrayList<>();
        Table res = new TableN();
        if (element instanceof ElementFilter) {
            logger.debug("is filter");
            Expr expr = ((ElementFilter) element).getExpr();
            if (expr instanceof E_Equals) {
                Var var = Var.alloc(((E_Equals) expr).getArg1().toString().replace("?", ""));
                NodeValue node = (NodeValue) ((E_Equals) expr).getArg2();
                Iterator<Binding> iter = mapping.rows();
                iter.forEachRemaining((b) -> {
                    if (b.get(var).toString().equals(node.asString())) {
                        res.addBinding(b);
                        logger.debug("matched :" + b);
                    } else {
                        logger.debug("did not match : " + b);
                    }
                });
            }
        } else if (element instanceof ElementPathBlock) {
            logger.debug("is pathblock");
            Var var = (Var) ((ElementPathBlock) element).getPattern().get(0).getSubject();
            // Subject is always a variable because triples are obtained from describeNode
            Node predicate = ((ElementPathBlock) element).getPattern().get(0).getPredicate();
            Node object = ((ElementPathBlock) element).getPattern().get(0).getObject();
            logger.debug("predicate : " + predicate);
            if (object.isVariable()) {
                logger.debug("against variable");
                // Object can be something other than a Variable if we are describing classes by their members and subclasses
                Var objVar = (Var) object;
                if (mapping.getVars().contains(var)) {
                    logger.debug("subject connected");
                    if (mapping.getVars().contains(objVar)) {
                        logger.debug("object connected");
                        Iterator<Binding> iter = mapping.rows();
                        iter.forEachRemaining((b) -> {
                            if (b.get(var).isURI()&&b.get(objVar).isURI()) {
                                RDFNode bindSubject = new ResourceImpl(b.get(var).getURI());
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
                            RDFNode bindSubject = new ResourceImpl(b.get(var).getURI());
                            logger.debug("found " + bindSubject + " at " + var + " in " + b);
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
                                    bind2.add(var, subj.asNode());
                                    res.addBinding(bind2);
                                }
                            }else logger.debug("not in this predicate");
                        });
                    }
                    // No need for else because there has to be at least one that's connected so for now it's always true
                    //TODO If one day we can relax predicates as filters, this will change
                }
            } else {
                logger.debug("subject is variable");
                if (object.isURI()) {
                    logger.debug("object is uri");
                    RDFNode objNode = new ResourceImpl(object.getURI());
                    if (mapping.getVars().contains(var)) {
                        logger.debug(var + " connected");
                        Iterator<Binding> iter = mapping.rows();
                        iter.forEachRemaining((b) -> {
                            RDFNode bindSubject = new ResourceImpl(b.get(var).getURI());
                            logger.debug("found " + bindSubject + " at " + var + " in " + b);
                            if (model.predicates.get(predicate.toString()).keySet().contains(bindSubject)) {
                                if (model.predicates.get(predicate.toString()).get(bindSubject).contains(objNode)) {
                                    res.addBinding(b);
                                    logger.debug("found "+objNode+" in predicates");
                                }
                            } else logger.debug("not in this predicate");
                        });
                    } // this should always be true or else there wouldn't be a connected variable
                    // TODO Same thing
                }
            }
            for (Binding b : solutionsList) {
                res.addBinding(b);
            }
        }
        return res;
    }

    @Deprecated
    public static Table ans(List<Var> vars, List<Element> elements, Model graph) {
        List<QuerySolution> solutionsList;
        Table res = new TableN();
        SingletonStopwatchCollection.resume("querying");
        String queryString = ElementUtils.getSelectStringFrom(vars, elements);
        logger.info(queryString);
        Query qAnsE = QueryFactory.create(queryString);
        ResultSet resultSet = QueryExecutionFactory.create(qAnsE, graph).execSelect();
        SingletonStopwatchCollection.stop("querying");
        SingletonStopwatchCollection.resume("formatting");
        solutionsList = ResultSetFormatter.toList(resultSet);
        for (QuerySolution qs : solutionsList) {
            res.addBinding(BindingUtils.asBinding(qs));
        }
        SingletonStopwatchCollection.stop("formatting");
        return res;
    }

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

}
