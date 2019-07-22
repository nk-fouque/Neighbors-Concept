package implementation;

import implementation.algorithms.Partition;
import implementation.utils.CollectionsModel;
import implementation.utils.TimeOut;
import implementation.utils.profiling.CallCounterCollection;
import implementation.utils.profiling.stopwatches.SingletonStopwatchCollection;
import org.apache.log4j.BasicConfigurator;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author nk-fouque
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
        String format = "TTL";
        CollectionsModel model = NeighborsImplementation.loadModelFromFile(filename, format, false);

        // Choose node
        String uriTarget = "http://www.semwebtech.org/mondial/10/country/PE/";
//        String uriTarget = "http://example.org/royal/Charlotte";

        // Preparing Partition
        Partition p = new Partition(model, uriTarget, 1);
//        System.out.println(p.getClusters().get(0));
//        System.out.println("Printing graph" + model);

        // Preparing file export
        FileWriter writer = new FileWriter("/udd/nfouque/Documents/results.txt");

        AtomicBoolean cut = new AtomicBoolean(false);
        //Defining Timeout for anytime implementation
        Thread timer = TimeOut.planTimeOut(cut, 30);
//        timer.start();
        // Defining Signal Handler for anytime implementation
        SignalHandler handler = NeighborsImplementation.interruptCutter(cut, Collections.singleton(timer));
        Signal.handle(new Signal("INT"), handler);

        // Starting main Stopwatch
        SingletonStopwatchCollection.resume("Main");

        // Launching the algorithm
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
                System.out.println("Java Heap went out of memory after "+SingletonStopwatchCollection.getElapsedSeconds("Main")+"s");
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
                    System.out.println("Could not recover results, allocate more heap size or use (shorter) timeout");
                }
            }
        }

        writer.close();

        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("Main"));
        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("iterate") + " : " + CallCounterCollection.getCallCount("iterate"));
        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("reste"));
        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("newans"));
        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("connect"));
        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("projjoin"));

        System.out.println("\n"+SingletonStopwatchCollection.getElapsedMilliseconds("lazyjoin"));

        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("join") + " : " + CallCounterCollection.getCallCount("join"));
        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("projection") + " : " + CallCounterCollection.getCallCount("projection"));
        System.out.println(SingletonStopwatchCollection.getElapsedMilliseconds("difference") + " : " + CallCounterCollection.getCallCount("difference"));


        timer.interrupt();
        Thread.currentThread().interrupt();

    }
}
