package implementation.matchTrees;

import implementation.utils.CollectionsModel;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.table.TableN;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.expr.E_Equals;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MatchTreeRoot extends MatchTreeNode {
    public static Logger logger = Logger.getLogger(MatchTreeNode.class);

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
        domM = new ArrayList<>(top);

        children = new ArrayList<>();
    }

    public MatchTreeRoot(MatchTreeNode other) {
        super();
        element = null;
        varE = new ArrayList<>();
        D = new ArrayList<>(other.getD());

        matchSet = other.getMatchSet();
        delta = new ArrayList<>(other.getDelta());
        domM = new ArrayList<>(other.getDomM());

        children = new ArrayList<>(other.getChildren());

    }

    @Override
    public String toString() {
        return super.toString(0);
    }

    public Table getMatchSet() {
        return matchSet;
    }

    public MatchTreeRoot lazyJoin(Element element, CollectionsModel colMd, List<Var> varPprime) {
        MatchTreeNode newnode = new MatchTreeNode(element, colMd, varPprime);
//        ResultSetFormatter.out(System.out,newnode.matchSet.toResultSet());
        LazyJoin res = this.lazyJoin(this, newnode);
        return new MatchTreeRoot(res.copy);
    }


    public static void main(String[] args) throws FileNotFoundException {
        // Logger setup
        BasicConfigurator.configure();
        Logger.getLogger("implementation.utils").setLevel(Level.OFF);
        Logger.getLogger("implementation.matchTrees.MatchTreeNode").setLevel(Level.DEBUG);

        // Loading Model from file
//        String filename = "/udd/nfouque/Documents/default_mondial.nt";
        String filename = "/udd/nfouque/Documents/royal.ttl";
        Model md = ModelFactory.createDefaultModel();
        md.read(new FileInputStream(filename), null, "TTL");
//        md.write(System.out,"TURTLE");

        Model saturated = ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), md);
//        saturated.write(System.out, "TURTLE");
        CollectionsModel colmd = new CollectionsModel(md, saturated);


        List<Var> top = new ArrayList<>(Collections.singletonList(Var.alloc("person")));
        MatchTreeRoot root = new MatchTreeRoot(top, colmd);


        ElementPathBlock el1 = new ElementPathBlock();
        el1.addTriple(new Triple(Var.alloc("person"), new ResourceImpl("http://example.org/royal/parent").asNode(), Var.alloc("parent").asNode()));

        root.lazyJoin(el1, colmd, top);
        ResultSetFormatter.out(System.out, root.matchSet.toResultSet());
        top.add(Var.alloc("parent"));

        ElementFilter el2 = new ElementFilter(new E_Equals(new ExprVar(Var.alloc("parent")), new NodeValueNode(new ResourceImpl("http://example.org/royal/Kate").asNode())));

        root.lazyJoin(el2, colmd, top);
        ResultSetFormatter.out(System.out, root.matchSet.toResultSet());

    }
}
