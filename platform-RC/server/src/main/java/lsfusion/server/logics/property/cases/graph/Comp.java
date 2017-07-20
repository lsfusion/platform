package lsfusion.server.logics.property.cases.graph;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImSet;

public interface Comp<T> {
    
    <IV, V> V proceed(CompProcessor<T, IV, V> processor);

    <IV, V> IV proceedInner(CompProcessor<T, IV, V> processor);

    ImList<NodeSetComp<T>> getList();
    ImSet<NodeListComp<T>> getSet();
}
