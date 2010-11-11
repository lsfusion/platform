package platform.server.logics.property;

import platform.server.classes.ValueClass;

public abstract class ExecuteProperty extends UserProperty {

    protected ExecuteProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    public boolean isStored() {
        return false;
    }
}
