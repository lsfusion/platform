package lsfusion.server.logics.form.interactive.event;

public class UpdateKeysEventObject {
    public final String groupObject;
    public final Type type;

    public UpdateKeysEventObject(String groupObject, Type type) {
        this.groupObject = groupObject;
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof UpdateKeysEventObject && groupObject.equals(((UpdateKeysEventObject) obj).groupObject) &&
                type.equals(((UpdateKeysEventObject) obj).type);
    }

    @Override
    public int hashCode() {
        return 31 * groupObject.hashCode() + type.hashCode();
    }
    
    public enum Type {
        ORDER, FILTER
    }
}
