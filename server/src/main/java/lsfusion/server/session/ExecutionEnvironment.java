package lsfusion.server.session;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.ConcreteObjectClass;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.FormEnvironment;
import lsfusion.server.logics.property.actions.flow.FlowResult;

import java.sql.SQLException;

public abstract class ExecutionEnvironment {

    private ObjectValue lastUserInput;
    private boolean wasUserInput = false;

    public abstract DataSession getSession();

    public abstract QueryEnvironment getQueryEnv();

    public abstract Modifier getModifier();

    public abstract FormInstance getFormInstance();

    public abstract boolean isInTransaction();

    public <P extends PropertyInterface> void change(CalcProperty<P> property, PropertyChange<P> change) throws SQLException {
        if(change.isEmpty()) // оптимизация
            return;
        
        DataChanges userDataChanges = null;
        if(property instanceof DataProperty) // оптимизация
            userDataChanges = getSession().getUserDataChanges((DataProperty)property, (PropertyChange<ClassPropertyInterface>) change);
        change(userDataChanges != null ? userDataChanges : property.getDataChanges(change, getModifier()));
    }

    public <P extends PropertyInterface> void change(DataChanges mapChanges) throws SQLException {
        for(DataProperty change : mapChanges.getProperties())
            getSession().changeProperty(change, mapChanges.get(change));
    }

    public <P extends PropertyInterface> void execute(ActionProperty<P> property, PropertySet<P> set, FormEnvironment<P> formEnv) throws SQLException {
        for(ImMap<P, DataObject> row : set.executeClasses(this))
            execute(property, row, formEnv, null, null);
    }

    public <P extends PropertyInterface> FlowResult execute(ActionProperty<P> property, ImMap<P, ? extends ObjectValue> change, FormEnvironment<P> formEnv, ObjectValue pushUserInput, DataObject pushAddObject) throws SQLException {
        return property.execute(new ExecutionContext<P>(change, pushUserInput, pushAddObject, this, formEnv, null));
    }

    public abstract void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject dataObject, ConcreteObjectClass cls) throws SQLException;

    public boolean apply(BusinessLogics BL) throws SQLException {
        return apply(BL, null);
    }

    public abstract boolean apply(BusinessLogics BL, UpdateCurrentClasses update) throws SQLException;

    public abstract void cancel() throws SQLException;

    public ObjectValue getLastUserInput() {
        return lastUserInput;
    }
    public boolean getWasUserInput() {
        return wasUserInput;
    }

    public void setLastUserInput(ObjectValue lastUserInput) {
        this.lastUserInput = lastUserInput;
        this.wasUserInput = true;
    }
}
