package implementation.utils;

import java.util.concurrent.atomic.AtomicBoolean;

public class TimeOut implements Runnable {
    private long millis;
    private AtomicBoolean cut;

    public TimeOut(AtomicBoolean cut, int seconds) {
        millis = seconds * 1000;
        this.cut = cut;
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
