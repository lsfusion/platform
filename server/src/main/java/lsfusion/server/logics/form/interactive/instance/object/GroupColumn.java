package lsfusion.server.logics.form.interactive.instance.object;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.mutability.ImmutableObject;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.interop.form.property.PropertyGroupType;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;

public class GroupColumn extends TwinImmutableObject {

    public final PropertyDrawInstance property;
    public final ImMap<ObjectInstance, DataObject> columnKeys;

    public GroupColumn(PropertyDrawInstance property, ImMap<ObjectInstance, DataObject> columnKeys) {
        this.property = property;
        this.columnKeys = columnKeys;
    }

    protected boolean calcTwins(TwinImmutableObject o) {
        return property.equals(((GroupColumn)o).property) && columnKeys.equals(((GroupColumn)o).columnKeys);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + columnKeys.hashCode();
    }
}
