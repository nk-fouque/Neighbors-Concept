package implementation.utils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runnable to set an AtomicBoolean to true after a certain amount of time
 * To be tested by anytime algorithms to know if they should stop running
 *
 * @author nk-fouque
 */
public class TimeOut implements Runnable {
    private long millis;
    private AtomicBoolean cut;

    public TimeOut(AtomicBoolean cut, int seconds) {
        millis = seconds * 1000;
        this.cut = cut;
    }

    /**
     * Starts a thread that will wait a certain time before setting a boolean to true
     * Said boolean can then be used by algorithms to check if they should stop
     *
     * @param cut The AtomicBoolean that should be set to true
     * @param seconds The time to wait in seconds
     */
    public static void planTimeOut(AtomicBoolean cut, int seconds) {
        Thread thread = new Thread(new TimeOut(cut, seconds));
        thread.start();
    }

    @Override
    public void run() {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Timeout");
        cut.set(true);
        Thread.currentThread().interrupt();
    }
}
