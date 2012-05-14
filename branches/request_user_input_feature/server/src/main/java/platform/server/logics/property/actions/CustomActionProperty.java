package platform.server.logics.property.actions;

import platform.server.classes.ValueClass;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.CalcProperty;

import java.util.HashSet;
import java.util.Set;

public abstract class CustomActionProperty extends ActionProperty {

    protected CustomActionProperty(String sID, ValueClass... classes) {
        super(sID, classes);
    }

    protected CustomActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    public Set<CalcProperty> getChangeProps() {
        return new HashSet<CalcProperty>();
    }

    public Set<CalcProperty> getUsedProps() {
        return new HashSet<CalcProperty>();
    }
}
