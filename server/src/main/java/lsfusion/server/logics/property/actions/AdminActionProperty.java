package lsfusion.server.logics.property.actions;

import lsfusion.server.classes.ValueClass;

public abstract class AdminActionProperty extends SystemExplicitActionProperty {

    protected AdminActionProperty(String sID, ValueClass... classes) {
        super(sID, classes);
    }

    protected AdminActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}
