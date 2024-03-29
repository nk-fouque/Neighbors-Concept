package implementation.utils;

import implementation.utils.profiling.CallCounterCollection;
import implementation.utils.profiling.stopwatches.SingletonStopwatchCollection;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.table.TableN;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingHashMap;
import org.apache.jena.sparql.engine.join.QueryIterHashJoin;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Several static methods to do various things with Jena {@link Table}
 *
 * @author nk-fouque
 */
public class TableUtils {
    private static Logger logger = Logger.getLogger(TableUtils.class);

    /**
     * Uses Jena's iterator join to join two tables
     */
    public static TableN simpleJoin(Table left, Table right) {
        CallCounterCollection.call("join");
        SingletonStopwatchCollection.resume("join");
//        System.out.println("Joining : \n"+left+" and \n"+right);
        TableN res = new TableN(QueryIterHashJoin.create(left.iterator(null), right.iterator(null), null));
        SingletonStopwatchCollection.stop("join");
        return res;
    }

    /**
     * Algebric table projection
     *
     * @param table The table to project
     * @param vars  A list of variables to do the projection on
     */
    public static Table projection(Table table, Set<Var> vars) {
        CallCounterCollection.call("projection");
        SingletonStopwatchCollection.resume("projection");
        Table res;
        if (vars.containsAll(table.getVars())) {
            res = table;
        } else {
            res = new TableN();
            Iterator<Binding> iter = table.rows();
            Set<Binding> temp = new HashSet<>();
            Binding b;
            while (iter.hasNext()) {
                b = iter.next();
                BindingHashMap bind = new BindingHashMap();
                for (Var var : vars) {
                    bind.add(var, b.get(var));
                }
                temp.add(bind);
            }
            temp.forEach(res::addBinding);
        }
        SingletonStopwatchCollection.stop("projection");
        return (res);
    }

    /**
     * Algebraic Table difference
     *
     * @return The left table from which all elements appearing in the right table have been removed
     */
    public static TableN difference(Table left, Table right) {
        CallCounterCollection.call("difference");
        SingletonStopwatchCollection.resume("difference");
        TableN res = new TableN();
        Iterator<Binding> iterLeft = left.rows();

        Iterator<Binding> iterRight = right.rows();
        Set<Binding> listRight = new HashSet<>();
        while (iterRight.hasNext()) {
            listRight.add(iterRight.next());
        }

        iterLeft.forEachRemaining(b -> {
            if (!listRight.contains(b)) {
                res.addBinding(b);
            }
        });
        SingletonStopwatchCollection.stop("difference");
        return res;
    }

    /**
     * @param table The Table you want to remove duplicate lines from
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
