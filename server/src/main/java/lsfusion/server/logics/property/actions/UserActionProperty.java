package lsfusion.server.logics.property.actions;

import lsfusion.server.classes.ValueClass;

public abstract class UserActionProperty extends ExplicitActionProperty {

    protected UserActionProperty(ValueClass... classes) {
        super(classes);
    }

    protected UserActionProperty(String caption, ValueClass[] classes) {
        super(caption, classes);
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}
