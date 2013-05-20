package platform.server.logics.property.actions;

import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.server.classes.ValueClass;
import platform.server.logics.property.PropertyInterface;

public abstract class SystemActionProperty extends BaseActionProperty<PropertyInterface> {

    protected SystemActionProperty(String sID, String caption, ImOrderSet<PropertyInterface> interfaces) {
        super(sID, caption, interfaces);
    }
}
