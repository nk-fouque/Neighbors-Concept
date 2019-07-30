package implementation.utils.profiling;

import java.util.HashMap;

/**
 * Collection of CallCounter with static methods to call from anywhere in code
 *
 * @author nk-fouque
 */
public class CallCounterCollection {
    private static HashMap<String, CallCounter> counters = new HashMap<>();

    /**
     * Increments the call count of the function in parameter, creates a new counter if not already done
     */
    public static void call(String functionName) {
        counters.putIfAbsent(functionName, new CallCounter(functionName));
        counters.get(functionName).call();
    }

    /**
     * Sets the counter for said function to zero
     */
    public static void reset(String functionName){
        CallCounter counter = counters.getOrDefault(functionName,null);
        if (counter != null) counter.reset();
    }

    public static void resetAll(){
        counters.keySet().forEach(s -> counters.get(s).reset());
    }

    /**
     * @return The number of time the function in parameter has been called
     */
    public static int getCallCount(String functionName) {
        CallCounter counter = counters.getOrDefault(functionName, new CallCounter(functionName));
        return counter.getCount();
    }
}
