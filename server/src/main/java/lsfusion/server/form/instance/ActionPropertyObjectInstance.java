package lsfusion.server.form.instance;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.ActionPropertyValueImplement;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.FormEnvironment;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.session.ExecutionEnvironment;

import java.sql.SQLException;

public class ActionPropertyObjectInstance<P extends PropertyInterface> extends PropertyObjectInstance<P, ActionProperty<P>> {

    public ActionPropertyObjectInstance(ActionProperty<P> property, ImMap<P, ? extends PropertyObjectInterfaceInstance> mapping) {
        super(property, mapping);
    }

    public ActionPropertyObjectInstance<P> getRemappedPropertyObject(ImMap<? extends PropertyObjectInterfaceInstance, DataObject> mapKeyValues) {
        return new ActionPropertyObjectInstance<>(property, remapSkippingEqualsObjectInstances(mapKeyValues));
    }

    public FlowResult execute(ExecutionEnvironment env, ExecutionStack stack, ObjectValue pushValue, DataObject pushAdd, PropertyDrawInstance changingProperty, FormInstance formInstance) throws SQLException, SQLHandledException {
        return env.execute(property, getInterfaceValues(), new FormEnvironment<>(mapping, changingProperty, formInstance), pushValue, pushAdd, stack);
    }

    public CalcPropertyObjectInstance<?> getDrawProperty() {
//        return DerivedProperty.createTrue().mapObjects(MapFact.<PropertyInterface, PropertyObjectInterfaceInstance>EMPTY());
        return property.getWhereProperty().mapObjects(mapping);
    }

    public ActionPropertyValueImplement<P> getValueImplement(FormInstance formInstance) {
        return new ActionPropertyValueImplement<>(property, getInterfaceValues(), mapping, formInstance);
    }
}
