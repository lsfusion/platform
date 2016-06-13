package lsfusion.server.form.instance;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.*;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.IsClassProperty;
import lsfusion.server.session.SessionChanges;

import java.lang.ref.WeakReference;
import java.sql.SQLException;

public class CustomObjectInstance extends ObjectInstance {

    public CustomClass baseClass;
    public CustomClass gridClass;

    public ConcreteCustomClass currentClass;

    private WeakReference<CustomClassListener> weakClassListener;

    public CustomClassListener getClassListener() {
        return weakClassListener.get();        
    }

    public void setClassListener(CustomClassListener classListener) {
        this.weakClassListener = new WeakReference<CustomClassListener>(classListener);
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

    ObjectValue value = NullValue.instance;

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
                classListener.objectChanged(changeClass, (Integer) getDataObject().object);
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

    public void changeGridClass(int classID) {

        CustomClass changeClass = baseClass.findClassID(classID);

        if(gridClass != changeClass) {
            gridClass = changeClass;

            // расставляем пометки
            updated |= ObjectInstance.UPDATED_GRIDCLASS;
            groupTo.updated |= GroupObjectInstance.UPDATED_GRIDCLASS;
        }
    }

    public Type getType() {
        return ObjectType.instance;
    }
}
