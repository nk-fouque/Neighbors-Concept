package implementation.utils.elements;

import implementation.algorithms.Cluster;
import implementation.utils.CollectionsModel;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_Exists;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public abstract class QueryElement implements Comparable {
    public static Logger logger = Logger.getLogger(QueryElement.class);
    protected Element element;
    protected CollectionsModel model;

    abstract Table answer();

    public Table ans(){
        logger.debug("answering :" + element);
        Table knownAns = model.ans(this);
        if (knownAns != null) {
            return knownAns;
        } else {
            Table res = answer();
            model.addAns(this,res);
            return res;
        }
    }

    public Element getElement() {
        return element;
    }

    public int getDepth(){
        return model.getDepth().get(this);
    }

    public Set<QueryElement> relax(int maxDepth){
        return Collections.emptySet();
    }

    /**
     * @return The variables used in this element
     */
    public Set<Var> mentionedVars(){
        Set<Var> varE = new HashSet<>();
        if (element instanceof ElementFilter) {
            varE.addAll(((ElementFilter) element).getExpr().getVarsMentioned());
        } else // if (element instanceof ElementPathBlock)
        {
            varE.addAll((new E_Exists(element)).getVarsMentioned());
        }
        return varE;
    }

    public static QueryElement create(Element element,CollectionsModel collectionsModel,int depth){
        QueryElement res;
        if (element instanceof ElementFilter){
            res = new FilterElement((ElementFilter) element,collectionsModel);
        } else if (element instanceof ElementPathBlock){
            if (((ElementPathBlock) element).getPattern().get(0).getPredicate().equals(RDF.type.asNode())){
                res = new ClassElement((ElementPathBlock) element,collectionsModel);
            } else {
                res = new TriplePatternElement((ElementPathBlock) element,collectionsModel);
            }
        } else res=null;
        collectionsModel.setDepth(res,depth);
        return res;
    }

    public String toString() {
        return element.toString();
    }

    @Override
    public int hashCode() {
        return element.hashCode();
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof QueryElement){
            return compareTo((QueryElement)o);
        } else return 0;
    }

    public int compareTo(QueryElement other) {
        int res = Comparator.comparingInt(queryElement -> {
            if (queryElement instanceof FilterElement)
                return -1;
            else
                return 1;
        })
                .thenComparingInt(queryElement -> ((QueryElement)queryElement).getDepth())
                .thenComparingInt(queryElement -> {
            if (queryElement instanceof ClassElement)
                return -1;
            else
                return 1;
        })
                .thenComparing(Object::toString)
                .compare(this, other);

        return res;
    }

    public Set<FilterElement> separate(){
        return Collections.emptySet();
    }

    public int getUsage(Cluster c){
        return model.getUsage(this,c);
    }

    public void use(Cluster c){
        model.use(this,c);
    }
}
