package implementation.algorithms.matchTree;

import org.apache.jena.sparql.core.Var;

import java.util.ArrayList;

/**
 * Object to encapsulate every useful attributes for the Lazy Joins algorithm recursion
 *
 * @author nk-fouque
 */
public class LazyJoin {
    MatchTreeNode copy;
    ArrayList<Var> deltaplus;
    ArrayList<Var> deltaminus;
    boolean modified;

    public LazyJoin(MatchTreeNode node, ArrayList<Var> plus, ArrayList<Var> minus, boolean update) {
        copy = node;
        deltaplus = plus;
        deltaminus = minus;
        modified = update;
    }
}
