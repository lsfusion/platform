package platform.server.view.form;

import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.CustomClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.Expr;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.session.ChangesSession;

import java.sql.SQLException;
import java.util.Collection;

public class CustomObjectImplement extends ObjectImplement {

    public CustomClass baseClass;
    CustomClass gridClass;
    
    public ConcreteCustomClass currentClass;

    private CustomClassView classView;

    final boolean addOnTransaction;

    public CustomObjectImplement(int ID, String sID, CustomClass baseClass, String caption, CustomClassView classView, boolean addOnTransaction) {
        super(ID,sID,caption);
        this.baseClass = baseClass;
        gridClass = baseClass;

        this.classView = classView;

        this.addOnTransaction = addOnTransaction;
    }

    public CustomClass getBaseClass() {
        return baseClass;
    }

    public AndClassSet getClassSet(GroupObjectImplement classGroup) {
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

    ObjectValue value;

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
            classView.objectChanged(changeClass, (Integer) getDataObject().object);
        }

        if(changeClass != currentClass) {
            currentClass = changeClass;
            updated = updated | ObjectImplement.UPDATED_CLASS;
        }

        // вообще должно быть в changeValue, но так как пока в endApply отдельно не "разбирается" случай изменения класса без изменения объекта, сделано так
        updated = updated | ObjectImplement.UPDATED_OBJECT;
        groupTo.updated = groupTo.updated | GroupObjectImplement.UPDATED_OBJECT;
    }

    public void changeValue(ChangesSession session, Object changeValue) throws SQLException {
        changeValue(session,session.getObjectValue(changeValue, baseClass.getType()));
    }

    public boolean classChanged(Collection<CustomClass> changedClasses) {
        return changedClasses.contains(gridClass);
    }

    public boolean classUpdated() {
        return (updated & ObjectImplement.UPDATED_CLASS)!=0; 
    }

    public boolean classUpdated(GroupObjectImplement classGroup) {
        if(groupTo!=classGroup)
            return (updated & ObjectImplement.UPDATED_CLASS)!=0;
        else
            return (updated & ObjectImplement.UPDATED_GRIDCLASS)!=0;
    }

    public boolean isInInterface(GroupObjectImplement group) {
        return groupTo == group || value instanceof DataObject; // если не в классовом виде то только если не null
    }

    public ObjectValue getObjectValue() {
        return value;
    }

    protected Expr getExpr() {
        return value.getExpr();
    }

    public void changeClass(ChangesSession session,int classID) throws SQLException {

        // запишем объекты, которые надо будет сохранять
        if(classID==-1) {
            session.changeClass(getDataObject(),null);
            changeValue(session, NullValue.instance);
        } else {
            session.changeClass(getDataObject(), baseClass.findConcreteClassID(classID));
            updateValueClass(session);
        }
    }


    public void changeGridClass(int classID) {

        CustomClass changeClass = baseClass.findClassID(classID);

        if(gridClass != changeClass) {
            gridClass = changeClass;

            // расставляем пометки
            updated |= ObjectImplement.UPDATED_GRIDCLASS;
            groupTo.updated |= GroupObjectImplement.UPDATED_GRIDCLASS;
        }
    }

    public Type getType() {
        return ObjectType.instance;
    }
}
