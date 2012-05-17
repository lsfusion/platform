package platform.server.form.instance;

import platform.interop.action.ClientAction;
import platform.server.caches.IdentityLazy;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ActionPropertyMapImplement;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.FormEnvironment;
import platform.server.logics.property.actions.edit.GroupChangeActionProperty;
import platform.server.session.ExecutionEnvironment;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ActionPropertyObjectInstance<P extends PropertyInterface> extends PropertyObjectInstance<P, ActionProperty<P>> {

    public ActionPropertyObjectInstance(ActionProperty<P> property, Map<P, ? extends PropertyObjectInterfaceInstance> mapping) {
        super(property, mapping);
    }

    @IdentityLazy
    public ActionPropertyObjectInstance<P> getRemappedPropertyObject(Map<? extends PropertyObjectInterfaceInstance, DataObject> mapKeyValues) {
        return new ActionPropertyObjectInstance<P>(property, remap(mapKeyValues));
    }

    public List<ClientAction> execute(ExecutionEnvironment env) throws SQLException {
        return execute(env, null, null);
    }

    public List<ClientAction> execute(ExecutionEnvironment env, ObjectValue requestValue, PropertyDrawInstance propertyDraw) throws SQLException {
        return env.execute(property, getInterfaceValues(), new FormEnvironment<P>(mapping, propertyDraw), requestValue);
    }

    public ActionPropertyObjectInstance<?> getGroupChange() {
        ActionPropertyMapImplement<P, P> changeImplement = property.getImplement();
        ArrayList<P> listInterfaces = new ArrayList<P>(property.interfaces);

        GroupChangeActionProperty groupChangeActionProperty = new GroupChangeActionProperty("GCH" + property.getSID(), "sys", listInterfaces, changeImplement);
        return groupChangeActionProperty.getImplement(listInterfaces).mapObjects(mapping);
    }

}
