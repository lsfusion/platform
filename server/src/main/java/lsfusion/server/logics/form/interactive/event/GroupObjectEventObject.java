package lsfusion.server.logics.form.interactive.event;

import lsfusion.interop.form.event.FormEvent;
import lsfusion.server.logics.form.ObjectMapping;

public class GroupObjectEventObject extends FormServerEvent<GroupObjectEventObject> {
    public final String groupObject;
    public final Type type;

    public GroupObjectEventObject(String groupObject, Type type) {
        this.groupObject = groupObject;
        this.type = type;
    }

    @Override
    public GroupObjectEventObject get(ObjectMapping mapping) {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof GroupObjectEventObject && groupObject.equals(((GroupObjectEventObject) obj).groupObject) &&
                type.equals(((GroupObjectEventObject) obj).type);
    }

    @Override
    public int hashCode() {
        return 31 * groupObject.hashCode() + type.hashCode();
    }
    
    public enum Type {
        ORDER, FILTER
    }
}
