package implementation;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.sparql.core.Var;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import implementation.utils.SingletonStopwatchCollection;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Main {


    public static void main(String[] args) throws IOException {
        // Logger setup
        BasicConfigurator.configure();
        Logger.getLogger("org.apache.jena").setLevel(Level.INFO);
        Logger.getLogger("implementation.Partition").setLevel(Level.INFO);
        Logger.getLogger("implementation.Cluster").setLevel(Level.DEBUG);
        Logger.getLogger("implementation.utils").setLevel(Level.OFF);

        // Loading Model from file
        String filename = "/udd/nfouque/Documents/default_mondial.nt";
//        String filename = "/udd/nfouque/Documents/royal.ttl";
        Model md = ModelFactory.createDefaultModel();
        md.read(new FileInputStream(filename), null, "NT");
//        md.write(System.out,"TURTLE");

        Model saturated = ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), md);
//        saturated.write(System.out, "TURTLE");

        // Choose node and describe it
        String uriTarget = "http://www.semwebtech.org/mondial/10/country/PE/";
//        String uriTarget = "http://example.org/royal/Charlotte";
        Map<String, Var> keys = new HashMap<>();
        String QueryString = Partition.initialQueryString(uriTarget, md, keys);
        System.out.println(QueryString);

        // Printing the result just to show that we find it back
        Query q = QueryFactory.create(QueryString);
        QueryExecution qe = QueryExecutionFactory.create(q, saturated);
        ResultSetFormatter.out(System.out, qe.execSelect(), q);
        System.out.println("\n\n");

        // Creation of the Partition
        Partition p = new Partition(q, md, saturated, keys);
        System.out.println(p.getClusters().get(0));
        System.out.println("Printing graph"+p.getGraph());

        FileWriter writer = new FileWriter("/udd/nfouque/Documents/results.txt");
        // Apply algorithm
        SingletonStopwatchCollection.resume("Main");
        int algoRun = p.partitionAlgorithm();
        switch(algoRun){
            case 0 : {
                System.out.println(p.toString());
                writer.write(p.toString());
            }
            case -1 : {
                System.out.println("Something went Wrong with the partition");
            }
            case 1 : {
                System.out.println("Java Heap went out of memory, anytime algorithm cut");
                p.cut();
                System.out.println(p.toString());
                writer.write(p.toString());
            }
        }
        writer.close();

        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("Main"));
        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("iterate"));
        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("relax"));
        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("reste"));
        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("newans"));

    }
}
