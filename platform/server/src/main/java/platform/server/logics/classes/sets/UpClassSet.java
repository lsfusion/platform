package platform.server.logics.classes.sets;

import platform.base.GraphNodeSet;
import platform.server.logics.classes.RemoteClass;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

// выше вершин
class UpClassSet extends GraphNodeSet<RemoteClass,UpClassSet> {

    UpClassSet() {
    }

    protected UpClassSet(Set<RemoteClass> iNodes) {
        super(iNodes);
    }

    public boolean has(RemoteClass orNode, RemoteClass node) {
        return orNode.isParent(node);
    }

    public Set<RemoteClass> and(RemoteClass andNode, RemoteClass node) {
        return andNode.commonChilds(node);
    }

    public UpClassSet create(Set<RemoteClass> iNodes) {
        return new UpClassSet(iNodes);
    }

    Set<RemoteClass> andSet(Set<RemoteClass> set) {
        Set<RemoteClass> result = new HashSet<RemoteClass>();
        for(RemoteClass node : set)
            if(has(node)) result.add(node);
        return result;
    }

    void removeSet(Set<RemoteClass> set) {
        for(Iterator<RemoteClass> i = set.iterator();i.hasNext();)
            if(has(i.next())) i.remove();
    }
}
