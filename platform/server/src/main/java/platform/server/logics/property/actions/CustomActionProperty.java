package platform.server.logics.property.actions;

import platform.server.classes.ValueClass;
import platform.server.logics.property.ActionProperty;

public abstract class CustomActionProperty extends ActionProperty {

    protected CustomActionProperty(String sID, ValueClass... classes) {
        super(sID, classes);
    }

    protected CustomActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }
}
