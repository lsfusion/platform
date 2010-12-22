package platform.server.caches.hash;

import platform.server.data.Value;

public class HashCodeValues implements HashValues {

    private HashCodeValues() {
    }
    public final static HashValues instance = new HashCodeValues();

    public int hash(Value expr) {
        return expr.hashCode();
    }

}
