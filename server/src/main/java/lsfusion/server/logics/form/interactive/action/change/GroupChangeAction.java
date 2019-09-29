package lsfusion.server.logics.form.interactive.action.change;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLCallable;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.AroundAspectAction;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

// групповые изменения (групповая корректировка, paste таблицы, multi cell paste)
public class GroupChangeAction extends AroundAspectAction {

    public <I extends PropertyInterface> GroupChangeAction(LocalizedString caption, ImOrderSet<I> innerInterfaces, ActionMapImplement<?, I> changeAction) {
        super(caption, innerInterfaces, changeAction);
        
        finalizeInit();
    }

    private static ImOrderSet<ImMap<ObjectInstance, DataObject>> getObjectGroupKeys(GroupObjectInstance groupObject, ExecutionContext context) throws SQLException, SQLHandledException {
        DataSession session = context.getSession();
        return groupObject.readKeys(session.sql, context.getQueryEnv(), context.getModifier(), session.baseClass).keyOrderSet();
    }

    @Override
    protected FlowResult aroundAspect(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        GroupObjectInstance groupObject = context.getChangingPropertyToDraw();
        if(groupObject == null || !groupObject.curClassView.isGrid())
            return proceed(context);

        final ImOrderSet<ImMap<ObjectInstance, DataObject>> groupKeys = getObjectGroupKeys(groupObject, context); // читаем вначале, чтобы избежать эффекта последействия и влияния его на хинты

        context.dropRequestCanceled();

        FlowResult flowResult = proceed(context.override(true));// вызываем CHANGE (для текущего)
        if (!flowResult.equals(FlowResult.FINISH))
            return flowResult;

        if(context.isRequestCanceled()) // canceled оптимизация
            return FlowResult.FINISH;
            
        return context.pushRequest(() -> {
            for (ImMap<ObjectInstance, DataObject> row : groupKeys) { // бежим по всем
                ImMap<PropertyInterface, ObjectValue> override = MapFact.override(context.getKeys(), context.getObjectInstances().innerJoin(row));
                if (!BaseUtils.hashEquals(override, context.getKeys())) { // кроме текущего
                    proceed(context.override(override, true));
                }
            }
            return FlowResult.FINISH;
        });
    }
}
