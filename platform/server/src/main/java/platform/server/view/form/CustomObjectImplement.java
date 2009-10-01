package platform.server.view.form;

import platform.server.data.classes.ConcreteCustomClass;
import platform.server.data.classes.ConcreteObjectClass;
import platform.server.data.classes.CustomClass;
import platform.server.data.classes.ValueClass;
import platform.server.data.classes.where.AndClassSet;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.ObjectType;
import platform.server.data.types.Type;
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

    public CustomObjectImplement(int iID, String iSID, CustomClass iBaseClass, String iCaption, CustomClassView iClassView) {
        super(iID,iSID,iCaption);
        baseClass = iBaseClass;
        gridClass = baseClass;

        classView = iClassView;
    }

    public CustomClass getBaseClass() {
        return baseClass;
    }

    public AndClassSet getClassSet(GroupObjectImplement classGroup) {
        if(groupTo==classGroup)
            return getGridClass().getUpSet();
        else
            return getObjectClass();
    }

    public ValueClass getGridClass() {
        return gridClass;
    }

    public ConcreteObjectClass getObjectClass() {
        if(currentClass==null) // нету объекта
            return baseClass.getBaseClass().unknown;
        else
            return currentClass;
    }

    ObjectValue value;

    @Override
    public void changeValue(ChangesSession session, ObjectValue changeValue) throws SQLException {

        assert changeValue!=null;
        
        if(changeValue.equals(value)) return;

        value = changeValue;

        // запишем класс объекта
        ConcreteCustomClass changeClass;
        if(value instanceof NullValue)
            changeClass = null;
        else {
            changeClass = (ConcreteCustomClass) session.getCurrentClass(getDataObject());
            classView.objectChanged(changeClass, (Integer) getDataObject().object);
        }

        if(changeClass != currentClass) {
            currentClass = changeClass;
            updated = updated | ObjectImplement.UPDATED_CLASS;
        }

        updated = updated | ObjectImplement.UPDATED_OBJECT;
        groupTo.updated = groupTo.updated | GroupObjectImplement.UPDATED_OBJECT;
    }

    public void changeValue(ChangesSession session, Object changeValue) throws SQLException {
        changeValue(session,session.getObjectValue(changeValue, baseClass.getType()));
    }

    public boolean classChanged(Collection<CustomClass> changedClasses) {
        return changedClasses.contains(gridClass);
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

    protected SourceExpr getExpr() {
        return value.getExpr();
    }

    public void changeClass(ChangesSession session,int classID) throws SQLException {

        // запишем объекты, которые надо будет сохранять
        if(classID==-1) {
            session.changeClass(getDataObject(),null);
            changeValue(session, NullValue.instance);
        } else
            session.changeClass(getDataObject(),baseClass.findConcreteClassID(classID));
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
