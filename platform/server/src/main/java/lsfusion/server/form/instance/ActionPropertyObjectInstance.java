package lsfusion.server.form.instance;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ActionProperty;
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
        return new ActionPropertyObjectInstance<P>(property, remap(mapKeyValues));
    }

    public FlowResult execute(ExecutionEnvironment env) throws SQLException {
        return execute(env, null, null, null);
    }

    public FlowResult execute(ExecutionEnvironment env, ObjectValue pushValue, DataObject pushAdd, PropertyDrawInstance propertyDraw) throws SQLException {
        return env.execute(property, getInterfaceValues(), new FormEnvironment<P>(mapping, propertyDraw), pushValue, pushAdd);
    }

    public CalcPropertyObjectInstance<?> getDrawProperty() {
        return property.getWhereProperty().mapObjects(mapping);
    }
}
