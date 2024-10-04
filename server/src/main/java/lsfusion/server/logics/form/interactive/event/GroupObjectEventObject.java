package lsfusion.server.logics.form.interactive.event;

public class GroupObjectEventObject {
    public final String groupObject;
    public final Type type;

    public GroupObjectEventObject(String groupObject, Type type) {
        this.groupObject = groupObject;
        this.type = type;
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
