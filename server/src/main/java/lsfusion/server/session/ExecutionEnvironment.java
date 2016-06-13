package lsfusion.server.session;

import lsfusion.base.FunctionSet;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.classes.ConcreteObjectClass;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.data.MutableClosedObject;
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

    public <P extends PropertyInterface> FlowResult execute(ActionProperty<P> property, ImMap<P, ? extends ObjectValue> change, FormEnvironment<P> formEnv, ObjectValue pushUserInput, DataObject pushAddObject, ExecutionStack stack) throws SQLException, SQLHandledException {
        return property.execute(new ExecutionContext<P>(change, pushUserInput, pushAddObject, this, null, formEnv, stack));
    }

    public abstract void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject dataObject, ConcreteObjectClass cls) throws SQLException, SQLHandledException;

    public boolean apply(BusinessLogics BL, ExecutionStack stack) throws SQLException, SQLHandledException {
        return apply(BL, stack, null);
    }

    public boolean apply(BusinessLogics BL, ExecutionContext context, ImOrderSet<ActionPropertyValueImplement> applyActions) throws SQLException, SQLHandledException {
        return apply(BL, context.stack, context, applyActions, SetFact.<SessionDataProperty>EMPTY(), null);
    }

    public boolean apply(BusinessLogics BL, ExecutionContext context) throws SQLException, SQLHandledException {
        return apply(BL, context.stack, context);
    }
    public boolean apply(ExecutionContext context) throws SQLException, SQLHandledException {
        return apply(context.getBL(), context);
    }

    public boolean apply(BusinessLogics BL, ExecutionStack stack, UserInteraction interaction) throws SQLException, SQLHandledException {
        return apply(BL, stack, interaction, SetFact.<ActionPropertyValueImplement>EMPTYORDER(), SetFact.<SessionDataProperty>EMPTY(), null);
    }

    public void cancel(ExecutionStack stack) throws SQLException, SQLHandledException {
        cancel(stack, SetFact.<SessionDataProperty>EMPTY());
    }

    public abstract boolean apply(BusinessLogics BL, ExecutionStack stack, UserInteraction interaction, ImOrderSet<ActionPropertyValueImplement> applyActions, FunctionSet<SessionDataProperty> keepProperties, FormInstance formInstance) throws SQLException, SQLHandledException;
    
    public abstract void cancel(ExecutionStack stack, FunctionSet<SessionDataProperty> keep) throws SQLException, SQLHandledException;
}
