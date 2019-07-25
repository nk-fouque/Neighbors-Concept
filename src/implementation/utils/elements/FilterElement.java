package implementation.utils.elements;

import implementation.utils.CollectionsModel;
import implementation.utils.ElementUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.table.TableN;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementFilter;

import java.util.HashSet;
import java.util.Set;

public class FilterElement extends QueryElement {
    public FilterElement(ElementFilter filter, CollectionsModel colMd){
        element = filter;
        model = colMd;
    }

    @Override
    public Set<QueryElement> relax(int maxDepth){
        Set<QueryElement> res = new HashSet<>();

        ExprFunction f = ((ElementFilter) element).getExpr().getFunction();
        Set<Element> list = new HashSet<>();
        if (f instanceof E_Equals) {
            for (Expr expr : f.getArgs()) {
                if (expr instanceof NodeValueNode) {
                    logger.info("Relaxing filter : " + expr);
                    list.addAll(ElementUtils.describeNode((expr).toString().replaceAll("<", "").replaceAll(">", ""), model));
                } else {
                    logger.info(expr + " not NodeValueNode");
                }
            }
        } else {
            logger.info(f + " not E_Equals");
        }

        Set<Element> elements = new HashSet<>();
        for (Element e : list) {
            if (!(e instanceof ElementFilter)) {
                if (getDepth() < maxDepth) {
                    // TODO For now any number>2 = infinite
                    elements.add(e);
                }
            }
        }
        logger.info("relaxed to " + elements);

        for(Element element : elements){
            if (!(element instanceof ElementFilter) || getDepth()<maxDepth)
            res.add(QueryElement.create(element,model,getDepth()+1));
        }
        return res;
    }

    @Override
    protected Table answer() {
        Table res = new TableN();
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
        }
        return res;
    }
}
