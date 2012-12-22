package platform.server.caches.hash;

import platform.base.col.interfaces.immutable.ImSet;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;

public class HashCodeValues extends HashValues {

    private HashCodeValues() {
    }
    public final static HashValues instance = new HashCodeValues();

    public int hash(Value expr) {
        return expr.hashCode();
    }

    public boolean isGlobal() {
        return true;
    }

    public HashValues filterValues(ImSet<Value> values) {
        return this;
    }

    public HashValues reverseTranslate(MapValuesTranslate translate, ImSet<Value> values) {
        if(translate.identityValues(values))
            return this;
        else
            return null;
    }
}
