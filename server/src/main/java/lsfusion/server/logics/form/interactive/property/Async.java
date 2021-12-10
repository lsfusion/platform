package lsfusion.server.logics.form.interactive.property;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.form.interactive.changed.FormChanges;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;

import java.io.DataOutputStream;
import java.io.IOException;

public class Async {

    public final String displayString;
    public final String rawString;

    public final ImMap<ObjectInstance, DataObject> key;

    public static final Async RECHECK = new Async("RECHECK", "RECHECK", null);
    public static final Async CANCELED = new Async("CANCELED", "CANCELED", null);

    public Async(String displayString, String rawString, ImMap<ObjectInstance, DataObject> key) {
        this.displayString = displayString;
        this.rawString = rawString;
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException();
//        return this == o || o instanceof Async && displayString.equals(((Async) o).displayString) && rawString.equals(((Async) o).rawString);
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
//        return displayString.hashCode() * 31 + rawString.hashCode();
    }

    public void serialize(DataOutputStream dataStream) throws IOException {
        dataStream.writeUTF(displayString);
        dataStream.writeUTF(rawString);
        dataStream.writeBoolean(key != null);
        if(key != null)
            FormChanges.serializeGroupObjectValue(dataStream, key);
    }
}
