package implementation.utils.profiling;

import java.util.HashMap;

public class CallCounterCollection {
    private static HashMap<String, CallCounter> counters = new HashMap<>();

    public static void call(String functionName){
        counters.putIfAbsent(functionName, new CallCounter(functionName));
        counters.get(functionName).call();
    }

    public static int getCallCount(String functionName){
        CallCounter counter = counters.getOrDefault(functionName,new CallCounter(functionName));
        return counter.getCount();
    }
}
