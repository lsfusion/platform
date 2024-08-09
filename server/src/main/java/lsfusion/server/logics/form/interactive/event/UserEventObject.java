package lsfusion.server.logics.form.interactive.event;

import java.util.Objects;

import static lsfusion.base.BaseUtils.nullEquals;

public class UserEventObject {
    public final String groupObject;
    public final Type type;
    public final boolean user;

    public UserEventObject(String groupObject, Type type, boolean user) {
        this.groupObject = groupObject;
        this.type = type;
        this.user = user;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof UserEventObject && groupObject.equals(((UserEventObject) obj).groupObject) &&
                type.equals(((UserEventObject) obj).type) && user == ((UserEventObject) obj).user;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * groupObject.hashCode() + type.hashCode()) + (user ? 1 : 0);
    }
    
    public enum Type {
        ORDER, FILTER
    }
}
