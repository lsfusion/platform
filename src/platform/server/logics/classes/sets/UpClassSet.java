package platform.server.logics.classes.sets;

import platform.server.logics.classes.DataClass;
import platform.base.GraphNodeSet;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

// выше вершин
class UpClassSet extends GraphNodeSet<DataClass,UpClassSet> {

    UpClassSet() {
    }

    protected UpClassSet(Set<DataClass> iNodes) {
        super(iNodes);
    }

    public boolean has(DataClass orNode, DataClass node) {
        return orNode.isParent(node);
    }

    public Set<DataClass> and(DataClass andNode, DataClass node) {
        return andNode.commonChilds(node);
    }

    public UpClassSet create(Set<DataClass> iNodes) {
        return new UpClassSet(iNodes);
    }

    Set<DataClass> andSet(Set<DataClass> set) {
        Set<DataClass> result = new HashSet<DataClass>();
        for(DataClass node : set)
            if(has(node)) result.add(node);
        return result;
    }

    void removeSet(Set<DataClass> set) {
        for(Iterator<DataClass> i = set.iterator();i.hasNext();)
            if(has(i.next())) i.remove();
    }
}
