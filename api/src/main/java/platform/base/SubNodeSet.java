package platform.base;

import java.util.HashSet;
import java.util.Set;

abstract public class SubNodeSet<T,S extends SubNodeSet<T,S>> extends HashSet<T> {

    protected SubNodeSet() {
    }

    protected SubNodeSet(Set<T> iNodes) {
        super(iNodes);
    }

    protected SubNodeSet(T node) {
        add(node);
    }

    public void or(S set) {
        for(T node : set)
            or(node);
    }
    public abstract void or(T orNode);

    public S and(S set) {
        S andSet = create(new HashSet<T>());
        for(T node : this)
            andSet.or(set.and(node));
        return andSet;
    }
    public S and(T andNode) {
        S andSet = create(new HashSet<T>());
        for(T Node : this) andSet.or(create(and(andNode,Node)));
        return andSet;
    }
    public abstract Set<T> and(T andNode,T node);
    public abstract S create(Set<T> iNodes);

    public boolean equals(Object o) {
        return this == o || o instanceof SubNodeSet && super.equals(o);
    }
}
