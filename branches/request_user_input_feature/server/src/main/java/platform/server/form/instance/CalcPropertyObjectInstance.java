package platform.server.form.instance;

import platform.server.caches.IdentityLazy;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.logics.DataObject;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.CalcPropertyValueImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.Map;

public class CalcPropertyObjectInstance<P extends PropertyInterface> extends PropertyObjectInstance<P, CalcProperty<P>> implements OrderInstance {

    public CalcPropertyObjectInstance(CalcProperty<P> property, Map<P, ? extends PropertyObjectInterfaceInstance> mapping) {
        super(property, mapping);
    }

    @IdentityLazy
    public CalcPropertyObjectInstance<P> getRemappedPropertyObject(Map<? extends PropertyObjectInterfaceInstance, DataObject> mapKeyValues) {
        return new CalcPropertyObjectInstance<P>(property, remap(mapKeyValues));
    }

    public CalcPropertyValueImplement<P> getValueImplement() {
        return new CalcPropertyValueImplement<P>(property, getInterfaceValues());
    }

    public Object read(SQLSession session, Modifier modifier, QueryEnvironment env) throws SQLException {
        return property.read(session, getInterfaceValues(), modifier, env);
    }

    public Object read(DataSession session, Modifier modifier) throws SQLException {
        return read(session.sql, modifier, session.env);
    }

}
