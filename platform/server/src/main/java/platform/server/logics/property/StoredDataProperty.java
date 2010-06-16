package platform.server.logics.property;

import net.jcip.annotations.Immutable;
import platform.server.classes.ValueClass;

public class StoredDataProperty extends DataProperty {

    public StoredDataProperty(String sID, String caption, ValueClass[] classes, ValueClass value) {
        super(sID, caption, classes, value);
    }
}
