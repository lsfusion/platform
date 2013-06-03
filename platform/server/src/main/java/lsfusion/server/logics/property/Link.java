package lsfusion.server.logics.property;

import lsfusion.base.TwinImmutableObject;

public class Link extends TwinImmutableObject {

    public final Property from;
    public final Property to;
    public final LinkType type;

    public Link(Property from, Property to, LinkType type) {
        this.from = from;
        this.to = to;
        this.type = type;
    }

    @Override
    public String toString() {
        return from + " " + type + " " + to;
    }

    public boolean twins(TwinImmutableObject o) {
        return from.equals(((Link) o).from) && to.equals(((Link) o).to) && type == ((Link) o).type;
    }

    public int immutableHashCode() {
        return 31 * (31 * from.hashCode() + to.hashCode()) + type.hashCode();
    }
}
