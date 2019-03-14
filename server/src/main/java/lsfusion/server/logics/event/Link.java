package lsfusion.server.logics.event;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public class Link extends TwinImmutableObject {

    public final ActionOrProperty from;
    public final ActionOrProperty to;
    public final LinkType type;

    public Link(ActionOrProperty from, ActionOrProperty to, LinkType type) {
        this.from = from;
        this.to = to;
        this.type = type;
    }

    @Override
    public String toString() {
        return from + " " + type + " " + to;
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return from.equals(((Link) o).from) && to.equals(((Link) o).to) && type == ((Link) o).type;
    }

    public int immutableHashCode() {
        return 31 * (31 * from.hashCode() + to.hashCode()) + type.hashCode();
    }
}
