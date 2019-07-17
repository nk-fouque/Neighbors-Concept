package implementation.utils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class ListUtils {
    /**
     * https://www.geeksforgeeks.org/how-to-remove-duplicates-from-arraylist-in-java/
     */
    public static <T> List<T> removeDuplicates(List<T> list) {

        // Create a new LinkedHashSet
        Set<T> set = new LinkedHashSet<>();

        // Add the elements to set
        set.addAll(list);

        // Clear the list
        list.clear();

        // add the elements of set
        // with no duplicates to the list
        list.addAll(set);

        // return the list
        return list;
    }
}
