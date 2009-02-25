package platform.server.logics.classes.sets;

import platform.server.logics.classes.RemoteClass;
import platform.base.GraphNodeSet;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

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
