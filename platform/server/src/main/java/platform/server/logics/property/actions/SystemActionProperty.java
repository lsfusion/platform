package platform.server.logics.property.actions;

import platform.server.classes.ValueClass;

public abstract class SystemActionProperty extends CustomActionProperty {

    protected SystemActionProperty(String sID, ValueClass... classes) {
        super(sID, classes);
    }

    protected SystemActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }
}
