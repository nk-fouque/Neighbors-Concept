package implementation.utils;

import java.util.Set;

public class SetUtils {
    public static String toSimpleJson(Set set) {
        StringBuilder res = new StringBuilder("[\"\"");
        for (Object o : set) {
            res.append(",\"");
            res.append(o.toString().replaceAll("[^\\\\]\"", "\\\\\""));
            res.append("\"");
        }
        set.forEach(o -> {
        });
        res.append("]");
        return res.toString();
    }
}
