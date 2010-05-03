package platform.server.session;

import platform.server.classes.CustomClass;
import platform.server.logics.property.DataProperty;
import platform.server.caches.hash.HashValues;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.AbstractMapValues;
import platform.server.data.expr.ValueExpr;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public abstract class Changes<U extends Changes<U>> extends AbstractMapValues<U> {
    
    public final Map<CustomClass, AddClassTable> add;
    public final Map<CustomClass, RemoveClassTable> remove;
    public final Map<DataProperty, DataChangeTable> data;

    public boolean hasChanges() {
        return !add.isEmpty() || !remove.isEmpty() || !data.isEmpty();
    }

    public Changes() {
        add = new HashMap<CustomClass, AddClassTable>();
        remove = new HashMap<CustomClass, RemoveClassTable>();
        data = new HashMap<DataProperty, DataChangeTable>();
    }

    protected Changes(Changes<U> changes, Map<ValueExpr,ValueExpr> mapValues) {
        add = MapValuesIterable.translate(changes.add, mapValues);
        remove = MapValuesIterable.translate(changes.remove, mapValues);
        data = MapValuesIterable.translate(changes.data, mapValues);
    }

    // конструктор копирования
    public Changes(U changes) {
        add = changes.add;
        remove = changes.remove;
        data = changes.data;
    }

    protected Changes(Map<CustomClass, AddClassTable> add, Map<CustomClass, RemoveClassTable> remove, Map<DataProperty, DataChangeTable> data) {
        this.add = add;
        this.remove = remove;
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Changes && add.equals(((Changes) o).add) && data.equals(((Changes) o).data) && remove.equals(((Changes) o).remove);
    }

    public void addChanges(Changes<?> changes) {
        add.putAll(changes.add);
        remove.putAll(changes.remove);
        data.putAll(changes.data);
    }

    public void add(U changes) {
        addChanges(changes);
    }

    public int hashValues(HashValues hashValues) {
        return (MapValuesIterable.hash(add,hashValues) * 31 + MapValuesIterable.hash(remove,hashValues)) * 31 + MapValuesIterable.hash(data,hashValues);
    }

    public Set<ValueExpr> getValues() {
        Set<ValueExpr> result = new HashSet<ValueExpr>();
        MapValuesIterable.enumValues(result,add);
        MapValuesIterable.enumValues(result,remove);
        MapValuesIterable.enumValues(result,data);
        return result;
    }
}
