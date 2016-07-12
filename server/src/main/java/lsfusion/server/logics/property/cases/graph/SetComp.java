package lsfusion.server.logics.property.cases.graph;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;

/**
* Created by User on 18.09.2014.
*/
public class SetComp<T> implements NodeSetComp<T> {
    public final ImSet<NodeListComp<T>> comps;

    public SetComp(ImSet<NodeListComp<T>> comps) {
        this.comps = comps;
    }

    private <IV, V> ImSet<IV> proceedComps(final CompProcessor<T, IV, V> processor) {
        return comps.mapSetValues(new GetValue<IV, NodeListComp<T>>() {
            public IV getMapValue(NodeListComp<T> value) {
                return value.proceedInner(processor);
            }
        });
    }
    @Override
    public <IV, V> V proceed(CompProcessor<T, IV, V> processor) {
        return processor.proceedSet(proceedComps(processor));
    }

    @Override
    public <IV, V> IV proceedInner(CompProcessor<T, IV, V> processor) {
        return processor.proceedInnerSet(proceedComps(processor));
    }

    public ImList<NodeSetComp<T>> getList() {
        if(comps.isEmpty())
            return ListFact.EMPTY();
        return ListFact.<NodeSetComp<T>>singleton(this);
    }

    public ImSet<NodeListComp<T>> getSet() {
        return comps;
    }

    public static <T> Comp<T> create(ImSet<NodeListComp<T>> comps) {
        if(comps.size() == 1)
            return comps.single();
        return new SetComp<T>(comps);
    }
}
