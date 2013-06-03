package lsfusion.server.logics.property.actions;

import lsfusion.server.classes.ValueClass;

public abstract class SystemExplicitActionProperty extends ExplicitActionProperty {

    protected SystemExplicitActionProperty(String sID, ValueClass... classes) {
        super(sID, classes);
    }

    protected SystemExplicitActionProperty(String sID, String caption, ValueClass... classes) {
        super(sID, caption, classes);
    }
}
