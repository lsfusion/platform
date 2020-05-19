package lsfusion.server.logics.action.controller.context;

import lsfusion.base.Result;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.server.base.exception.ApplyCanceledException;
import lsfusion.server.base.MutableClosedObject;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.implement.ActionValueImplement;
import lsfusion.server.logics.action.interactive.UserInteraction;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.DataChanges;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.user.ConcreteObjectClass;
import lsfusion.server.logics.form.interactive.instance.FormEnvironment;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public abstract class ExecutionEnvironment extends MutableClosedObject<Object> {

    public QueryEnvironment getQueryEnv() {
        return getSession().env;
    }

    public <P extends PropertyInterface> void change(Property<P> property, PropertyChange<P> change) throws SQLException, SQLHandledException {
        if(change.isEmpty()) // оптимизация
            return;
        
        DataChanges userDataChanges = null;
        if(property instanceof DataProperty) // оптимизация
            userDataChanges = getSession().getUserDataChanges((DataProperty)property, (PropertyChange<ClassPropertyInterface>) change, getQueryEnv());
        change(userDataChanges != null ? userDataChanges : property.getDataChanges(change, getModifier()));
    }

    public <P extends PropertyInterface> void change(DataChanges mapChanges) throws SQLException, SQLHandledException {
        for(DataProperty change : mapChanges.getProperties())
            getSession().changeProperty(change, mapChanges.get(change));
    }

    public <P extends PropertyInterface> FlowResult execute(Action<P> property, ImMap<P, ? extends ObjectValue> change, FormEnvironment<P> formEnv, DataObject pushAddObject, ExecutionStack stack) throws SQLException, SQLHandledException {
        // hasMoreSessionUsages is true since we don't know what gonna happen next
        return property.execute(new ExecutionContext<>(change, pushAddObject, this, null, formEnv, stack, true)); 
    }

    public void cancel(ExecutionStack stack) throws SQLException, SQLHandledException {
        cancel(stack, SetFact.EMPTY());
    }

    public abstract DataSession getSession();

    public abstract Modifier getModifier();

    public abstract FormInstance getFormInstance();

    public abstract void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject dataObject, ConcreteObjectClass cls) throws SQLException, SQLHandledException;

    public abstract boolean apply(BusinessLogics BL, ExecutionStack stack, UserInteraction interaction, ImOrderSet<ActionValueImplement> applyActions, FunctionSet<SessionDataProperty> keepProperties, ExecutionEnvironment sessionEventFormEnv, Result<String> applyMessage) throws SQLException, SQLHandledException;
    
    // no message needed
    public boolean apply(BusinessLogics BL, ExecutionStack stack, UserInteraction interaction, ImOrderSet<ActionValueImplement> applyActions, FunctionSet<SessionDataProperty> keepProperties, ExecutionEnvironment sessionEventFormEnv) throws SQLException, SQLHandledException {
        return apply(BL, stack, interaction, applyActions, keepProperties, sessionEventFormEnv, null);
    }

    // if canceled throw exception with message
    public void applyException(BusinessLogics BL, ExecutionStack stack, UserInteraction interaction, ImOrderSet<ActionValueImplement> applyActions, FunctionSet<SessionDataProperty> keepProps, ExecutionEnvironment sessionEventFormEnv) throws SQLException, SQLHandledException {
        String message = applyMessage(BL, stack, interaction, applyActions, keepProps, sessionEventFormEnv);
        if(message != null)
            throw new ApplyCanceledException(message);
    }

    // if canceled return message
    public String applyMessage(BusinessLogics BL, ExecutionStack stack, UserInteraction interaction, ImOrderSet<ActionValueImplement> applyActions, FunctionSet<SessionDataProperty> keepProps, ExecutionEnvironment sessionEventFormEnv) throws SQLException, SQLHandledException {
        Result<String> message = new Result<>();
        if (!apply(BL, stack, interaction, applyActions, keepProps, sessionEventFormEnv, message))
            return message.result;
        return null;
    }

    public abstract void cancel(ExecutionStack stack, FunctionSet<SessionDataProperty> keep) throws SQLException, SQLHandledException;
}
