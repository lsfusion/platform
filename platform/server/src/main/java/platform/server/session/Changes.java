package platform.server.session;

import platform.server.classes.CustomClass;
import platform.server.logics.property.DataProperty;

import java.util.HashMap;
import java.util.Map;

public abstract class Changes<U extends Changes<U>> {
    
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

    @Override
    public int hashCode() {
        return 31 * (31 * (31 * add.hashCode() + remove.hashCode()) + data.hashCode());
    }

    public void addChanges(Changes<?> changes) {
        add.putAll(changes.add);
        remove.putAll(changes.remove);
        data.putAll(changes.data);
    }

    public void add(U changes) {
        addChanges(changes);
    }
}
