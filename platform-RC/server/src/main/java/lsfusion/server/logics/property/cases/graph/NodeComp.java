package lsfusion.server.logics.property.cases.graph;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImSet;

public class NodeComp<T> implements NodeListComp<T>, NodeSetComp<T> {
    public final T node;

    public NodeComp(T node) {
        this.node = node;
    }

    @Override
    public <IV, V> V proceed(CompProcessor<T, IV, V> processor) {
        return processor.proceedNode(node);
    }

    @Override
    public <IV, V> IV proceedInner(CompProcessor<T, IV, V> processor) {
        return processor.proceedInnerNode(node);
    }

    public ImList<NodeSetComp<T>> getList() {
        return ListFact.<NodeSetComp<T>>singleton(this);
    }

    public ImSet<NodeListComp<T>> getSet() {
        return SetFact.<NodeListComp<T>>singleton(this);
    }
}
