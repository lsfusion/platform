package lsfusion.server.logics.property.actions;

import lsfusion.server.classes.ValueClass;

public abstract class SystemExplicitActionProperty extends ExplicitActionProperty {

    protected SystemExplicitActionProperty(ValueClass... classes) {
        super(classes);
    }

    protected SystemExplicitActionProperty(String caption, ValueClass... classes) {
        super(caption, classes);
    }
}
