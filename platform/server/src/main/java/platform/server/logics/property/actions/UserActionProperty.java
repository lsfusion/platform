package platform.server.logics.property.actions;

import platform.server.classes.ValueClass;

public abstract class UserActionProperty extends CustomActionProperty {

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
