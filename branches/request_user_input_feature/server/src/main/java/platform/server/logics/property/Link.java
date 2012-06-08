package platform.server.logics.property;

public class Link {

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

    public boolean equals(Object o) {
        return this == o || o instanceof Link && from.equals(((Link) o).from) && to.equals(((Link) o).to) && type == ((Link) o).type;
    }

    public int hashCode() {
        return 31 * (31 * from.hashCode() + to.hashCode()) + type.hashCode();
    }
}
