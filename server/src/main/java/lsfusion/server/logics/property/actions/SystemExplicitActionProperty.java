package lsfusion.server.logics.property.actions;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.i18n.LocalizedString;

public abstract class SystemExplicitActionProperty extends ExplicitActionProperty {

    protected SystemExplicitActionProperty(ValueClass... classes) {
        super(classes);
    }

    protected SystemExplicitActionProperty(LocalizedString caption, ValueClass... classes) {
        super(caption, classes);
    }
}
