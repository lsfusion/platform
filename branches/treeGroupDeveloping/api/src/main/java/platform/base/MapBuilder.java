package platform.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class MapBuilder {

    private static <T> void recBuildPermutations(Collection<T> col, List<T> cur, Collection<List<T>> result) {

        if (cur.size() == col.size()) { result.add(new ArrayList<T>(cur)); return; }

        for (T element : col) {
            if (!cur.contains(element)) {
                cur.add(element);
                recBuildPermutations(col, cur, result);
                cur.remove(element);
            }
        }
    }

    public static <T> Collection<List<T>> buildPermutations(Collection<T> col) {

        Collection<List<T>> result = new ArrayList<List<T>>();
        recBuildPermutations(col, new ArrayList<T>(), result);
        return result;
    }
}
