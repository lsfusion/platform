package platform.server.logics.property;

import platform.base.QuickSet;
import platform.server.classes.ValueClass;
import platform.server.session.IncrementApply;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;
import platform.server.session.StructChanges;

import java.util.Set;
import java.util.HashSet;

public abstract class ExecuteProperty extends UserProperty {

    protected ExecuteProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    public boolean isStored() {
        return false;
    }

    public boolean pendingDerivedExecute() {
        return getChangeProps().size()==0;
    }
}
