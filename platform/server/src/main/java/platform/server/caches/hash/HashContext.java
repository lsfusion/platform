package platform.server.caches.hash;

import platform.base.ImmutableObject;

public class HashContext extends HashObject {

    public final HashKeys keys;
    public final HashValues values;

    public boolean isGlobal() {
        return keys.isGlobal() && values.isGlobal();
    }

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
}
