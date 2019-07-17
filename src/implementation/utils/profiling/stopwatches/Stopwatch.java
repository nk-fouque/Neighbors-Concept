package implementation.utils.profiling.stopwatches;

/**
 * @author francesco.bariatti@irisa.fr
 */

public class Stopwatch {
    private long startTime = 0;
    private long stopTime = 0;
    private boolean isRunning = false;

    /**
     * Start the stopwatch.
     * Only a stopped stopwatch can be started.
     */
    public void start() {
        if (isRunning)
            throw new RuntimeException("Trying to start a running stopwatch");

        isRunning = true;
        stopTime = 0;
        startTime = System.nanoTime();
    }

    /**
     * Stop the stopwatch and store elapsed time.
     * Only a running stopwatch can be stopped.
     */
    public void stop() {
        if (!isRunning)
            throw new RuntimeException("Trying to stop a non-running stopwatch");

        stopTime = System.nanoTime();
        isRunning = false;
    }

    /**
     * Start the stopwatch, but instead of starting from 0, it starts from the previous elapsed time.
     * If the stopwatch has never been started, it is equivalent to start().
     * Only a stopped stopwatch can be resumed.
     */
    public void resume() {
        if (isRunning)
            throw new RuntimeException("Trying to resume a running stopwatch");

        startTime = System.nanoTime() - getElapsedNanoSecond();
        stopTime = 0;
        isRunning = true;
    }

    /**
     * Stop and restart the stopwatch.
     * Only a running stopwatch can be restarted.
     */
    public void restart() {
        stop();
        start();
    }

    /**
     * @return Whether the stopwatch is running.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * If the stopwatch is running: return elapsed time since the stopwatch has been started.
     * If the stopwatch is stopped: return time for which the stopwatch has been running.
     */
    public long getElapsedNanoSecond() {
        if (isRunning)
            return System.nanoTime() - startTime;
        else
            return stopTime - startTime;
    }

    /**
     * @return Same as getElapsedNanoSecond, but the time is in milliseconds.
     */
    public long getElapsedMilliseconds() {
        return getElapsedNanoSecond() / 1000000;
    }

    /**
     * @return Same as getElapsedNanoSecond, but the time is in seconds.
     */
    public long getElapsedSeconds() {
        return getElapsedNanoSecond() / 1000000000;
    }
}
