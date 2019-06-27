package v2.utils;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarAlloc;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.vocabulary.RDF;
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

public class ElementUtils {
    private static Logger logger = Logger.getLogger(ElementUtils.class);
    /** Creates a String for the SPARQL Query given Jena Variables and Elements
     * @param vars The List of Jena Variables to be put in the head of the Query
     * @param elements The List of Jena Elements to be put in the body of the Query
     * @return The String for the SPARQL Query */
    public static String getSelectStringFrom(List<Var> vars, List<Element> elements){
        StringBuilder queryString= new StringBuilder("SELECT DISTINCT");
        for (Var v : ListUtils.removeDuplicates(vars)){
            queryString.append(" ").append(v.toString());
        }
        queryString.append(" WHERE {");
        for (Element element : elements){
            queryString.append("\n").append(element.toString()).append(".");
        }
        queryString.append("}");
        return queryString.toString();
    }

    /**
     *
     * @param uri
     * @param keys
     * @return
     */
    public static Var varKey(String uri,Map<String,Var> keys){
        Var key;
        if(keys.containsKey(uri)){
            key = keys.get(uri);
        }else{
            do{key = newVar();}
            while (keys.containsValue(key));
            keys.put(uri,key);
        }
        return key;
    }

    /**
     *
     * @return
     */
    private static Var newVar() {
        String varName = new RandomString(8).nextString();
        return Var.alloc(Var.alloc(varName));
    }

    /**
     * @return A list of every variables mentioned by the element that are mentioned to this Cluster
     * @return null if the element is completely disconnected from the Cluster
     */
    public static List<Var> mentioned(Element element){
        List<Var> varE = new ArrayList<>();
        if (element instanceof ElementFilter) {
            varE.addAll(((ElementFilter) element).getExpr().getVarsMentioned());
        } else // if (element instanceof ElementPathBlock)
            {
            varE.addAll((new E_Exists(element)).getVarsMentioned());
        }
        return varE;
    }

    public static List<Element> relaxFilter(ElementFilter filter,CollectionsModel model,Map<String,Var> keys){
        ExprFunction f = filter.getExpr().getFunction();
        List<Element> list = new ArrayList<>();
        if(f instanceof E_Equals){
            for (Expr expr : f.getArgs()){
                if (expr instanceof NodeValueNode){
                    logger.info("Relaxing : "+expr);
                    list.addAll(ElementUtils.describeNode((expr).toString().replaceAll("<","").replaceAll(">",""),model,keys));
                }
            }
        }

        List<Element> res= new ArrayList<>();
        for (Element e : list){
            if (!(e instanceof ElementFilter)){ //TODO For now the algorithm doesn't add new filters, which means it only uses a depth of one to describe the node
                res.add(e);
            }
        }
        return res;
    }

