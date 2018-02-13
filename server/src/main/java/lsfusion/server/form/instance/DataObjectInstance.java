package lsfusion.server.form.instance;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.session.SessionChanges;

import java.sql.SQLException;

// ObjectInstance table'ы
public class DataObjectInstance extends ObjectInstance {

    DataClass dataClass;
    ObjectValue value = NullValue.instance;

    public DataObjectInstance(ObjectEntity entity, DataClass dataClass) {
        super(entity);
        this.dataClass = dataClass;
    }

    public AndClassSet getClassSet(ImSet<GroupObjectInstance> gridGroups) {
        return dataClass;
    }

    public ValueClass getGridClass() {
        return dataClass;
    }

    public DataClass getBaseClass() {
        return dataClass;
    }

    public void changeValue(SessionChanges session, ObjectValue objectValue) throws SQLException {
        if(BaseUtils.nullEquals(value, objectValue)) return;

        assert objectValue instanceof NullValue || dataClass.equals(((DataObject) objectValue).getType());
        value = objectValue;

        updated = updated | UPDATED_OBJECT;
        groupTo.updated = groupTo.updated | GroupObjectInstance.UPDATED_OBJECT;
    }

    public boolean classChanged(ChangedData changedProps) {
        return false;
    }

    public boolean classUpdated(ImSet<GroupObjectInstance> gridGroups) {
        return false;
    }

    public boolean isInInterface(GroupObjectInstance group) {
        return true;
    }

    public ObjectValue getObjectValue() {
        return value;
    }

    public ConcreteClass getCurrentClass() {
        return dataClass;
    }

    public Type getType() {
        return dataClass;
    }
}
