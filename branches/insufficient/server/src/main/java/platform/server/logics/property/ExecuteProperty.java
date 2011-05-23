package platform.server.logics.property;

import platform.server.classes.ValueClass;

import java.util.Set;
import java.util.HashSet;

public abstract class ExecuteProperty extends UserProperty {

    protected ExecuteProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    public boolean isStored() {
        return false;
    }

    public Set<Property> getChangeProps() {
        return new HashSet<Property>();
    }
}
