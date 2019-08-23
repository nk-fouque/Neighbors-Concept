package implementation.algorithms.matchTree;

import implementation.utils.CollectionsModel;
import implementation.utils.ElementUtils;
import implementation.utils.TableUtils;
import implementation.utils.profiling.stopwatches.SingletonStopwatchCollection;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.Element;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * See the article for the exact mathematical definitions of the attributes and behavior of the algorithm
 * https://hal.archives-ouvertes.fr/hal-01945454/document Ch.6
 *
 * @author nk-fouque
 */
public class MatchTreeNode {
    private static Logger logger = Logger.getLogger(MatchTreeNode.class);

    Element element;
    Set<Var> varE;
    Set<Var> D;

    Table matchSet;
    Set<Var> delta;

    Set<MatchTreeNode> children;

    boolean inserted;

    /**
     * e : the Element defined by this node
     */
    public Element getElement() {
        return element;
    }

    /**
     * var(e) : the variables mentioned by e
     */
    public Set<Var> getVarE() {
        return varE;
    }

    /**
     * D : the set of variable introduced by e
     */
    public Set<Var> getD() {
        return D;
    }

    /**
     * M : the match-set
     */
    public Table getMatchSet() {
        return matchSet;
    }

    /**
     * Δ : the sub-domain of variables useful to this node's parent
     */
    public Set<Var> getDelta() {
        return delta;
    }

    /**
     * The nodes under this node
     */
    public Set<MatchTreeNode> getChildren() {
        return children;
    }

    /**
     * Same as {@link Element#toString()} except for the top element, expressed as T(X)
     */
    public String elementString() {
        if (element == null) {
            return ("T(" + D.toString() + ")");
        } else {
            return element.toString();
        }
    }

    @Override
    public String toString() {
        return toString(0);
    }

    /**
     * Visualization of the tree using indentation
     * Still has a few issues
     */
    public String toString(int tab) {
        StringBuilder res = new StringBuilder();
        res.append("\t".repeat(Math.max(0, tab)));
        res.append("[Element : ").append(elementString());
        res.append("\n");
        res.append("\t".repeat(Math.max(0, tab + 1)));
        res.append("Children : ");
        for (MatchTreeNode nc : children) {
            res.append("\n");
            res.append("\t".repeat(Math.max(0, tab + 1)));
            res.append(nc.toString(tab + 1));
        }
        res.append("\n");
        res.append("\t".repeat(Math.max(0, tab + 1)));
        res.append("]");
        return res.toString();
    }

    public String toJson(int tab) {
        StringBuilder res = new StringBuilder();
        res.append("\t".repeat(Math.max(0, tab)));
        res.append("{\"element\":\"").append(elementString()).append("\"");
        if (children.size()>0) {
            res.append(",\n");
            res.append("\t".repeat(Math.max(0, tab + 1)));
            res.append("\"children\":[\"\"");
            for (MatchTreeNode nc : children) {
                res.append(",\n");
                res.append("\t".repeat(Math.max(0, tab + 1)));
                res.append(nc.toJson(tab + 1));
            }
            res.append("]\n");
        }
        res.append("\t".repeat(Math.max(0, tab + 1)));
        res.append("}");
        return res.toString();
    }

    /**
     * Initialize everything to null
     * (Needed by {@link MatchTreeRoot#MatchTreeRoot(MatchTreeNode)} )} as super() constructor)
     */
    public MatchTreeNode() {
    }

    /**
     * Base Constructor
     *
     * @param element   The element defined by this node
     * @param colmd     The graph to work in
     * @param varPprime The variables already defined in the cluster
     */
    public MatchTreeNode(Element element, CollectionsModel colmd, Set<Var> varPprime) {
        children = new HashSet<>();

        this.element = element;
        varE = ElementUtils.mentioned(element);
        D = new HashSet<>(varE);
        D.removeAll(varPprime);

        matchSet = ElementUtils.ans(this.element, colmd);
        delta = new HashSet<>(varE);
        delta.retainAll(varPprime);

        inserted = false;
    }

