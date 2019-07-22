package implementation.utils.profiling;

public class CallCounter {

    private String function;

    private int count;

    public CallCounter(String functionName){
        function = functionName;
        count = 0;
    }

    public void call(){
        count++;
    }

    public int getCount() {
        return count;
    }
}
