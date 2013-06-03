package lsfusion.server.logics.property.actions;

import lsfusion.server.classes.ValueClass;

public abstract class UserActionProperty extends ExplicitActionProperty {

    protected UserActionProperty(String sID, ValueClass... classes) {
        super(sID, classes);
    }

    protected UserActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }
}
