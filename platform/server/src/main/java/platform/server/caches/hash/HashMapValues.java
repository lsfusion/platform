package platform.server.caches.hash;

import platform.base.BaseUtils;
import platform.base.ImmutableObject;
import platform.server.data.Value;
import platform.base.GlobalObject;

import java.util.Map;
import java.util.Set;

public class HashMapValues extends HashLocalValues {

    public Map<Value, ? extends GlobalObject> hashValues;
    public HashMapValues(Map<Value, ? extends GlobalObject> hashValues) {
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

    public HashValues filterValues(Set<Value> values) {
        return new HashMapValues(BaseUtils.filterInclKeys(hashValues, values));
    }
}
