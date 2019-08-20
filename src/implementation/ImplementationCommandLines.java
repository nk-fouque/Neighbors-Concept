package implementation;

import implementation.algorithms.Partition;
import implementation.utils.CollectionsModel;
import implementation.utils.TimeOut;
import implementation.utils.profiling.stopwatches.SingletonStopwatchCollection;
import org.apache.log4j.BasicConfigurator;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImplementationCommandLines {
    public static void main(String[] args) throws IOException {
        // Checking Parameters
        if (args.length < 3)
            throw new IOException("Missing arguments : required arguments are rdfFilePath, rdfFileFormat and targetNodeUri ");

        // Setting Parameters
        String filename = args[0];
        String format = args[1];
        String uriTarget = args[2];

        // Setting optional parameters
        String resultsPath = "";
        boolean export = false;

        int time = 0;
        boolean timeout = false;

        int depth = 1;

        String verboseMode = "silent";

        if (args.length > 3) {
            for (int i = 3; i<args.length;i++){
                String[] arg = args[i].split("=",2);
                switch (arg[0]){
                    case "--e" : {
                        resultsPath = arg[1];
                        export = true;
                        break;
                    }
                    case "--t" : {
                        time = Integer.valueOf(arg[1]);
                        timeout = true;
                        break;
                    }
                    case "--d" : {
                        depth = Integer.valueOf(arg[1]);
                        break;
                    }
                    case "--v" : {
                        verboseMode = arg[1];
                        break;
                    }
                    default: {
                        System.out.println("Unrecognized parameter : "+args[i]);
                    }
                }
            }
        }

        // Jena setup
        BasicConfigurator.configure();

        // Logger setup
        NeighborsImplementation.myLogsLevels(verboseMode);

        // Loading Model from file
        CollectionsModel model = NeighborsImplementation.loadModelFromFile(filename, format, false);

        // Preparing Partition
        Partition p = new Partition(model, uriTarget, depth);

        // Preparing file export
        FileWriter writer = null;
        if (export) writer = new FileWriter(resultsPath);

        //Defining Timeout for anytime implementation
        AtomicBoolean cut = new AtomicBoolean(false);
        Thread timer = TimeOut.planTimeOut(cut, time);
        if (timeout){
            timer.start();
        }
        // Defining Signal Handler for anytime implementation
        SignalHandler handler = NeighborsImplementation.interruptCutter(cut, Collections.singleton(timer));
        Signal.handle(new Signal("INT"), handler);

        // Starting main Stopwatch
        SingletonStopwatchCollection.resume("Main");

        // Launching the algorithm
        int algoRun = p.completePartitioning(cut);

        // Processing results
        switch (algoRun) {
            case 0: {
                System.out.println(p.toString());
                if (export) writer.write(p.toString());
                break;
            }
            case -1: {
                System.out.println("Something went Wrong with the partition");
                break;
            }
            case 1: {
                System.out.println("Java Heap went out of memory after " + SingletonStopwatchCollection.getElapsedSeconds("Main") + "s");
                break;
            }
            case 2: {
                System.out.println("Anytime algorithm cut");
                p.cut();
                try {
                    String results = p.toString();
                    System.out.println(results);
                    if (export) writer.write(results);
                } catch (OutOfMemoryError err) {
                    System.out.println("Could not recover results, allocate more heap size or use (shorter) timeout");
                }
            }
        }

        if (export) writer.close();

        timer.interrupt();
        Thread.currentThread().interrupt();
    }
}
