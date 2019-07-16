package implementation.matchTrees;

import implementation.utils.CollectionsModel;
import implementation.utils.ElementUtils;
import implementation.utils.ListUtils;
import implementation.utils.TableUtils;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.table.TableN;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.Element;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MatchTreeNode {
    private static Logger logger = Logger.getLogger(MatchTreeNode.class);

    Element element;
    List<Var> varE;
    List<Var> D;

    Table matchSet;
    List<Var> delta;
    List<Var> domM;

    List<MatchTreeNode> children;

    boolean inserted;

    public Element getElement() {
        return element;
    }

    public List<Var> getVarE() {
        return varE;
    }

    public List<Var> getD() {
        return D;
    }

    public Table getMatchSet() {
        return matchSet;
    }

    public List<Var> getDelta() {
        return delta;
    }

    public List<Var> getDomM() {
        return domM;
    }

    public List<MatchTreeNode> getChildren() {
        return children;
    }

    public String elementString(){
        if(element == null){
            return ("T("+D.toString()+")");
        } else {
            return element.toString();
        }
    }

    public String toString(int tab) {
        StringBuilder res = new StringBuilder();
        for(int i = 0;i<tab;i++){
            res.append("\t");
        }
        res.append("[Element : "+elementString());
        res.append("\n");
        for(int i = 0;i<=tab;i++){
            res.append("\t");
        }
        res.append("Children : ");
        for(MatchTreeNode nc : children){
            res.append("\n");
            for(int i = 0;i<=tab;i++){
                res.append("\t");
            }
            res.append(nc.toString(tab+1));
        }
        res.append("\n");
        for(int i = 0;i<=tab;i++){
            res.append("\t");
        }
        res.append("]");
        return res.toString();
    }

    public MatchTreeNode() {
    }

    public MatchTreeNode(Element element,CollectionsModel colmd,List<Var> varPprime){
        children = new ArrayList<>();

        this.element = element;
        varE = ElementUtils.mentioned(element);
        D = new ArrayList<>(varE);
        D.removeAll(varPprime);

        matchSet = ElementUtils.ans(this.element,colmd);
        domM = new ArrayList<>(varE);
        delta = new ArrayList<>(varE);
        delta.retainAll(varPprime);


        inserted = false;
    }

    public MatchTreeNode(MatchTreeNode other){
        children = new ArrayList<>(other.getChildren());
        element = other.getElement();
        varE = new ArrayList<>(other.getVarE());
        D = new ArrayList<>(other.getD());
        matchSet = new TableN(other.getMatchSet().iterator(null));
        delta = new ArrayList<>(other.getDelta());
        domM = new ArrayList<>(other.getDomM());
        inserted = other.inserted;
    }

    public void insert(){
        inserted = true;
    }

    public boolean inserted(){
        return this.inserted;
    }

    public void replace(MatchTreeNode child, MatchTreeNode other){
        this.children.remove(child);
        this.children.add(other);
    }

    public LazyJoin lazyJoin(MatchTreeRoot tree, MatchTreeNode node) {
        logger.debug("trying "+node.elementString()+" under "+elementString());
        ArrayList<Var> deltaplus = new ArrayList<>();
        ArrayList<Var> deltaminus = new ArrayList<>();
        MatchTreeNode copy = new MatchTreeNode(this);
        boolean modified = false;

        for (MatchTreeNode nc : children) {
            logger.debug("recur in");
            LazyJoin recur = nc.lazyJoin(tree, node);
            logger.debug("recur out");
            copy.replace(nc,recur.copy);
            deltaplus.addAll(recur.deltaplus);
            ListUtils.removeDuplicates(deltaplus);
            deltaminus.addAll(recur.deltaminus);
            ListUtils.removeDuplicates(deltaminus);
            if (recur.modified) { //TODO Determine how to know if DeltaC or Mc was modified
                logger.debug("modified");
                if (Level.TRACE.isGreaterOrEqual(logger.getLevel())){
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ResultSet rs = recur.copy.matchSet.toResultSet();
                    ResultSetFormatter.out(baos,rs);
                    logger.trace(baos.toString());
                };
                logger.debug("proj");
                Table proj = TableUtils.projection(recur.copy.matchSet,recur.copy.delta);
                logger.debug("join");
                copy.matchSet=TableUtils.simpleJoin(matchSet,proj);
                logger.debug("joined");
                modified = true;
            }
        }
        if (!Collections.disjoint(this.D, node.delta)) {
            logger.debug(node.elementString()+" connected to "+elementString());
            if (!node.inserted()) {
                logger.debug("inserting "+node.elementString()+" under "+elementString());
                List<Var> addminus = new ArrayList<>(node.delta);
                addminus.removeAll(this.D);
                addminus.removeAll(deltaminus);
                deltaminus.addAll(addminus);
                try {
                    logger.debug("proj");
                    Table proj = TableUtils.projection(node.matchSet, node.delta);
                    logger.debug("join");
                    copy.matchSet = TableUtils.simpleJoin(matchSet, proj);
                } catch(OutOfMemoryError err){
                    copy = null;
                    System.gc();
                    throw err;
                }

                node.insert();
                copy.children.add(node);
                modified = true;
            } else {
                logger.debug("inserted elsewhere, adding plus");
                List<Var> addplus = new ArrayList<>(node.delta);
                addplus.retainAll(this.D);
                addplus.removeAll(deltaplus);
                deltaplus.addAll(addplus);
            }
        }

        deltaplus.removeAll(deltaminus);
        deltaminus.removeAll(deltaplus);

        if(!copy.delta.containsAll(deltaplus)) {
            copy.delta.addAll(deltaplus);
            modified = true;
        }

        if(!copy.delta.containsAll(deltaminus)) {
            copy.delta.addAll(deltaminus);
            modified = true;
        }

        LazyJoin res;
        if (modified) {
            logger.debug("returning modified");
            res = new LazyJoin(copy, deltaplus, deltaminus,true);
        } else {
            logger.debug("returning unchanged");
            res = new LazyJoin(this,deltaplus,deltaminus,false);
        }
        return res;
    }
}
