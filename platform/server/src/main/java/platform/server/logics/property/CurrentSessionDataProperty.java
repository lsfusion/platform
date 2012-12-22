package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.logics.ServerResourceBundle;

public class CurrentSessionDataProperty extends SessionDataProperty {

    public CurrentSessionDataProperty(String sID, ValueClass valueClass) {
        super(sID, ServerResourceBundle.getString("logics.property.current.session"), new ValueClass[0], valueClass);
    }
}
