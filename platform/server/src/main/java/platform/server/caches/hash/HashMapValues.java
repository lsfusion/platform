package platform.server.caches.hash;

import platform.base.*;
import platform.server.caches.ValuesContext;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;

import java.util.HashMap;
import java.util.Map;

public class HashMapValues extends HashValues {

    public final QuickMap<Value, ? extends GlobalObject> hashValues;
    public HashMapValues(QuickMap<Value, ? extends GlobalObject> hashValues) {
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
        return false;
    }

    public HashValues filterValues(QuickSet<Value> values) {
        return new HashMapValues(hashValues.filterInclKeys(values));
    }

    public HashValues reverseTranslate(MapValuesTranslate translator, QuickSet<Value> values) {
        QuickMap<Value, GlobalObject> transValues = new SimpleMap<Value, GlobalObject>();
        for(int i=0;i<values.size;i++) {
            Value value = values.get(i);
            transValues.add(value, hashValues.get(translator.translate(value)));
        }
        return new HashMapValues(transValues);
    }
}
