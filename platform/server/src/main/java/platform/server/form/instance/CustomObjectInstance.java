package platform.server.form.instance;

import platform.server.classes.*;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.Expr;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.Property;
import platform.server.session.Changes;
import platform.server.session.ChangesSession;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class CustomObjectInstance extends ObjectInstance {

    public CustomClass baseClass;
    CustomClass gridClass;

    public DataObjectClassInstance objectClassInstance;

    public ConcreteCustomClass currentClass;

    private CustomClassListener classListener;
    public void setClassListener(CustomClassListener classListener) {
        this.classListener = classListener;
    }

    public boolean isAddOnTransaction() {
        return entity.addOnTransaction;
    }

    public CustomObjectInstance(ObjectEntity entity, CustomClass baseClass) {
        super(entity);
        this.baseClass = baseClass;
        gridClass = baseClass;
        objectClassInstance = new DataObjectClassInstance();
    }

    public CustomClass getBaseClass() {
        return baseClass;
    }

    public void setDefaultValue(ChangesSession session) throws SQLException {
        changeValue(session, NullValue.instance);
        groupTo.updated = groupTo.updated | GroupObjectInstance.UPDATED_GRIDCLASS;
    }

    public AndClassSet getClassSet(GroupObjectInstance classGroup) {
        if(groupTo==classGroup)
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

    public void changeValue(ChangesSession session, ObjectValue changeValue) throws SQLException {

        assert changeValue!=null;
        
        if(changeValue.equals(value)) return;

        value = changeValue;

        updateValueClass(session);
    }

    public void refreshValueClass(ChangesSession session) throws SQLException {
        value = value.refresh(session);
        updateValueClass(session);
    }

    public void updateValueClass(ChangesSession session) throws SQLException {
        // запишем класс объекта
        ConcreteCustomClass changeClass;
        if(value instanceof NullValue)
            changeClass = null;
        else {
            ConcreteClass sessionClass = session.getCurrentClass(getDataObject());
            if(!(sessionClass instanceof ConcreteCustomClass)) {
                changeValue(session, NullValue.instance);
                return;
            }
            changeClass = (ConcreteCustomClass) sessionClass;
            if (classListener != null) // если вообще кто-то следит за изменением классов объектов
                classListener.objectChanged(changeClass, (Integer) getDataObject().object);
        }

        if(changeClass != currentClass) {
            currentClass = changeClass;
            updated = updated | ObjectInstance.UPDATED_CLASS;
        }

        // вообще должно быть в changeValue, но так как пока в endApply отдельно не "разбирается" случай изменения класса без изменения объекта, сделано так
        updated = updated | ObjectInstance.UPDATED_OBJECT;
        groupTo.updated = groupTo.updated | GroupObjectInstance.UPDATED_OBJECT;
    }

    public void changeValue(ChangesSession session, Object changeValue) throws SQLException {
        changeValue(session,session.getObjectValue(changeValue, baseClass.getType()));
    }

    public boolean classChanged(Collection<CustomClass> changedClasses) {
        return changedClasses.contains(gridClass);
    }

    public boolean classUpdated() {
        return (updated & ObjectInstance.UPDATED_CLASS)!=0;
    }

    public boolean classUpdated(GroupObjectInstance classGroup) {
        if(groupTo!=classGroup)
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

    protected Expr getExpr() {
        return value.getExpr();
    }

    public void changeClass(ChangesSession session, DataObject change, int classID) throws SQLException {

        // запишем объекты, которые надо будет сохранять
        if(classID==-1) {
            session.changeClass(change,null);
            changeValue(session, NullValue.instance);
        } else {
            session.changeClass(change, baseClass.findConcreteClassID(classID));
            updateValueClass(session);
        }
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

    public class DataObjectClassInstance implements OrderInstance {

        public GroupObjectInstance getApplyObject() {
            return CustomObjectInstance.this.getApplyObject();
        }

        public Type getType() {
            return SystemClass.instance;
        }

        public Expr getExpr(Map<ObjectInstance, ? extends Expr> classSource, Modifier<? extends Changes> modifier) throws SQLException {
            return CustomObjectInstance.this.getExpr(classSource, modifier).classExpr(CustomObjectInstance.this.baseClass.getBaseClass());
        }

        public boolean classUpdated(GroupObjectInstance classGroup) {
            return false;
        }

        public boolean objectUpdated(GroupObjectInstance classGroup) {
            return false;
        }

        public boolean dataUpdated(Collection<Property> changedProps) {
            return false;
        }

        public void fillProperties(Set<Property> properties) {
        }

        public boolean isInInterface(GroupObjectInstance classGroup) {
            return groupTo == classGroup;
        }
    }

}
