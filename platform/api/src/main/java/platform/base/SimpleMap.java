package platform.base;

public class SimpleMap<K,V> extends QuickMap<K,V> {

    protected V addValue(K key, V prevValue, V newValue) {
        return newValue;
    }

    protected boolean containsAll(V who, V what) {
        throw new RuntimeException("not supported");
    }
}
