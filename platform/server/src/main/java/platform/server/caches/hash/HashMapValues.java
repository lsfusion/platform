package platform.server.caches.hash;

import platform.base.GlobalObject;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.Settings;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;

public class HashMapValues extends HashValues {

    public final ImMap<Value, ? extends GlobalObject> hashValues;
    public static HashValues create(ImMap<Value, ? extends GlobalObject> hashValues) {
        if(hashValues.isEmpty())
            return HashCodeValues.instance;
        return new HashMapValues(hashValues);
    }
    private HashMapValues(ImMap<Value, ? extends GlobalObject> hashValues) {
        this.hashValues = hashValues;
    }

    public int hash(Value expr) {
        return hashValues.get(expr).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof HashMapValues && hashValues.equals(((HashMapValues) o).hashValues);
    }

    @Override
    public int hashCode() {
        return hashValues.hashCode();
    }

    public boolean isGlobal() {
        return Settings.instance.isCacheInnerHashes();
    }

    public HashValues filterValues(ImSet<Value> values) {
        return create(hashValues.filterIncl(values));
    }

    public HashValues reverseTranslate(MapValuesTranslate translator, ImSet<Value> values) {
        return create(translator.translateMapValues(values.toMap()).join(hashValues));
    }
}
