package v2.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class SingletonStopwatchCollection {
    private static Map<Object, Stopwatch> stopwatches = new HashMap<>();

    private SingletonStopwatchCollection()
    {}

    private static NoSuchElementException createMissingStopWatchException()
    {
        return new NoSuchElementException("Requested stopwatch does not exist in collection");
    }

    /**
     * Start the stopwatch associated with the given key, creating it if it doesn't exist.
     */
    public static void start(Object key)
    {
        stopwatches.computeIfAbsent(key, k -> new Stopwatch()).start();
    }

    /**
     * Stop the stopwatch associated with the given key
     */
    public static void stop(Object key)
    {
        Stopwatch stopWatch = stopwatches.get(key);
        if (stopWatch == null)
            throw createMissingStopWatchException();
        stopWatch.stop();
    }

    /**
     * Resume the stopwatch associated with the given key.
     * If no stopwatch is associated with the given key, a new one is created (in this case resume is equivalent to start).
     */
    public static void resume(Object key)
    {
        stopwatches.computeIfAbsent(key, k -> new Stopwatch()).resume();
    }

    /**
     * Restart the stopwatch associated with the given key
     */
    public static void restart(Object key)
    {
        Stopwatch stopWatch = stopwatches.get(key);
        if (stopWatch == null)
            throw createMissingStopWatchException();
        stopWatch.restart();
    }

    /**
     * @return Whether the stopwatch associated with the given key exist in this collection
     */
    public static boolean contains(Object key)
    {
        return stopwatches.containsKey(key);
    }

    /**
     * @return Whether the stopwatch associated with the given key is running. False if there is no stopwatch associated to the key.
     */
    public static boolean isRunning(Object key)
    {
        Stopwatch stopWatch = stopwatches.get(key);
        return stopWatch != null && stopWatch.isRunning();
    }

    /**
     * @return Elapsed nanoseconds on the stopwatch associated with the given key.
     */
    public static long getElapsedNanoSecond(Object key)
    {
        Stopwatch stopWatch = stopwatches.get(key);
        if (stopWatch == null)
            throw createMissingStopWatchException();
        return stopWatch.getElapsedNanoSecond();
    }

    /**
     * @return Elapsed milliseconds on the stopwatch associated with the given key.
     */
    public static long getElapsedMilliseconds(Object key)
    {
        Stopwatch stopWatch = stopwatches.get(key);
        if (stopWatch == null)
            throw createMissingStopWatchException();
        return stopWatch.getElapsedMilliseconds();
    }

    /**
     * @return Elapsed seconds on the stopwatch associated with the given key.
     */
    public static long getElapsedSeconds(Object key)
    {
        Stopwatch stopWatch = stopwatches.get(key);
        if (stopWatch == null)
            throw createMissingStopWatchException();
        return stopWatch.getElapsedSeconds();
    }
}
