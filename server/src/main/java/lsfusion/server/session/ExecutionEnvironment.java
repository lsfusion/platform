package lsfusion.server.session;

import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.Result;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.exception.ApplyCanceledException;
import lsfusion.server.logics.classes.ConcreteObjectClass;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.data.MutableClosedObject;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.FormEnvironment;
import lsfusion.server.logics.action.flow.FlowResult;

import java.sql.SQLException;

public abstract class ExecutionEnvironment extends MutableClosedObject<Object> {

    public QueryEnvironment getQueryEnv() {
        return getSession().env;
    }

    public <P extends PropertyInterface> void change(CalcProperty<P> property, PropertyChange<P> change) throws SQLException, SQLHandledException {
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

    // в обход dataChanges (иначе проблема с классами, так как в новой сессии изменений по классам нет, и в итоге изменению внутрь не скопируются и при копировании назад затрут те что были) - чем то напоминает noClasses - но noClasses более общая штука
    public void copyDataTo(SessionDataProperty property, PropertyChange<ClassPropertyInterface> change) throws SQLException, SQLHandledException {
        getSession().changeProperty(property, change);
    }

    public <P extends PropertyInterface> FlowResult execute(ActionProperty<P> property, ImMap<P, ? extends ObjectValue> change, FormEnvironment<P> formEnv, DataObject pushAddObject, ExecutionStack stack) throws SQLException, SQLHandledException {
        return property.execute(new ExecutionContext<>(change, pushAddObject, this, null, formEnv, stack));
    }

    public void cancel(ExecutionStack stack) throws SQLException, SQLHandledException {
        cancel(stack, SetFact.<SessionDataProperty>EMPTY());
    }

    public abstract DataSession getSession();

    public abstract Modifier getModifier();

    public abstract FormInstance getFormInstance();

    public abstract void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject dataObject, ConcreteObjectClass cls) throws SQLException, SQLHandledException;

    public abstract boolean apply(BusinessLogics BL, ExecutionStack stack, UserInteraction interaction, ImOrderSet<ActionPropertyValueImplement> applyActions, FunctionSet<SessionDataProperty> keepProperties, ExecutionEnvironment sessionEventFormEnv, Result<String> applyMessage) throws SQLException, SQLHandledException;
    
    // no message needed
    public boolean apply(BusinessLogics BL, ExecutionStack stack, UserInteraction interaction, ImOrderSet<ActionPropertyValueImplement> applyActions, FunctionSet<SessionDataProperty> keepProperties, ExecutionEnvironment sessionEventFormEnv) throws SQLException, SQLHandledException {
        return apply(BL, stack, interaction, applyActions, keepProperties, sessionEventFormEnv, null);
    }

    // if canceled throw exception with message
    public void applyException(BusinessLogics BL, ExecutionStack stack, UserInteraction interaction, ImOrderSet<ActionPropertyValueImplement> applyActions, FunctionSet<SessionDataProperty> keepProps, ExecutionEnvironment sessionEventFormEnv) throws SQLException, SQLHandledException {
        String message = applyMessage(BL, stack, interaction, applyActions, keepProps, sessionEventFormEnv);
        if(message != null)
            throw new ApplyCanceledException(message);
    }

    // if canceled return message
    public String applyMessage(BusinessLogics BL, ExecutionStack stack, UserInteraction interaction, ImOrderSet<ActionPropertyValueImplement> applyActions, FunctionSet<SessionDataProperty> keepProps, ExecutionEnvironment sessionEventFormEnv) throws SQLException, SQLHandledException {
        Result<String> message = new Result<>();
        if (!apply(BL, stack, interaction, applyActions, keepProps, sessionEventFormEnv, message))
            return message.result;
        return null;
    }

    public abstract void cancel(ExecutionStack stack, FunctionSet<SessionDataProperty> keep) throws SQLException, SQLHandledException;
}
