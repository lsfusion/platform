package lsfusion.server.logics.property.cases.graph;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImSet;

public interface CompProcessor<T, IV, V> {
    
    IV proceedInnerNode(T element);
    
    IV proceedInnerSet(ImSet<IV> elements);

    IV proceedInnerList(ImList<IV> elements);

    V proceedNode(T element);

    V proceedSet(ImSet<IV> elements);

    V proceedList(ImList<IV> elements);
}
