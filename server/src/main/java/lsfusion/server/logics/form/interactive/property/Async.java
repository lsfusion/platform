package lsfusion.server.logics.form.interactive.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.form.interactive.changed.FormChanges;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;

import java.io.DataOutputStream;
import java.io.IOException;

public class Async {

    public final String displayString;
    public final String rawString;

    public final Object key; // ImMap (ObjectInstance -> ObjectValue) or String (JSON)

    public static final Async RECHECK = new Async("RECHECK", "RECHECK", null);
    public static final Async CANCELED = new Async("CANCELED", "CANCELED", null);
    public static final Async NEEDMORE = new Async("NEEDMORE", "NEEDMORE", null);

    public Async(String displayString, String rawString, Object key) {
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

    public void serialize(FormInstanceContext context, DataOutputStream dataStream) throws IOException {
        BaseUtils.serializeObject(dataStream, FormChanges.convertFileValue(null, displayString, context));
        BaseUtils.serializeObject(dataStream, FormChanges.convertFileValue(null, rawString, context));
        serializeKey(dataStream, key);
    }

    public static void serializeKey(DataOutputStream outStream, Object key) throws IOException {
        if(key == null) {
            outStream.writeByte(0);
            return;
        }

        if(key instanceof ImMap) {
            outStream.writeByte(1);
            FormChanges.serializeGroupObjectValue(outStream, (ImMap<ObjectInstance, ? extends ObjectValue>) key);
            return;
        }

        if(key instanceof String) {
            outStream.writeByte(2);
            BaseUtils.serializeString(outStream, (String) key);
            return;
        }

        throw new IOException();
    }

}
