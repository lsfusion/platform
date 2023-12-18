package lsfusion.server.logics.form.interactive.event;

import java.util.Objects;

import static lsfusion.base.BaseUtils.nullEquals;

public class UserEventObject {
    public String groupObject;
    public Type type;

    public UserEventObject(String groupObject, Type type) {
        this.groupObject = groupObject;
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UserEventObject) {
            return nullEquals(groupObject, ((UserEventObject) obj).groupObject) && 
                    nullEquals(type, ((UserEventObject) obj).type);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(groupObject);
    }
    
    public enum Type {
        ORDER, FILTER
    }
}
