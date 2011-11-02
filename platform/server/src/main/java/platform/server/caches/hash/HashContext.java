package platform.server.caches.hash;

import platform.server.data.Value;

import java.util.Set;

public class HashContext extends HashObject {

    public final HashKeys keys;
    public final HashValues values;

    public HashContext(HashKeys keys, HashValues values) {
        this.keys = keys;
        this.values = values;
    }

    public final static HashContext hashCode = new HashContext(HashCodeKeys.instance,HashCodeValues.instance);

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof HashContext && keys.equals(((HashContext) o).keys) && values.equals(((HashContext) o).values);
    }

    @Override
    public int hashCode() {
        return 31 * keys.hashCode() + values.hashCode();
    }

    @Override
    public HashContext filterValues(Set<Value> filter) {
        return new HashContext(keys, values.filterValues(filter));
    }
}
