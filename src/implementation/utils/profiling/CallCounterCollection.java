package implementation.utils.profiling;

import java.util.HashMap;

/**
 * @author nk-fouque
 */
public class CallCounterCollection {
    private static HashMap<String, CallCounter> counters = new HashMap<>();

    /**
     * Increments the call count of the function in parameter, creates a new counter if not already done
     *
     * @param functionName
     */
    public static void call(String functionName) {
        counters.putIfAbsent(functionName, new CallCounter(functionName));
        counters.get(functionName).call();
    }

    /**
     * @return The number of time the function in parameter has been called
     */
    public static int getCallCount(String functionName) {
        CallCounter counter = counters.getOrDefault(functionName, new CallCounter(functionName));
        return counter.getCount();
    }
}
