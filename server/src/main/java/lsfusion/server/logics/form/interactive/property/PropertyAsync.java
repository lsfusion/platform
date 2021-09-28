package lsfusion.server.logics.form.interactive.property;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class PropertyAsync<T extends PropertyInterface> {

    public final String displayString;
    public final String rawString;

    public final ImMap<T, DataObject> key;

    public PropertyAsync(String displayString, String rawString, ImMap<T, DataObject> key) {
        this.displayString = displayString;
        this.rawString = rawString;
        this.key = key;
    }
}
