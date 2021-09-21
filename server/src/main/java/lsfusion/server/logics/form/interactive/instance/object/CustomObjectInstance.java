package lsfusion.server.logics.form.interactive.instance.object;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.session.change.SessionChanges;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.ConcreteObjectClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.UnknownClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.form.interactive.changed.ChangedData;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.IsClassProperty;

import java.lang.ref.WeakReference;
import java.sql.SQLException;

public class CustomObjectInstance extends ObjectInstance {

    public CustomClass baseClass;
    public CustomClass gridClass;

    public ConcreteCustomClass currentClass;

    private WeakReference<CustomClassListener> weakClassListener;

    public CustomClassListener getClassListener() {
        return weakClassListener == null ? null : weakClassListener.get();
    }

    public void setClassListener(CustomClassListener classListener) {
        this.weakClassListener = new WeakReference<>(classListener);
    }

    public CustomObjectInstance(ObjectEntity entity, CustomClass baseClass) {
        super(entity);
        this.baseClass = baseClass;
        gridClass = baseClass;
    }

    public CustomClass getBaseClass() {
        return baseClass;
    }

    public AndClassSet getClassSet(ImSet<GroupObjectInstance> gridGroups) {
        if(objectInGrid(gridGroups))
            return getGridClass().getUpSet();
        else
            return getCurrentClass();
    }

    public ConcreteObjectClass getCurrentClass() {
        if(currentClass==null) // нету объекта
            return baseClass.getBaseClass().unknown;
        else
            return currentClass;
    }

    public CustomClass getGridClass() {
        return gridClass;
    }

    private ObjectValue value = NullValue.instance;

    public void changeValue(SessionChanges session, ObjectValue changeValue) throws SQLException, SQLHandledException {
        if(changeValue.equals(value)) return;

        value = changeValue;

        updateCurrentClass(session);

        updated = updated | ObjectInstance.UPDATED_OBJECT;
        groupTo.updated = groupTo.updated | GroupObjectInstance.UPDATED_OBJECT;
    }

    public void refreshValueClass(SessionChanges session) throws SQLException, SQLHandledException {
        value = value.refresh(session, getBaseClass());
        updateCurrentClass(session);
    }

    public void updateCurrentClass(SessionChanges session) throws SQLException, SQLHandledException {
        // запишем класс объекта
        ConcreteCustomClass changeClass;
        if(value instanceof NullValue)
            changeClass = null;
        else {
            ConcreteClass sessionClass = session.getCurrentClass(getDataObject());
            if(!(sessionClass instanceof ConcreteCustomClass)) {
//                groupTo.addSeek(this, getDataObject(), false);
                return;
            }
            changeClass = (ConcreteCustomClass) sessionClass;
            CustomClassListener classListener = getClassListener();
            if (classListener != null) // если вообще кто-то следит за изменением классов объектов
                classListener.objectChanged(changeClass, (Long) getDataObject().object);
        }

        if(changeClass != currentClass) {
            currentClass = changeClass;
            updated = updated | ObjectInstance.UPDATED_CLASS;
        }
    }

    public boolean classChanged(ChangedData changedProps) {
        IsClassProperty property = gridClass.getProperty();
        return changedProps.externalProps.contains(property) || changedProps.props.contains(property);

    }

    public boolean classUpdated(ImSet<GroupObjectInstance> gridGroups) {
        if(objectInGrid(gridGroups))
            return (updated & ObjectInstance.UPDATED_CLASS)!=0;
        else
            return (updated & ObjectInstance.UPDATED_GRIDCLASS)!=0;
    }

    public boolean isInInterface(GroupObjectInstance group) {
        return groupTo == group || value instanceof DataObject; // если не в классовом виде то только если не null
    }

    public ObjectValue getObjectValue() {
        return value;
    }

    public void changeClass(SessionChanges session, DataObject change, ConcreteObjectClass cls) throws SQLException, SQLHandledException {

        // запишем объекты, которые надо будет сохранять
        session.changeClass(change,cls);

        if(cls instanceof UnknownClass)
            groupTo.dropSeek(this);
        else
            updateCurrentClass(session);
    }

    public Type getType() {
        return ObjectType.instance;
    }
}
