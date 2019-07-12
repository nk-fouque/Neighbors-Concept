package implementation.utils;

import java.util.concurrent.atomic.AtomicBoolean;

public class CutPlanner implements Runnable {
    private long millis;
    private AtomicBoolean cut;

    public CutPlanner(int seconds, AtomicBoolean cut){
        millis = seconds*1000;
        this.cut = cut;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Stop");
        cut.set(true);
        Thread.currentThread().interrupt();
    }
}
