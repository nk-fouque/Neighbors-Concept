package implementation.utils;

import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.table.TableN;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.engine.join.QueryIterHashJoin;
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
            BindingHashMap bind = new BindingHashMap();
            for (Var var : vars) {
                bind.add(var, b.get(var));
            }
            res.addBinding(bind);
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
}
