package platform.server.form.instance;

import platform.interop.action.ClientAction;
import platform.server.caches.IdentityLazy;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.actions.FormEnvironment;
import platform.server.session.ExecutionEnvironment;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ActionPropertyObjectInstance extends PropertyObjectInstance<ClassPropertyInterface, ActionProperty> {

    public ActionPropertyObjectInstance(ActionProperty property, Map<ClassPropertyInterface, ? extends PropertyObjectInterfaceInstance> mapping) {
        super(property, mapping);
    }

    @IdentityLazy
    public ActionPropertyObjectInstance getRemappedPropertyObject(Map<? extends PropertyObjectInterfaceInstance, DataObject> mapKeyValues) {
        return new ActionPropertyObjectInstance(property, remap(mapKeyValues));
    }

    public List<ClientAction> execute(ExecutionEnvironment env) throws SQLException {
        return execute(env, null, null);
    }

    public List<ClientAction> execute(ExecutionEnvironment env, ObjectValue requestValue, PropertyDrawInstance propertyDraw) throws SQLException {
        return env.execute(property, ActionProperty.cast(getInterfaceValues()), new FormEnvironment<ClassPropertyInterface>(mapping, propertyDraw), requestValue);
    }
}
