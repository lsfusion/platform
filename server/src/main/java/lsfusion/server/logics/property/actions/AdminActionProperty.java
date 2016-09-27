package lsfusion.server.logics.property.actions;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.i18n.LocalizedString;

public abstract class AdminActionProperty extends SystemExplicitActionProperty {

    protected AdminActionProperty(ValueClass... classes) {
        super(classes);
    }

    protected AdminActionProperty(LocalizedString caption, ValueClass[] classes) {
        super(caption, classes);
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}
