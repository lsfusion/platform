package lsfusion.server.logics.property.actions.edit;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Query;
import lsfusion.server.form.instance.GroupObjectInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.form.instance.OrderInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ActionPropertyMapImplement;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.flow.AroundAspectActionProperty;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

// групповые изменения (групповая корректировка, paste таблицы, multi cell paste)
public class GroupChangeActionProperty extends AroundAspectActionProperty {

    public <I extends PropertyInterface> GroupChangeActionProperty(String caption, ImOrderSet<I> innerInterfaces, ActionPropertyMapImplement<?, I> changeAction) {
        super(caption, innerInterfaces, changeAction);
        
        finalizeInit();
    }

    private ImOrderSet<ImMap<ObjectInstance, DataObject>> getObjectGroupKeys(ExecutionContext context) throws SQLException, SQLHandledException {
        GroupObjectInstance groupObject = context.getChangingPropertyToDraw();
        DataSession session = context.getSession();
        return groupObject.readKeys(session.sql, context.getQueryEnv(), context.getModifier(), session.baseClass).keyOrderSet();
    }

    @Override
    protected FlowResult aroundAspect(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ImOrderSet<ImMap<ObjectInstance, DataObject>> groupKeys = getObjectGroupKeys(context); // читаем вначале, чтобы избежать эффекта последействия и влияния его на хинты

        FlowResult flowResult = proceed(context);// вызываем CHANGE (для текущего)
        if(!flowResult.equals(FlowResult.FINISH))
            return flowResult;

        if(context.getWasUserInput()) {
            ObjectValue lastObject; // запоминаем его значение, если не cancel
            if((lastObject = context.getLastUserInput())==null)
                return FlowResult.FINISH;
            context = context.pushUserInput(lastObject);
        }
        for(ImMap<ObjectInstance, DataObject> row : groupKeys) { // бежим по всем
            ImMap<PropertyInterface, ObjectValue> override = MapFact.override(context.getKeys(), context.getObjectInstances().innerJoin(row));
            if (!BaseUtils.hashEquals(override, context.getKeys())) { // кроме текущего
                proceed(context.override(override));
            }
        }

        return FlowResult.FINISH;
    }
}
