package platform.server.logics.property.actions.edit;

import platform.base.BaseUtils;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

// групповые изменения (групповая корректировка, paste таблицы, multi cell paste)
public class GroupChangeActionProperty extends AroundAspectActionProperty {

    public <I extends PropertyInterface> GroupChangeActionProperty(String sID, String caption, List<I> innerInterfaces, ActionPropertyMapImplement<I> changeAction) {
        super(sID, caption, innerInterfaces, changeAction);
    }

    private Set<Map<ObjectInstance, DataObject>> getObjectGroupKeys(ExecutionContext context) throws SQLException {
        GroupObjectInstance groupObject = context.getGroupObjectInstance();
        Map<ObjectInstance, KeyExpr> groupKeys = groupObject.getMapKeys();
        return new Query<ObjectInstance, Object>(groupKeys, groupObject.getWhere(groupKeys, context.getModifier())).executeClasses(context.getSession()).keySet();
    }

    @Override
    protected FlowResult aroundAspect(ExecutionContext context) throws SQLException {
        FlowResult flowResult = proceed(context);// вызываем CHANGE (для текущего)
        ObjectValue lastObject; // запоминаем его значение, если не cancel
        if(!flowResult.equals(FlowResult.FINISH) || (lastObject = context.getLastUserInput())==null)
            return flowResult;

        context = context.pushUserInput(lastObject);
        for(Map<ObjectInstance, DataObject> row : getObjectGroupKeys(context)) // бежим по всем
            if(!BaseUtils.hashEquals(row, context.getKeys())) { // кроме текущего
                proceed(context.override(BaseUtils.replace(context.getKeys(), BaseUtils.rightJoin(context.getObjectInstances(), row))));
            }

        return FlowResult.FINISH;
    }
}
