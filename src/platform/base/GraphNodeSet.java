package platform.base;

import java.util.Set;
import java.util.Iterator;
import java.util.HashSet;

public abstract class GraphNodeSet<T,S extends GraphNodeSet<T,S>> extends SubNodeSet<T,S> {

    protected GraphNodeSet() {
    }

    protected GraphNodeSet(Set<T> iNodes) {
        super(iNodes);
    }

    public boolean has(T checkNode) {
        for(T node : this)
            if(has(checkNode,node)) return true;
        return false;
    }
    public abstract boolean has(T orNode,T node);

    public void or(T orNode) {
        for(T Node : this)
            if(has(orNode,Node)) return;
        for(Iterator<T> i=iterator();i.hasNext();)
            if(has(i.next(), orNode)) i.remove();
        add(orNode);
    }

    void removeAll(GraphNodeSet<T,S> toRemove) {
        for(Iterator<T> i = iterator();i.hasNext();)
            if(toRemove.has(i.next())) i.remove();
    }

    public S excludeAll(GraphNodeSet<T,S> toRemove) {
        S result = create(new HashSet<T>());
        for(T node : this)
            if(!toRemove.has(node)) result.add(node);
        return result;
    }

}
