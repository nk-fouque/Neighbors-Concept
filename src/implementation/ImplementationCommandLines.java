package implementation;

import implementation.algorithms.Partition;
import implementation.utils.CollectionsModel;
import implementation.utils.TimeOut;
import implementation.utils.profiling.stopwatches.SingletonStopwatchCollection;
import org.apache.log4j.BasicConfigurator;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImplementationCommandLines {
    public static void main(String[] args) throws IOException {
        // Checking Parameters
        if (args.length < 2)
            throw new IOException("Missing arguments : required arguments are rdfFilePath and targetNodesFilePath ");

        // Setting Parameters
        String filename = args[0];
        String targetsFile = args[1];

        List<String> uriTargets = Collections.emptyList();
        try {
            uriTargets =
                    Files.readAllLines(Paths.get(targetsFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // TODO do something
            e.printStackTrace();
        }


        // Setting optional parameters
        int time = 0;
        boolean timeout = false;

        int depth = 1;

        String verboseMode = "silent";

        if (args.length > 2) {
            for (int i = 2; i < args.length; i++) {
                String[] arg = args[i].split("=", 2);
                switch (arg[0]) {
                    case "--t": {
                        time = Integer.valueOf(arg[1]);
                        timeout = true;
                        break;
                    }
                    case "--d": {
                        depth = Integer.valueOf(arg[1]);
                        break;
                    }
                    case "--v": {
                        verboseMode = arg[1];
                        break;
                    }
                    default: {
                        System.out.println("Unrecognized parameter : " + args[i]);
                    }
                }
            }
        }

        // Jena setup
        BasicConfigurator.configure();

        // Logger setup
        NeighborsImplementation.myLogsLevels(verboseMode);

        // Loading Model from file
        CollectionsModel model = NeighborsImplementation.loadModelFromFile(filename, false);

        // Preparing file export
        SimpleDateFormat formatter= new SimpleDateFormat("yyyyMMdd-HHmmss");
        Date now = new Date(System.currentTimeMillis());
        String resultsPath = "/tmp/cnn/results"+formatter.format(now)+".json";

        File file = new File(resultsPath);
        FileWriter writer;
        if (file.exists()) {
            writer = new FileWriter(file, true);
        } else {
            System.out.println(resultsPath+" doesn't already exist, will be created");
            String[] dirs = resultsPath.split("/");
            String dir = "";
            for (int i = 0;i<dirs.length-1;i++){
                dir+="/"+dirs[i];
            }
            File mkdir = new File(dir);
            mkdir.mkdirs();

            writer = new FileWriter(file);
        }
        writer.write("[\n");
        writer.close();

        for (String uriTarget : uriTargets) {
            writer = new FileWriter(file,true);

            // Preparing Partition
            Partition p = new Partition(model, uriTarget, depth);

            //Defining Timeout for anytime implementation
            AtomicBoolean cut = new AtomicBoolean(false);
            Thread timer = TimeOut.planTimeOut(cut, time);
            if (timeout) {
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
                    writer.write(p.toJson());
                    writer.write(",\n");
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
                        writer.write(p.toJson());
                        writer.write(",\n");
                    } catch (OutOfMemoryError err) {
                        System.out.println("Could not recover results, allocate more heap size or use (shorter) timeout");
                    }
                }
            }

            writer.close();
            SingletonStopwatchCollection.stop("Main");
            timer.interrupt();
        }

        writer = new FileWriter(file,true);
        writer.write("\"\"]");
        writer.close();


        Thread.currentThread().interrupt();
    }
}
