package lsfusion.server.logics.property.actions;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.i18n.LocalizedString;

public abstract class UserActionProperty extends ExplicitActionProperty {

    protected UserActionProperty(ValueClass... classes) {
        super(classes);
    }

    protected UserActionProperty(LocalizedString caption, ValueClass[] classes) {
        super(caption, classes);
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}
