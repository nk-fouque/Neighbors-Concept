package implementation.utils.profiling;

/**
 * Small profiling functions to count the number of times a function has been called
 *
 * @author nk-fouque
 */
public class CallCounter {

    private String function;

    private int count;

    public CallCounter(String functionName) {
        function = functionName;
        count = 0;
    }

    /**
     * Increases call count by one
     */
    public void call() {
        count++;
    }

    public void reset() {
        count = 0;
    }

    public int getCount() {
        return count;
    }
}
