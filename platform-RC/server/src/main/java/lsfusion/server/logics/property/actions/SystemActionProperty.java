package lsfusion.server.logics.property.actions;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.PropertyInterface;

public abstract class SystemActionProperty extends BaseActionProperty<PropertyInterface> {

    protected SystemActionProperty(LocalizedString caption, ImOrderSet<PropertyInterface> interfaces) {
        super(caption, interfaces);
    }
}
