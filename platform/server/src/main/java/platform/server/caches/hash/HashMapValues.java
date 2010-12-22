package platform.server.caches.hash;

import platform.server.data.Value;

import java.util.Map;

public class HashMapValues implements HashValues {

    private Map<Value, Integer> hashValues;
    public HashMapValues(Map<Value, Integer> hashValues) {
        this.hashValues = hashValues;
    }

    public int hash(Value expr) {
        return hashValues.get(expr);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof HashMapValues && hashValues.equals(((HashMapValues) o).hashValues);
    }

    @Override
    public int hashCode() {
        return hashValues.hashCode();
    }
}
