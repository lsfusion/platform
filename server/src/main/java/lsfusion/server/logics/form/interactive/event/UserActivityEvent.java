package lsfusion.server.logics.form.interactive.event;

import java.util.Objects;

import static lsfusion.base.BaseUtils.nullEquals;

public class UserActivityEvent {
    public String groupObject;
    public Type type;

    public UserActivityEvent(String groupObject, Type type) {
        this.groupObject = groupObject;
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UserActivityEvent) {
            return nullEquals(groupObject, ((UserActivityEvent) obj).groupObject) && 
                    nullEquals(type, ((UserActivityEvent) obj).type);
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
