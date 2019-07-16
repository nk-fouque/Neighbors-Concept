package implementation.utils;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.table.TableN;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.engine.binding.BindingUtils;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ElementUtils {
    private static Logger logger = Logger.getLogger(ElementUtils.class);

    /**
     * Creates a String for the SPARQL Query given Jena Variables and Elements
     *
     * @param vars     The List of Jena Variables to be put in the head of the Query
     * @param elements The List of Jena Elements to be put in the body of the Query
     * @return The String for the SPARQL Query
     */
    public static String getSelectStringFrom(List<Var> vars, List<Element> elements) {
        StringBuilder queryString = new StringBuilder("SELECT DISTINCT");
        for (Var v : ListUtils.removeDuplicates(vars)) {
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
     * If the string already has a key associated with it, returns it, else keeps generating random key until it finds one that isn't used
     *
     * @return The key associated with given uri
     */
    public static Var varKey(String uri, Map<String, Var> keys) {
        Var key;
        if (keys.containsKey(uri)) {
            key = keys.get(uri);
        } else {
            do {
                key = newVar();
            }
            while (keys.containsKey(key));
            keys.put(uri, key);
        }
        return key;
    }

    /**
     * @return a Var with a random name
     */
    private static Var newVar() {
        String varName = new RandomString(8).nextString();
        return Var.alloc(Var.alloc(varName));
    }

    /**
     * @return null if the element is completely disconnected from the Cluster
     */
    public static List<Var> mentioned(Element element) {
        List<Var> varE = new ArrayList<>();
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
     * @param keys   The keys used until now to describe certain uris
     * @return A list of all the new Query elements obtained from relaxing the filter (relax(e))
     */
    public static List<Element> relaxFilter(ElementFilter filter, CollectionsModel model, Map<String, Var> keys) {
        ExprFunction f = filter.getExpr().getFunction();
        List<Element> list = new ArrayList<>();
        if (f instanceof E_Equals) {
            for (Expr expr : f.getArgs()) {
                if (expr instanceof NodeValueNode) {
                    logger.info("Relaxing : " + expr);
                    list.addAll(ElementUtils.describeNode((expr).toString().replaceAll("<", "").replaceAll(">", ""), model, keys));
                } else {
                    logger.info(expr+" not NodeValueNode");
                }
            }
        } else {
            logger.info(f+" not E_Equals");
        }

        List<Element> res = new ArrayList<>();
        for (Element e : list) {
            if (!(e instanceof ElementFilter)) { //TODO For now the algorithm doesn't add new filters, which means it only uses a depth of one to describe the node
                // This if can be easily commented but further extends runtime
                res.add(e);
            }
        }
        logger.info("relaxed to "+res);
        return res;
    }

    /**
     * @param uri          The uri of the Node to be described
     * @param model        The Model to use to describe the Node
     * @param varsOccupied The keys used until now to describe certain uris
     * @return A list of Query elements (triple pattern and Filters) describing the Nodes known properties
     */
    public static List<Element> describeNode(String uri, CollectionsModel model, Map<String, Var> varsOccupied) {
        List<Element> res = new ArrayList<>();
        Map<Property, List<RDFNode>> propertiesFrom = model.triplesSimple.get(uri);
        if (propertiesFrom != null) {
            for (Property property : propertiesFrom.keySet()) {
                List<RDFNode> objects = propertiesFrom.get(property);
                for (RDFNode object : objects) {
                    if (property.equals(RDF.type)) {
                        ElementPathBlock pathBlock = new ElementPathBlock();
                        pathBlock.addTriple(Triple.create(varKey(uri, varsOccupied), property.asNode(), object.asNode()));
                        res.add(pathBlock);
                    } else {
                        Var var;
                        if (object.isURIResource()) {
                            var = varKey(object.asResource().getURI(), varsOccupied);
                        } else {
                            var = varKey(object.asLiteral().toString(), varsOccupied);
                        }
                        ElementPathBlock triple = new ElementPathBlock();
                        triple.addTriple(Triple.create(varKey(uri, varsOccupied), property.asNode(), var));
                        res.add(triple);
                        ElementFilter filter = new ElementFilter(new E_Equals(new ExprVar(var), new NodeValueNode(object.asNode())));
                        res.add(filter);
                    }
                }
            }
        }
        Map<Property, List<RDFNode>> propertiesTo = model.triplesSimpleReversed.get(uri);
        if (propertiesTo != null) {
            for (Property property : propertiesTo.keySet()) {
                List<RDFNode> subjects = propertiesTo.get(property);
                for (RDFNode subject : subjects) {
                    if (!property.equals(RDF.type)) {
                        Var var;
                        if (subject.isURIResource()) {
                            var = varKey(subject.asResource().getURI(), varsOccupied);
                        } else {
                            var = varKey(subject.asLiteral().toString(), varsOccupied);
                        }
                        ElementPathBlock triple = new ElementPathBlock();
                        triple.addTriple(Triple.create(var, property.asNode(), varKey(uri, varsOccupied)));
                        res.add(triple);
                        ElementFilter filter = new ElementFilter(new E_Equals(new ExprVar(var), new NodeValueNode(subject.asNode())));
                        res.add(filter);
                    }
                }
            }
        }
        return res;
    }

    /**
     * RDF:type is to be relaxed differently than other properties
     *
     * @param triple A triple pattern with predicate RDF:type
     * @param model  The model in which to search for successors
     * @return A list of all the new Query elements obtained from relaxing the triple pattern (relax(e))
     */
    public static List<Element> relaxClass(TriplePath triple, CollectionsModel model) {
        logger.info("Relaxing " + triple);
        List<RDFNode> list = model.subClassOf.get(triple.getObject().toString());
        logger.info("Subclasses " + model.subClassOf + "Found  :" + list);
        List<Element> res = new ArrayList<>();
        if (list != null) {
            for (RDFNode successor : list) {
                ElementPathBlock pathBlock = new ElementPathBlock();
                pathBlock.addTriple(Triple.create(triple.getSubject(), triple.getPredicate(), successor.asNode()));
                res.add(pathBlock);
            }
        }
        logger.info("Relaxed to " + res);
        return res;
    }

    /**
     * @param triple A triple pattern
     * @param model  The model in which to search for successors
     * @return A list of all the new Query elements obtained from relaxing the triple pattern (relax(e))
     */
    public static List<Element> relaxProperty(TriplePath triple, CollectionsModel model) {
        List<RDFNode> list = model.subPropertyOf.get(triple.getPredicate().toString());
        List<Element> res = new ArrayList<>();
        if (list != null) {
            for (RDFNode successor : list) {
                ElementPathBlock pathBlock = new ElementPathBlock();
                pathBlock.addTriple(Triple.create(triple.getSubject(), successor.asNode(), triple.getObject()));
                res.add(pathBlock);
            }
        }
        return res;
    }

    /**
     * TODO
     * @param element
     * @param colMd
     * @return
     */
    public static Table ans(Element element,CollectionsModel colMd){
        logger.debug("answering :"+element);
        List<Binding> solutionsList = new ArrayList<>();
        Table res = new TableN();
        if(element instanceof ElementFilter){
            logger.debug("is filter");
            Expr expr = ((ElementFilter) element).getExpr();
            if (expr instanceof E_Equals) {
                Var var = ((ExprVar) ((E_Equals) expr).getArg1()).asVar();
                Node node = ((NodeValue) ((E_Equals) expr).getArg2()).asNode();
                BindingHashMap bind = new BindingHashMap();
                bind.add(var,node);
                res.addBinding(bind);
            }
        } else if (element instanceof ElementPathBlock){
            logger.debug("is pathblock");
            Var subjVar = (Var) ((ElementPathBlock) element).getPattern().get(0).getSubject();
            // Subject is always a variable because triples are obtained from describeNode
            Node predicate = ((ElementPathBlock) element).getPattern().get(0).getPredicate();
            Node object = ((ElementPathBlock) element).getPattern().get(0).getObject();
            logger.debug("predicate : " + predicate);
            if (object.isVariable()) {
                // Object can be something other than a Variable if we are describing classes by their members and subclasses
                Var objVar = (Var) object;
                logger.debug("against variable");
                for(RDFNode subj : colMd.predicates.get(predicate.toString()).keySet()){
                    for (RDFNode obj : colMd.predicates.get(predicate.toString()).get(subj)){
                        BindingHashMap bind = new BindingHashMap();
                        bind.add(subjVar,subj.asNode());
                        bind.add(objVar,obj.asNode());
                        res.addBinding(bind);
                    }
                }
            } else {
                if (object.isURI()) {
                    logger.debug("object is uri");
                    RDFNode objNode = new ResourceImpl(object.getURI());
                    for(RDFNode subj : colMd.predicatesReversed.get(predicate.toString()).get(objNode)){
                        BindingHashMap bind = new BindingHashMap();
                        bind.add(subjVar,subj.asNode());
                        res.addBinding(bind);
                    }
                }
            }
        }
        return res;
    }

    /**
     * @deprecated Same as {@link #relaxFilter(ElementFilter, CollectionsModel, Map)} but very inefficient because it uses an ARQ query
     */
    @Deprecated
    public static List<Element> relaxFilter(ElementFilter filter, Model graph, Map<String, Var> keys) {
        ExprFunction f = filter.getExpr().getFunction();
        List<Element> list = new ArrayList<>();
        if (f instanceof E_Equals) {
            for (Expr expr : f.getArgs()) {
                if (expr instanceof NodeValueNode) {
                    logger.info("Relaxing : " + expr);
                    list.addAll(ElementUtils.describeNode(((ExprNode) expr).toString().replaceAll("<", "").replaceAll(">", ""), graph, keys));
                }
            }
        }
        List<Element> res = new ArrayList<>();
        for (Element e : list) {
            if (!(e instanceof ElementFilter)) {
                res.add(e);
            }
        }
        return res;
    }

    /**
     * @deprecated Same as {@link #relaxClass(TriplePath, CollectionsModel)} but very inefficient because it uses an ARQ query
     */
    @Deprecated
    public static List<Element> relaxClass(TriplePath triple, Model graph) {
        logger.info("Relaxing :" + triple);
        List<Element> list = new ArrayList<>();
        String queryString = "SELECT ?s ?p ?o WHERE {" +
                "?s ?p ?o. FILTER(?s = <" + triple.getObject() + ">). FILTER(?p = <" + RDFS.subClassOf + ">). }";
        QueryExecution qeExt = QueryExecutionFactory.create(QueryFactory.create(queryString), graph);
        ResultSet rs = qeExt.execSelect();
        while (rs.hasNext()) {
            Binding b = rs.nextBinding();
            Node successor = b.get(Var.alloc("o"));
            ElementPathBlock pathBlock = new ElementPathBlock();
            pathBlock.addTriple(Triple.create(triple.getSubject(), RDF.type.asNode(), successor));
            list.add(pathBlock);
            logger.info("Relaxed to " + successor);
        }
        return list;
    }

    /**
     * @deprecated Same as {@link #relaxProperty(TriplePath, CollectionsModel)} but very inefficient because it uses an ARQ query
     */
    @Deprecated
    public static List<Element> relaxProperty(TriplePath triple, Model graph) {
        logger.info("Relaxing :" + triple);
        List<Element> list = new ArrayList<>();
        String queryString = "SELECT ?s ?p ?o WHERE {" +
                "?s ?p ?o. FILTER(?s = <" + triple.getPredicate() + ">). FILTER(?p = <" + RDFS.subPropertyOf + ">). }";
        QueryExecution qeExt = QueryExecutionFactory.create(QueryFactory.create(queryString), graph);
        ResultSet rs = qeExt.execSelect();
        while (rs.hasNext()) {
            Binding b = rs.nextBinding();
            Node successor = b.get(Var.alloc("o"));
            ElementPathBlock pathBlock = new ElementPathBlock();
            pathBlock.addTriple(Triple.create(triple.getSubject(), successor, triple.getObject()));
            list.add(pathBlock);
            logger.info("Relaxed to " + successor);
        }
        return list;
    }

    /**
     * @deprecated Same as {@link #describeNode(String, CollectionsModel, Map)} but very inefficient because it uses an ARQ query
     */
    @Deprecated
    public static List<Element> describeNode(String uri, Model graph, Map<String, Var> varsOccupied) {
        uri = graph.expandPrefix(uri);
        String queryString = "SELECT ?s ?p ?o WHERE {" +
                "?s ?p ?o. FILTER(?s = <" + uri + ">).}";
//        logger.debug(queryString);
        QueryExecution qeExt = QueryExecutionFactory.create(QueryFactory.create(queryString), graph);
        ResultSet rs = qeExt.execSelect();
        List<Element> list = new ArrayList<>();
        while (rs.hasNext()) {
            Binding b = rs.nextBinding();
//            System.out.println(b);
            Node subject = b.get(Var.alloc("s"));
            Node property = b.get(Var.alloc("p"));
            Node object = b.get(Var.alloc("o"));
//            logger.debug(subject+" "+property+" "+object);
            if (property.equals(RDF.type.asNode())) {
                ElementPathBlock element = new ElementPathBlock();
                element.addTriple(Triple.create(varKey(uri, varsOccupied), property, object));
                list.add(element);
            } else {
                Var var;
                if (object.isURI()) {
                    var = varKey(object.getURI(), varsOccupied);
                } else {
                    var = varKey(object.getLiteral().toString(), varsOccupied);
                }
                ElementPathBlock triple = new ElementPathBlock();
                triple.addTriple(Triple.create(varKey(uri, varsOccupied), property, var));
                list.add(triple);
                ElementFilter filter = new ElementFilter(new E_Equals(new ExprVar(var), new NodeValueNode(object)));
                list.add(filter);
            }
        }
        queryString = "SELECT ?s ?p ?o WHERE {" +
                "?s ?p ?o. FILTER(?o = <" + uri + ">).}";
        QueryExecution qeInt = QueryExecutionFactory.create(QueryFactory.create(queryString), graph);
        rs = qeInt.execSelect();
        while (rs.hasNext()) {
            Binding b = rs.nextBinding();
//            System.out.println(b);
            Node subject = b.get(Var.alloc("s"));
            Node property = b.get(Var.alloc("p"));
            Node object = b.get(Var.alloc("o"));
//            System.out.println(subject+" "+property+" "+object);
            Var var = varKey(subject.getURI(), varsOccupied);
            ElementPathBlock triple = new ElementPathBlock();
            triple.addTriple(Triple.create(var, property, varKey(uri, varsOccupied)));
            list.add(triple);
            ElementFilter filter = new ElementFilter(new E_Equals(new ExprVar(var), new NodeValueNode(subject)));
            list.add(filter);
        }
        return list;
    }

    /**
     * Used to be used in the extension (now calculated by {@link TableUtils#ext(Table, Element, CollectionsModel)} )
     * removed because it was very inefficient and it still required a join with he mapping
     */
    @Deprecated
    public static Table ans(List<Var> vars, List<Element> elements, Model graph) {
        List<QuerySolution> solutionsList;
        Table res = new TableN();
        SingletonStopwatchCollection.resume("querying");
        String queryString = getSelectStringFrom(vars, elements);
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
}
