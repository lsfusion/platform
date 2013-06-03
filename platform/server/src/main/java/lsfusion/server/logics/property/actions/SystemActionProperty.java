package lsfusion.server.logics.property.actions;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.property.PropertyInterface;

public abstract class SystemActionProperty extends BaseActionProperty<PropertyInterface> {

    protected SystemActionProperty(String sID, String caption, ImOrderSet<PropertyInterface> interfaces) {
        super(sID, caption, interfaces);
    }
}
