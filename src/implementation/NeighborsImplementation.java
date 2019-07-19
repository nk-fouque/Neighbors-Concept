package implementation;

import implementation.utils.CollectionsModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import sun.misc.SignalHandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Static methods for a basic implementation
 *
 * @see Main#main(String[])
 */
public class NeighborsImplementation {

    /**
     * Sets up all the loggers required by the partition algorithm
     *
     * @param level "verbous" : Gives almost every details possible
     *              "silent" : Only shows how far the partition algorithm is, to be able to tell i
     */
    public static void myLogsLevels(String level) {
        switch (level) {
            case "verbous": {
                Logger.getLogger("org.apache.jena").setLevel(Level.INFO);
                Logger.getLogger("implementation.algorithms.Partition").setLevel(Level.DEBUG);
                Logger.getLogger("implementation.algorithms.Cluster").setLevel(Level.DEBUG);
                Logger.getLogger("implementation.utils").setLevel(Level.INFO);
                Logger.getLogger("implementation.algorithms.matchTree.MatchTreeNode").setLevel(Level.TRACE);
                break;
            }
            case "silent": {
                Logger.getLogger("org.apache.jena").setLevel(Level.INFO);
                Logger.getLogger("implementation.algorithms.Partition").setLevel(Level.INFO);
                Logger.getLogger("implementation.algorithms.Cluster").setLevel(Level.INFO);
                Logger.getLogger("implementation.utils").setLevel(Level.OFF);
                Logger.getLogger("implementation.algorithms.matchTree.MatchTreeNode").setLevel(Level.OFF);
            }
        }
    }

    /**
     * Creates a {@link CollectionsModel} from a file
     *
     * @param filename The absolute path of the file on the system
     * @param format   The format the file is written in, see Jena documentation for supported formats
     * @param verbose  Whether the models should be printed on console after loading
     * @return
     * @throws IOException
     */
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

    /**
     * Creates a SignalHandler that, when triggered, sets a boolean to true
     *
     * @param interrupted the AtomicBoolean to set true
     * @return a generic SignalHandler
     */
    public static SignalHandler interruptCutter(AtomicBoolean interrupted, Collection<Thread> toInterrupt) {
        SignalHandler handler = sig -> {
            System.out.println("Captured " + sig.getName());
            for (Thread thread : toInterrupt) {
                thread.interrupt();
            }
            interrupted.set(true);
        };
        return handler;
    }
}
