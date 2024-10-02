package lsfusion.server.logics.form.interactive.event;

public class FilterEventObject {
    public final Integer filter;
    public final Type type;

    public FilterEventObject(Integer filter, Type type) {
        this.filter = filter;
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof FilterEventObject && filter.equals(((FilterEventObject) obj).filter) &&
                type.equals(((FilterEventObject) obj).type);
    }

    @Override
    public int hashCode() {
        return 31 * filter.hashCode() + type.hashCode();
    }

    public enum Type {
        GROUP, PROPERTY
    }
}
