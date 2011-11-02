package platform.server.caches.hash;

import platform.server.data.Value;

import java.util.Set;

public class HashCodeValues extends HashValues {

    private HashCodeValues() {
    }
    public final static HashValues instance = new HashCodeValues();

    public int hash(Value expr) {
        return expr.hashCode();
    }

    public HashValues filterValues(Set<Value> values) {
        return this;
    }
}