    /**
     * Constructor by copy
     */
    public MatchTreeNode(MatchTreeNode other) {
        children = new HashSet<>(other.getChildren());
        element = other.getElement();
        varE = other.getVarE();
        D = other.getD();
        matchSet = other.getMatchSet();
        delta = new HashSet<>(other.getDelta());
        inserted = other.inserted;
    }

    /**
     * Indicates that the node has been inserted somewhere in the tree
     */
    public void insert() {
        inserted = true;
    }

    /**
     * Whether the node has been inserted somewhere in the tree
     */
    public boolean isInserted() {
        return this.inserted;
    }

    /**
     * Replaces one child node by a new node in this node's children
     *
     * @param child The child to remove
     * @param other Usually a slightly modified copy of child
     */
    public void replace(MatchTreeNode child, MatchTreeNode other) {
        this.children.remove(child);
        this.children.add(other);
    }

    /**
     * The Lazy Joins Algorithm
     *
     * @param tree T : The match-tree we are working in
     * @param node n* : The node to insert
     */
    LazyJoin lazyJoin(MatchTreeRoot tree, MatchTreeNode node) {
        logger.debug("trying " + node.elementString() + " under " + elementString());
        HashSet<Var> deltaplus = new HashSet<>();
        HashSet<Var> deltaminus = new HashSet<>();
        SingletonStopwatchCollection.resume("copy self");
        MatchTreeNode copy = new MatchTreeNode(this);
        SingletonStopwatchCollection.stop("copy self");
        boolean modified = false;

        for (MatchTreeNode nc : children) {
            logger.debug("recur in");
            LazyJoin recur = nc.lazyJoin(tree, node);
            logger.debug("recur out");
            copy.replace(nc, recur.copy);
            deltaplus.addAll(recur.deltaplus);
            deltaminus.addAll(recur.deltaminus);
            if (recur.modified) {
                logger.debug("modified");
                if (Level.TRACE.isGreaterOrEqual(logger.getLevel())) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ResultSet rs = recur.copy.matchSet.toResultSet();
                    ResultSetFormatter.out(baos, rs);
                    logger.trace(baos.toString());
                }
                int rowsBefore = copy.matchSet.size();
                int colsBefore = copy.matchSet.getVars().size();
                logger.debug("proj");
                Table proj = TableUtils.projection(recur.copy.matchSet, recur.copy.delta);
                logger.debug("join");
                copy.matchSet = TableUtils.simpleJoin(matchSet, proj);
                logger.debug("joined");
                int rowsAfter = copy.matchSet.size();
                int colsAfter = copy.matchSet.getVars().size();
                modified = (!(rowsBefore == rowsAfter && colsBefore == colsAfter));
            }
        }
        if (!Collections.disjoint(this.D, node.delta)) {
            logger.debug(node.elementString() + " connected to " + elementString());
            if (!node.isInserted()) {
                logger.debug("inserting " + node.elementString() + " under " + elementString());


                Set<Var> addminus = new HashSet<>(node.delta);
                addminus.removeAll(this.D);
                addminus.removeAll(deltaminus);
                deltaminus.addAll(addminus);

                logger.debug("proj");
                Table proj = TableUtils.projection(node.matchSet, node.delta);

                logger.debug("join");
                copy.matchSet = TableUtils.simpleJoin(matchSet, proj);

                node.insert();
                copy.children.add(node);
                modified = true;
            } else {
                logger.debug("isInserted elsewhere, adding plus");
                Set<Var> addplus = new HashSet<>(node.delta);
                addplus.retainAll(this.D);
                addplus.removeAll(deltaplus);
                deltaplus.addAll(addplus);
            }
        }

        deltaplus.removeAll(deltaminus);

        deltaminus.removeAll(deltaplus);

        if (!copy.delta.containsAll(deltaplus)) {
            copy.delta.addAll(deltaplus);
            modified = true;
        }

        if (!copy.delta.containsAll(deltaminus)) {
            copy.delta.addAll(deltaminus);
            modified = true;
        }

        LazyJoin res = new LazyJoin(copy, deltaplus, deltaminus, modified);

        return res;
    }
}
