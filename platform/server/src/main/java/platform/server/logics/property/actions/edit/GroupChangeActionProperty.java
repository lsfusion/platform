package platform.server.logics.property.actions.edit;

import platform.base.BaseUtils;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionPropertyMapImplement;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.flow.AroundAspectActionProperty;
import platform.server.logics.property.actions.flow.FlowResult;

import java.sql.SQLException;

// групповые изменения (групповая корректировка, paste таблицы, multi cell paste)
public class GroupChangeActionProperty extends AroundAspectActionProperty {

    public <I extends PropertyInterface> GroupChangeActionProperty(String sID, String caption, ImOrderSet<I> innerInterfaces, ActionPropertyMapImplement<?, I> changeAction) {
        super(sID, caption, innerInterfaces, changeAction);
    }

    private ImOrderSet<ImMap<ObjectInstance, DataObject>> getObjectGroupKeys(ExecutionContext context) throws SQLException {
        GroupObjectInstance groupObject = context.getGroupObjectInstance();
        ImRevMap<ObjectInstance, KeyExpr> groupKeys = groupObject.getMapKeys();
        return new Query<ObjectInstance, Object>(groupKeys, groupObject.getWhere(groupKeys, context.getModifier())).executeClasses(context).keyOrderSet();
    }

    @Override
    protected FlowResult aroundAspect(ExecutionContext<PropertyInterface> context) throws SQLException {
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
        for(ImMap<ObjectInstance, DataObject> row : groupKeys) // бежим по всем
            if(!BaseUtils.hashEquals(row, context.getKeys())) { // кроме текущего
                proceed(context.override(context.getKeys().override(context.getObjectInstances().innerJoin(row))));
            }

        return FlowResult.FINISH;
    }
}
