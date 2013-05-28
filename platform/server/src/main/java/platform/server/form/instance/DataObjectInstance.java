package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.base.FunctionSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.classes.ConcreteClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.type.Type;
import platform.server.form.entity.ObjectEntity;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.CalcProperty;
import platform.server.session.SessionChanges;

import java.sql.SQLException;

// ObjectInstance table'ы
public class DataObjectInstance extends ObjectInstance {

    DataClass dataClass;
    Object value = null;

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
        Object changeValue = objectValue.getValue();
        if(BaseUtils.nullEquals(value,changeValue)) return;

        value = changeValue;

        updated = updated | UPDATED_OBJECT;
        groupTo.updated = groupTo.updated | GroupObjectInstance.UPDATED_OBJECT;
    }

    public boolean classChanged(FunctionSet<CalcProperty> changedProps) {
        return false;
    }

    public boolean classUpdated(ImSet<GroupObjectInstance> gridGroups) {
        return false;
    }

    public boolean isInInterface(GroupObjectInstance group) {
        return true;
    }

    public ObjectValue getObjectValue() {
        return ObjectValue.getValue(value,dataClass);
    }

    public ConcreteClass getCurrentClass() {
        return dataClass;
    }

    public Type getType() {
        return dataClass;
    }
}
