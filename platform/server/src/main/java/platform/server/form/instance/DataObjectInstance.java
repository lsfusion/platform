package platform.server.form.instance;

import platform.server.classes.ConcreteClass;
import platform.server.classes.CustomClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.type.Type;
import platform.server.form.entity.ObjectEntity;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.ChangesSession;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;

// ObjectInstance table'ы
public class DataObjectInstance extends ObjectInstance {

    DataClass dataClass;
    Object value;

    public DataObjectInstance(ObjectEntity entity, DataClass dataClass) {
        super(entity);
        this.dataClass = dataClass;
        value = this.dataClass.getDefaultValue();
    }

    public AndClassSet getClassSet(Set<GroupObjectInstance> classGroups) {
        return dataClass;
    }

    public ValueClass getGridClass() {
        return dataClass;
    }

    public DataClass getBaseClass() {
        return dataClass;
    }

    public void changeValue(ChangesSession session, Object changeValue) throws SQLException {
        if(changeValue==null) changeValue = dataClass.getDefaultValue();
        if(value.equals(changeValue)) return;

        value = changeValue;

        updated = updated | UPDATED_OBJECT;
        groupTo.updated = groupTo.updated | GroupObjectInstance.UPDATED_OBJECT;
    }

    public void changeValue(ChangesSession session, ObjectValue changeValue) throws SQLException {
        changeValue(session, changeValue.getValue());
    }

    public boolean classChanged(Collection<CustomClass> changedClasses) {
        return false;
    }

    public boolean classUpdated() {
        return false;
    }

    public boolean classUpdated(GroupObjectInstance classGroup) {
        return false;
    }

    public boolean isInInterface(GroupObjectInstance group) {
        return true;
    }

    public ObjectValue getObjectValue() {
        return DataObject.getValue(value,dataClass);
    }

    public void setDefaultValue(ChangesSession session) throws SQLException {
        changeValue(session, dataClass.getDefaultValue());
        groupTo.updated = groupTo.updated | GroupObjectInstance.UPDATED_GRIDCLASS;
    }

    public ConcreteClass getCurrentClass() {
        return dataClass;
    }

    protected Expr getExpr() {
        return new ValueExpr(value,dataClass);
    }

    public Type getType() {
        return dataClass;
    }
}
