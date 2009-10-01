package platform.server.session;

import platform.base.BaseUtils;
import platform.server.data.classes.CustomClass;
import platform.server.logics.properties.DataProperty;

import java.util.HashMap;
import java.util.Map;

public class TableChanges extends DataChanges<TableChanges> {

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
    public TableChanges(TableChanges changes) {
        add = changes.add;
        remove = changes.remove;
        data = changes.data;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof TableChanges && add.equals(((TableChanges) o).add) && data.equals(((TableChanges) o).data) && remove.equals(((TableChanges) o).remove);

    }

    @Override
    public int hashCode() {
        return 31 * (31 * (31 * add.hashCode() + remove.hashCode()) + data.hashCode());
    }

    public void add(TableChanges changes) {
        add.putAll(changes.add);
        remove.putAll(changes.remove);
        data.putAll(changes.data);
    }

    public void dependsAdd(TableChanges changes, CustomClass customClass) {
        BaseUtils.putNotNull(customClass,changes.add,add);
    }

    public void dependsRemove(TableChanges changes, CustomClass customClass) {
        BaseUtils.putNotNull(customClass,changes.remove,remove);
    }

    public void dependsData(TableChanges changes, DataProperty property) {
        BaseUtils.putNotNull(property,changes.data,data);

    }
}
