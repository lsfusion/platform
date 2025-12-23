package lsfusion.server.logics.form.interactive.event;

import lsfusion.interop.form.event.FormEvent;
import lsfusion.server.logics.form.ObjectMapping;

public class UserEventObject extends FormServerEvent<UserEventObject> {
    public final String obj;
    public final Type type;

    public UserEventObject(String obj, Type type) {
        this.obj = obj;
        this.type = type;
    }

    @Override
    public UserEventObject get(ObjectMapping mapping) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof UserEventObject && obj.equals(((UserEventObject) o).obj) &&
                type.equals(((UserEventObject) o).type);
    }

    @Override
    public int hashCode() {
        return 31 * obj.hashCode() + type.hashCode();
    }

    public enum Type {
        ORDER, FILTER, FILTERGROUP, FILTERPROPERTY
    }
}
