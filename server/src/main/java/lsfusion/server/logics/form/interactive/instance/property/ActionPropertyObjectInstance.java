package lsfusion.server.logics.form.interactive.instance.property;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.data.DataObject;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.ActionPropertyValueImplement;
import lsfusion.server.base.context.ExecutionStack;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.form.interactive.instance.FormEnvironment;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.ExecutionEnvironment;

import java.sql.SQLException;

public class ActionPropertyObjectInstance<P extends PropertyInterface> extends PropertyObjectInstance<P, ActionProperty<P>> {

    public ActionPropertyObjectInstance(ActionProperty<P> property, ImMap<P, ? extends PropertyObjectInterfaceInstance> mapping) {
        super(property, mapping);
    }

    public ActionPropertyObjectInstance<P> getRemappedPropertyObject(ImMap<? extends PropertyObjectInterfaceInstance, DataObject> mapKeyValues) {
        return new ActionPropertyObjectInstance<>(property, remapSkippingEqualsObjectInstances(mapKeyValues));
    }

    public FlowResult execute(ExecutionEnvironment env, ExecutionStack stack, DataObject pushAdd, PropertyDrawInstance changingProperty, FormInstance formInstance) throws SQLException, SQLHandledException {
        return env.execute(property, getInterfaceObjectValues(), new FormEnvironment<>(mapping, changingProperty, formInstance), pushAdd, stack);
    }

    public CalcPropertyObjectInstance<?> getDrawProperty() {
//        return DerivedProperty.createTrue().mapObjects(MapFact.<PropertyInterface, PropertyObjectInterfaceInstance>EMPTY());
        return property.getWhereProperty().mapObjects(mapping);
    }

    public ActionPropertyValueImplement<P> getValueImplement(FormInstance formInstance) {
        return new ActionPropertyValueImplement<>(property, getInterfaceObjectValues(), mapping, formInstance);
    }
}
