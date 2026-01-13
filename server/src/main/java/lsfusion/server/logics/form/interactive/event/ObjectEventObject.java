package lsfusion.server.logics.form.interactive.event;

import lsfusion.interop.form.event.FormEvent;
import lsfusion.server.logics.form.ObjectMapping;

public class ObjectEventObject extends FormServerEvent<ObjectEventObject> {
    public final String object;

    public ObjectEventObject(String object) {
        this.object = object;
    }

    @Override
    public ObjectEventObject get(ObjectMapping mapping) {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof ObjectEventObject && object.equals(((ObjectEventObject) obj).object);
    }

    @Override
    public int hashCode() {
        return object.hashCode();
    }
}
