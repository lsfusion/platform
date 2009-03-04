package platform.base;

import java.util.Iterator;
import java.util.Map;

class MapUtils<T,V> {

    public T getKey(Map<T,V> m, V v) {

        Iterator<T> it = m.keySet().iterator();
        while (it.hasNext()) {
           T t = it.next();
           if (m.get(t) == v) return t;
        }
        return null;

    }

}
