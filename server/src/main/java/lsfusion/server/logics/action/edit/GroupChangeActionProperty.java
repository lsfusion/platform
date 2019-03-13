package lsfusion.server.logics.action.edit;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.SQLCallable;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.instance.GroupObjectInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.ActionPropertyMapImplement;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.action.flow.AroundAspectActionProperty;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

// групповые изменения (групповая корректировка, paste таблицы, multi cell paste)
public class GroupChangeActionProperty extends AroundAspectActionProperty {

    public <I extends PropertyInterface> GroupChangeActionProperty(LocalizedString caption, ImOrderSet<I> innerInterfaces, ActionPropertyMapImplement<?, I> changeAction) {
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

        FlowResult flowResult = proceed(context);// вызываем CHANGE (для текущего)
        if (!flowResult.equals(FlowResult.FINISH))
            return flowResult;

        if(context.isRequestCanceled()) // canceled оптимизация
            return FlowResult.FINISH;
            
        return context.pushRequest(new SQLCallable<FlowResult>() {
            public FlowResult call() throws SQLException, SQLHandledException {
                for (ImMap<ObjectInstance, DataObject> row : groupKeys) { // бежим по всем
                    ImMap<PropertyInterface, ObjectValue> override = MapFact.override(context.getKeys(), context.getObjectInstances().innerJoin(row));
                    if (!BaseUtils.hashEquals(override, context.getKeys())) { // кроме текущего
                        proceed(context.override(override));
                    }
                }
                return FlowResult.FINISH;
            }
        });
    }
}
