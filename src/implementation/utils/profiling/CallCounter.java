package implementation.utils.profiling;

/**
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

    public int getCount() {
        return count;
    }
}
