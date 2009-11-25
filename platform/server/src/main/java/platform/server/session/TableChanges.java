package platform.server.session;

import platform.server.classes.CustomClass;
import platform.server.logics.property.DataProperty;

import java.util.HashMap;
import java.util.Map;

public abstract class TableChanges<U extends TableChanges<U>> implements DataChanges<U> {
    
    public final Map<CustomClass,AddClassTable> add;
    public final Map<CustomClass, RemoveClassTable> remove;
    public final Map<DataProperty, DataChangeTable> data;

    public boolean hasChanges() {
        return !add.isEmpty() || !remove.isEmpty() || !data.isEmpty();
    }

    public TableChanges() {
        add = new HashMap<CustomClass, AddClassTable>();
        remove = new HashMap<CustomClass, RemoveClassTable>();
        data = new HashMap<DataProperty, DataChangeTable>();
    }

    // конструктор копирования
    public TableChanges(U changes) {
        add = changes.add;
        remove = changes.remove;
        data = changes.data;
    }

    protected TableChanges(Map<CustomClass, AddClassTable> add, Map<CustomClass, RemoveClassTable> remove, Map<DataProperty, DataChangeTable> data) {
        this.add = add;
        this.remove = remove;
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof TableChanges && add.equals(((TableChanges) o).add) && data.equals(((TableChanges) o).data) && remove.equals(((TableChanges) o).remove);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * (31 * add.hashCode() + remove.hashCode()) + data.hashCode());
    }

    public void addTableChanges(TableChanges<?> changes) {
        add.putAll(changes.add);
        remove.putAll(changes.remove);
        data.putAll(changes.data);
    }

    public void add(U changes) {
        addTableChanges(changes);
    }
}
