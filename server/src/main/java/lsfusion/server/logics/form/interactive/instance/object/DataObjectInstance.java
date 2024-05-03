package lsfusion.server.logics.form.interactive.instance.object;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.session.change.SessionChanges;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.form.interactive.changed.ChangedData;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;

// ObjectInstance table'ы
public class DataObjectInstance extends ObjectInstance {

    DataClass dataClass;

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
