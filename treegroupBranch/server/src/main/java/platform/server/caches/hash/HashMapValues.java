package platform.server.caches.hash;

import platform.server.data.expr.ValueExpr;

public class HashMapValues implements HashValues {

    private HashMapValues() {    
    }
    public final static HashValues instance = new HashMapValues();

    public int hash(ValueExpr expr) {
        return expr.objectClass.hashCode();
    }

}
