package implementation.algorithms.matchTree;

import org.apache.jena.sparql.core.Var;

import java.util.HashSet;

/**
 * Simple Object to encapsulate every useful attributes for the Lazy Joins algorithm recursion
 *
 * @author nk-fouque
 */
public class LazyJoin {
    MatchTreeNode copy;
    HashSet<Var> deltaplus;
    HashSet<Var> deltaminus;
    boolean modified;

    public LazyJoin(MatchTreeNode node, HashSet<Var> plus, HashSet<Var> minus, boolean update) {
        copy = node;
        deltaplus = plus;
        deltaminus = minus;
        modified = update;
    }
}
