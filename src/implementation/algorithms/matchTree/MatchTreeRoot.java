package implementation.algorithms.matchTree;

import implementation.utils.CollectionsModel;
import implementation.utils.profiling.stopwatches.SingletonStopwatchCollection;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.table.TableN;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.syntax.Element;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * The root node of a match-tree
 *
 * @author nk-fouque
 */
public class MatchTreeRoot extends MatchTreeNode {
    public static Logger logger = Logger.getLogger(MatchTreeNode.class);

    /**
     * Base Constructor
     *
     * @param top   A list with the variables to be returned in the answer, in the basic implementation, only one Var is relevant here
     * @param colMd The RDF Graph to work in
     */
    public MatchTreeRoot(List<Var> top, CollectionsModel colMd) {
        super();
        element = null;
        varE = new ArrayList<>();
        D = new ArrayList<>(top);

        Table init = new TableN();
        ResIterator data = colMd.getGraph().listSubjects();
        data.forEachRemaining((Resource resource) -> {
            for (Var var : top) {
                init.addBinding(BindingFactory.binding(var, resource.asNode()));
            }
        });
        matchSet = init;

        delta = new ArrayList<>(top);

        children = new ArrayList<>();
    }

    /**
     * Constructor for copies
     *
     * @param other The MatchTreeRoot to copy
     */
    public MatchTreeRoot(MatchTreeNode other) {
        super();
        element = null;
        varE = new ArrayList<>();
        D = new ArrayList<>(other.getD());

        matchSet = other.getMatchSet();
        delta = new ArrayList<>(other.getDelta());

        children = new ArrayList<>(other.getChildren());

    }

    @Override
    public String toString() {
        return super.toString(0);
    }

    /**
     * @return A Table containing all the proper answers for the Cluster
     */
    public Table getMatchSet() {
        return matchSet;
    }

    /**
     * Applies The Lazy Joins Algorithm to insert a new node
     *
     * @param element   The element defining the new node
     * @param colMd     The RDF Graph to work in
     * @param varPprime The variables already defined in the cluster
     * @return A copy of this tree with the new element
     * @see MatchTreeNode#lazyJoin(MatchTreeRoot, MatchTreeNode)
     */
    public MatchTreeRoot lazyJoin(Element element, CollectionsModel colMd, List<Var> varPprime) {
        MatchTreeNode newnode = new MatchTreeNode(element, colMd, varPprime);
//        ResultSetFormatter.out(System.out,newnode.matchSet.toResultSet());
        SingletonStopwatchCollection.resume("lazyjoin");
        LazyJoin res = this.lazyJoin(this, newnode);
        SingletonStopwatchCollection.stop("lazyjoin");
        return new MatchTreeRoot(res.copy);
    }
}
