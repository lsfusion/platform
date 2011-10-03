package platform.server.caches.hash;

import platform.server.data.Value;

public class HashCodeValues extends HashValues {

    private HashCodeValues() {
    }
    public final static HashValues instance = new HashCodeValues();

    public boolean isGlobal() {
        return true;
    }

    public int hash(Value expr) {
        return expr.hashCode();
    }

}
