package platform.server.session;

import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.data.QueryEnvironment;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.FormEnvironment;
import platform.server.logics.property.actions.flow.FlowResult;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

public abstract class ExecutionEnvironment {

    private ObjectValue lastUserInput;
    private boolean wasUserInput = false;

    public abstract DataSession getSession();

    public abstract QueryEnvironment getQueryEnv();

    public abstract Modifier getModifier();

    public abstract FormInstance getFormInstance();

    public abstract boolean isInTransaction();

    public <P extends PropertyInterface> void change(CalcProperty<P> property, PropertyChange<P> change) throws SQLException {
        DataChanges userDataChanges = null;
        if(property instanceof DataProperty) // оптимизация
            userDataChanges = getSession().getUserDataChanges((DataProperty)property, (PropertyChange<ClassPropertyInterface>) change);
        change(userDataChanges != null ? userDataChanges : property.getDataChanges(change, getModifier()));
    }

    public <P extends PropertyInterface> void change(DataChanges mapChanges) throws SQLException {
        for(DataProperty change : mapChanges.getProperties())
            getSession().changeProperty(change, mapChanges.get(change), true);
    }

    public <P extends PropertyInterface> void execute(ActionProperty<P> property, PropertyChange<P> set, FormEnvironment<P> formEnv) throws SQLException {
        for(Map.Entry<Map<P, DataObject>, Map<String, ObjectValue>> row : set.executeClasses(this).entrySet())
            execute(property, row.getKey(), formEnv, row.getValue().get("value"), null);
    }

    public <P extends PropertyInterface> void execute(ActionProperty<P> property, PropertySet<P> set, FormEnvironment<P> formEnv) throws SQLException {
        for(Map<P, DataObject> row : set.executeClasses(this))
            execute(property, row, formEnv, null, null);
    }

    public <P extends PropertyInterface> FlowResult execute(ActionProperty<P> property, Map<P, DataObject> change, FormEnvironment<P> formEnv, ObjectValue pushUserInput, DataObject pushAddObject) throws SQLException {
        return property.execute(new ExecutionContext<P>(change, pushUserInput, pushAddObject, this, formEnv, true));
    }

    public abstract DataObject addObject(ConcreteCustomClass cls, boolean groupLast, DataObject pushed) throws SQLException;

    public abstract void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject object, ConcreteObjectClass cls, boolean groupLast) throws SQLException;

    public abstract boolean apply(BusinessLogics BL) throws SQLException;

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
