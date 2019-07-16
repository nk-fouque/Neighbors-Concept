package implementation.utils;

import java.util.concurrent.atomic.AtomicBoolean;

public class AlgorithmTimer {
    public static void planTimeOut(AtomicBoolean cut, int seconds){
        Thread thread = new Thread(new TimeOut(cut, seconds));
        thread.start();
    }
}
