package platform.server.logics.property.actions;

import platform.server.classes.ValueClass;

public abstract class AdminActionProperty extends SystemActionProperty {

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