    public static List<Element> describeNode(String uri, CollectionsModel model, Map<String,Var> varsOccupied) {
        List<Element> res = new ArrayList<>();
        Map<Property,List<RDFNode>> propertiesFrom = model.triplesSimple.get(uri);
        for(Property property : propertiesFrom.keySet()){
            List<RDFNode> objects = propertiesFrom.get(property);
            for(RDFNode object : objects ){
                if (property.equals(RDF.type)){
                    ElementPathBlock pathBlock = new ElementPathBlock();
                    pathBlock.addTriple(Triple.create(varKey(uri,varsOccupied), property.asNode(), object.asNode()));
                    res.add(pathBlock);
                } else {
                    Var var;
                    if (object.isURIResource()) {
                        var = varKey(object.asResource().getURI(), varsOccupied);
                    } else {
                        var = varKey(object.asLiteral().toString(),varsOccupied);
                    }
                    ElementPathBlock triple = new ElementPathBlock();
                    triple.addTriple(Triple.create(varKey(uri, varsOccupied), property.asNode(), var));
                    res.add(triple);
                    ElementFilter filter = new ElementFilter(new E_Equals(new ExprVar(var), new NodeValueNode(object.asNode())));
                    res.add(filter);
                }
            }
        }
        Map<Property,List<RDFNode>> propertiesTo = model.triplesSimple.get(uri);
        for(Property property : propertiesTo.keySet()){
            List<RDFNode> subjects = propertiesTo.get(property);
            for(RDFNode subject : subjects ){
                if(!property.equals(RDF.type)) {
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
        return res;
    }

    public static List<Element> relaxClass(TriplePath triple, CollectionsModel model){
        logger.info("Relaxing "+triple);
        List<RDFNode> list = model.subClassOf.get(triple.getObject().toString());
        logger.info("Subclasses "+model.subClassOf+"Found  :"+list);
        List<Element> res = new ArrayList<>();
        if(list!=null) {
            for (RDFNode successor : list) {
                ElementPathBlock pathBlock = new ElementPathBlock();
                pathBlock.addTriple(Triple.create(triple.getSubject(), triple.getPredicate(), successor.asNode()));
                res.add(pathBlock);
            }
        }
        logger.info("Relaxed to "+res);
        return res;
    }

    public static List<Element> relaxProperty(TriplePath triple, CollectionsModel model){
        List<RDFNode> list = model.subPropertyOf.get(triple.getPredicate().toString());
        List<Element> res = new ArrayList<>();
        if (list!=null){
            for (RDFNode successor : list){
                ElementPathBlock pathBlock = new ElementPathBlock();
                pathBlock.addTriple(Triple.create(triple.getSubject(),successor.asNode(),triple.getObject()));
                res.add(pathBlock);
            }
        }
        return res;
    }

    @Deprecated
    public static List<Element> relaxFilter(ElementFilter filter,Model graph,Map<String,Var> keys) {
        ExprFunction f = filter.getExpr().getFunction();
        List<Element> list = new ArrayList<>();
        if(f instanceof E_Equals){
            for (Expr expr : f.getArgs()){
                if (expr instanceof NodeValueNode){
                    logger.info("Relaxing : "+expr);
                    list.addAll(ElementUtils.describeNode(((ExprNode)expr).toString().replaceAll("<","").replaceAll(">",""),graph,keys));
                }
            }
        }
        List<Element> res= new ArrayList<>();
        for (Element e : list){
            if (!(e instanceof ElementFilter)){
                res.add(e);
            }
        }
        return res;
    }

    @Deprecated
    public static List<Element> relaxClass(TriplePath triple, Model graph){
        logger.info("Relaxing :"+triple);
        List<Element> list = new ArrayList<>();
        String queryString = "SELECT ?s ?p ?o WHERE {" +
                "?s ?p ?o. FILTER(?s = <" + triple.getObject() + ">). FILTER(?p = <"+ RDFS.subClassOf+">). }";
        QueryExecution qeExt = QueryExecutionFactory.create(QueryFactory.create(queryString), graph);
        ResultSet rs = qeExt.execSelect();
        while (rs.hasNext()){
            Binding b = rs.nextBinding();
            Node successor = b.get(Var.alloc("o"));
            ElementPathBlock pathBlock = new ElementPathBlock();
            pathBlock.addTriple(Triple.create(triple.getSubject(),RDF.type.asNode(),successor));
            list.add(pathBlock);
            logger.info("Relaxed to "+successor);
        }
        return list;
    }

    @Deprecated
    public static List<Element> relaxProperty(TriplePath triple, Model graph){
        logger.info("Relaxing :"+triple);
        List<Element> list = new ArrayList<>();
        String queryString = "SELECT ?s ?p ?o WHERE {" +
                "?s ?p ?o. FILTER(?s = <" + triple.getPredicate() + ">). FILTER(?p = <"+ RDFS.subPropertyOf+">). }";
        QueryExecution qeExt = QueryExecutionFactory.create(QueryFactory.create(queryString), graph);
        ResultSet rs = qeExt.execSelect();
        while (rs.hasNext()){
            Binding b = rs.nextBinding();
            Node successor = b.get(Var.alloc("o"));
            ElementPathBlock pathBlock = new ElementPathBlock();
            pathBlock.addTriple(Triple.create(triple.getSubject(),successor,triple.getObject()));
            list.add(pathBlock);
            logger.info("Relaxed to "+successor);
        }
        return list;
    }

    @Deprecated
    public static List<Element> describeNode(String uri, Model graph, Map<String,Var> varsOccupied) {
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
                element.addTriple(Triple.create(varKey(uri,varsOccupied), property, object));
                list.add(element);
            } else {
                Var var;
                if (object.isURI()) {
                    var = varKey(object.getURI(), varsOccupied);
                } else {
                    var = varKey(object.getLiteral().toString(),varsOccupied);
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
            Var var = varKey(subject.getURI(),varsOccupied);
            ElementPathBlock triple = new ElementPathBlock();
            triple.addTriple(Triple.create(var, property, varKey(uri,varsOccupied)));
            list.add(triple);
            ElementFilter filter = new ElementFilter(new E_Equals(new ExprVar(var), new NodeValueNode(subject)));
            list.add(filter);
        }
        return list;
    }
}
