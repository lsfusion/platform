package lsfusion.server.logics.property.actions;

import lsfusion.server.classes.ValueClass;

public abstract class AdminActionProperty extends SystemExplicitActionProperty {

    protected AdminActionProperty(ValueClass... classes) {
        super(classes);
    }

    protected AdminActionProperty(String caption, ValueClass[] classes) {
        super(caption, classes);
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}
