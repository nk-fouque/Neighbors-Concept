package implementation;

import implementation.utils.CollectionsModel;
import implementation.utils.TimeOut;
import implementation.utils.profiling.stopwatches.SingletonStopwatchCollection;
import org.apache.log4j.BasicConfigurator;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author nfouque
 */
public class Main {

    public static void main(String[] args) throws IOException {

        //Jena setup
        BasicConfigurator.configure();

        // Logger setup
        NeighborsImplementation.myLogsLevels("silent");

        // Loading Model from file
        String filename = "/udd/nfouque/Documents/default_mondial.nt";
//        String filename = "/udd/nfouque/Documents/royal.ttl";
        CollectionsModel model = NeighborsImplementation.loadModelFromFile(filename, "TTL", false);

        // Choose node
        String uriTarget = "http://www.semwebtech.org/mondial/10/country/PE/";
//        String uriTarget = "http://example.org/royal/Charlotte";

        // Preparing Partition
        Partition p = new Partition(model, uriTarget);
//        System.out.println(p.getClusters().get(0));
//        System.out.println("Printing graph" + model);

        // Preparing file export
        FileWriter writer = new FileWriter("/udd/nfouque/Documents/results.txt");

        // Defining Signal Handler for anytime implementation
        AtomicBoolean cut = new AtomicBoolean(false);
        SignalHandler handler = NeighborsImplementation.interruptCutter(cut);
        Signal.handle(new Signal("INT"), handler);

        // Starting main Stopwatch
        SingletonStopwatchCollection.resume("Main");

        // Launching the algorithm
        TimeOut.planTimeOut(cut, 120);
        int algoRun = p.partitionAlgorithm(cut);

        // Processing results
        switch (algoRun) {
            case 0: {
                System.out.println(p.toString());
                writer.write(p.toString());
                break;
            }
            case -1: {
                System.out.println("Something went Wrong with the partition");
                break;
            }
            case 1: {
                System.out.println("Java Heap went out of memory");
                algoRun++;
                break;
            }
            case 2: {
                System.out.println("Anytime algorithm cut");
                p.cut();
                try {
                    String results = p.toString();
                    System.out.println(results);
                    writer.write(results);
                } catch (OutOfMemoryError err) {
                    System.out.println("Could not recover results, allocate more heap size or use a timeout");
                }
            }
        }

        writer.close();

        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("Main"));
        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("iterate"));
        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("relax"));
        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("reste"));
        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("newans"));
        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("connect"));
        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("projjoin"));

    }
}
