package lsfusion.server.session;

import lsfusion.base.MutableClosedObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.ConcreteObjectClass;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.FormEnvironment;
import lsfusion.server.logics.property.actions.flow.FlowResult;

import java.sql.SQLException;

public abstract class ExecutionEnvironment extends MutableClosedObject<Object> {

    public abstract DataSession getSession();

    public abstract QueryEnvironment getQueryEnv();

    public abstract Modifier getModifier();

    public abstract FormInstance getFormInstance();

    public abstract boolean isInTransaction();

    public <P extends PropertyInterface> void change(CalcProperty<P> property, PropertyChange<P> change) throws SQLException, SQLHandledException {
        if(change.isEmpty()) // оптимизация
            return;
        
        DataChanges userDataChanges = null;
        if(property instanceof DataProperty) // оптимизация
            userDataChanges = getSession().getUserDataChanges((DataProperty)property, (PropertyChange<ClassPropertyInterface>) change);
        change(userDataChanges != null ? userDataChanges : property.getDataChanges(change, getModifier()));
    }

    public <P extends PropertyInterface> void change(DataChanges mapChanges) throws SQLException, SQLHandledException {
        for(DataProperty change : mapChanges.getProperties())
            getSession().changeProperty(change, mapChanges.get(change));
    }

    public <P extends PropertyInterface> FlowResult execute(ActionProperty<P> property, ImMap<P, ? extends ObjectValue> change, FormEnvironment<P> formEnv, ObjectValue pushUserInput, DataObject pushAddObject) throws SQLException, SQLHandledException {
        return property.execute(new ExecutionContext<P>(change, pushUserInput, pushAddObject, this, formEnv, null));
    }

    public abstract void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject dataObject, ConcreteObjectClass cls) throws SQLException, SQLHandledException;

    public boolean apply(BusinessLogics BL) throws SQLException, SQLHandledException {
        return apply(BL, null, null);
    }

    public boolean apply(BusinessLogics BL, UserInteraction interaction) throws SQLException, SQLHandledException {
        return apply(BL, null, interaction);
    }

    public boolean apply(ExecutionContext context) throws SQLException, SQLHandledException {
        return apply(context.getBL(), context, context);
    }

    public boolean apply(BusinessLogics BL, UpdateCurrentClasses update, UserInteraction interaction) throws SQLException, SQLHandledException {
        return apply(BL, update, interaction, null, SetFact.<SessionDataProperty>EMPTY());
    }

    public void cancel() throws SQLException, SQLHandledException {
        cancel(SetFact.<SessionDataProperty>EMPTY());
    }

    public abstract boolean apply(BusinessLogics BL, UpdateCurrentClasses update, UserInteraction interaction, ActionPropertyValueImplement applyAction, ImSet<SessionDataProperty> keepProperties) throws SQLException, SQLHandledException;
    
    public abstract void cancel(ImSet<SessionDataProperty> keep) throws SQLException, SQLHandledException;
}
