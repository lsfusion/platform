package lsfusion.server.logics.form.interactive.instance.property;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.implement.ActionValueImplement;
import lsfusion.server.logics.form.interactive.action.input.InputListEntity;
import lsfusion.server.logics.form.interactive.action.input.InputValueList;
import lsfusion.server.logics.form.interactive.action.async.map.AsyncMapChange;
import lsfusion.server.logics.form.interactive.instance.FormEnvironment;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.action.async.PushAsyncResult;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class ActionObjectInstance<P extends PropertyInterface> extends ActionOrPropertyObjectInstance<P, Action<P>> {

    public ActionObjectInstance(Action<P> property, ImMap<P, ? extends PropertyObjectInterfaceInstance> mapping) {
        super(property, mapping);
    }

    public ActionObjectInstance<P> getRemappedPropertyObject(ImMap<? extends PropertyObjectInterfaceInstance, ? extends ObjectValue> mapKeyValues) {
        return new ActionObjectInstance<>(property, remapSkippingEqualsObjectInstances(mapKeyValues));
    }

    public FlowResult execute(ExecutionEnvironment env, ExecutionStack stack, PushAsyncResult pushAsyncResult, PropertyDrawInstance changingProperty, FormInstance formInstance) throws SQLException, SQLHandledException {
        return env.execute(property, getInterfaceObjectValues(), new FormEnvironment<>(mapping, changingProperty, formInstance), pushAsyncResult, stack);
    }

    public ActionValueImplement<P> getValueImplement(FormInstance formInstance) {
        return new ActionValueImplement<>(property, getInterfaceObjectValues(), mapping, formInstance);
    }

}
