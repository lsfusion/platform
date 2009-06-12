package platform.server.session;

import platform.server.data.classes.CustomClass;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.Property;

import java.util.*;

public class TableChanges implements DataChanges {

    public Map<CustomClass,AddClassTable> add;
    public Map<CustomClass, RemoveClassTable> remove;
    public Map<DataProperty, DataChangeTable> data;

    public Map<Property, IncrementChangeTable> increment;

    public Set<DataProperty> getProperties() {
        return data.keySet();
    }

    public Set<CustomClass> getAddClasses() {
        return add.keySet();
    }

    public Set<CustomClass> getRemoveClasses() {
        return remove.keySet();
    }

    public boolean hasChanges() {
        return !add.isEmpty() || !remove.isEmpty() || !data.isEmpty();
    }

    public TableChanges() {
        add = new HashMap<CustomClass, AddClassTable>();
        remove = new HashMap<CustomClass, RemoveClassTable>();
        data = new HashMap<DataProperty, DataChangeTable>();
        increment = new HashMap<Property, IncrementChangeTable>();
    }

    // конструктор копирования
    public TableChanges(TableChanges changes) {
        add = changes.add;
        remove = changes.remove;
        data = changes.data;
        increment = changes.increment;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof TableChanges && add.equals(((TableChanges) o).add) && data.equals(((TableChanges) o).data) && increment.equals(((TableChanges) o).increment) && remove.equals(((TableChanges) o).remove);

    }

    @Override
    public int hashCode() {
        return 31 * (31 * (31 * add.hashCode() + remove.hashCode()) + data.hashCode()) + increment.hashCode();
    }
}
