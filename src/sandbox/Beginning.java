package sandbox;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Beginning {
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getLogger("org.apache.jena").setLevel(Level.INFO);

        Model md = ModelFactory.createDefaultModel();
        md.createResource("http://rien")
                .addProperty(RDFS.label, "Test");
        md.createResource("http://presque.rien")
                .addProperty(RDFS.label, "Test2");
        md.write(System.out, "TTL");
        System.out.println(md.size());

        /*
        Property p = RDFS.label;
        ResIterator ri = md.listSubjectsWithProperty(p);
        while (ri.hasNext()){
            Resource r = ri.nextResource();
            Statement st = md.getResource(r.toString()).getProperty(p);
            System.out.println(st);
            System.out.println(st.getSubject()+" : "+st.getObject());
        }
        */

        Query qry = QueryFactory.create("SELECT ?x WHERE {}");
        QueryExecution qe = QueryExecutionFactory.create(qry, md);
        ResultSet res = qe.execSelect();
        ResultSetFormatter.out(System.out, res);


//        ResultSetFormatter.out(System.out,res,qry);
//        System.out.println(new E_Exists(((ElementGroup)qry.getQueryPattern()).getElements().get(0)).getVarsMentioned());
    }
}
