package implementation;

import implementation.utils.CollectionsModel;
import implementation.utils.TimeOut;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import sun.misc.SignalHandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class NeighborsImplementation {

    public static void myLogsLevels(String level) {
        switch (level) {
            case "verbous": {
                Logger.getLogger("org.apache.jena").setLevel(Level.INFO);
                Logger.getLogger("implementation.Partition").setLevel(Level.DEBUG);
                Logger.getLogger("implementation.Cluster").setLevel(Level.DEBUG);
                Logger.getLogger("implementation.utils").setLevel(Level.INFO);
                Logger.getLogger("implementation.matchTrees.MatchTreeNode").setLevel(Level.TRACE);
                break;
            }
            case "silent": {
                Logger.getLogger("org.apache.jena").setLevel(Level.INFO);
                Logger.getLogger("implementation.Partition").setLevel(Level.INFO);
                Logger.getLogger("implementation.Cluster").setLevel(Level.INFO);
                Logger.getLogger("implementation.utils").setLevel(Level.OFF);
                Logger.getLogger("implementation.matchTrees.MatchTreeNode").setLevel(Level.OFF);
            }
        }
    }

    public static CollectionsModel loadModelFromFile(String filename, String format, boolean verbose) throws IOException {
        Model md = ModelFactory.createDefaultModel();
        md.read(new FileInputStream(filename), null, format);
        if (verbose) {
            md.write(System.out, "TURTLE");
        }

        Model saturated = ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), md);
        if (verbose) {
            saturated.write(System.out, "TURTLE");
        }

        CollectionsModel res = new CollectionsModel(md, saturated);
        return res;
    }

    public static void planTimeOut(AtomicBoolean cut, int seconds) {
        Thread thread = new Thread(new TimeOut(cut, seconds));
        thread.start();
    }

    public static SignalHandler interruptCutter(AtomicBoolean cut) {
        SignalHandler handler = sig -> {
            System.out.println("Captured " + sig.getName());
            cut.set(true);
        };
        return handler;
    }
}
