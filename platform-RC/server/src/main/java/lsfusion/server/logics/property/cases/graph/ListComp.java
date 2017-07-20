package lsfusion.server.logics.property.cases.graph;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import org.apache.xpath.NodeSet;

public class ListComp<T> implements NodeListComp<T> {
    public final ImList<NodeSetComp<T>> comps;

    public ListComp(ImList<NodeSetComp<T>> comps) {
        this.comps = comps;
        
        assert comps.size() > 1;
    }

    private <IV, V> ImList<IV> proceedComps(final CompProcessor<T, IV, V> processor) {
        return comps.mapListValues(new GetValue<IV, NodeSetComp<T>>() {
            public IV getMapValue(NodeSetComp<T> value) {
                return value.proceedInner(processor);
            }});
    }
    @Override
    public <IV, V> V proceed(CompProcessor<T, IV, V> processor) {
        return processor.proceedList(proceedComps(processor));
    }

    @Override
    public <IV, V> IV proceedInner(CompProcessor<T, IV, V> processor) {
        return processor.proceedInnerList(proceedComps(processor));
    }

    @Override
    public ImList<NodeSetComp<T>> getList() {
        return comps;
    }

    @Override
    public ImSet<NodeListComp<T>> getSet() {
        return SetFact.<NodeListComp<T>>singleton(this);
    }

    public static <T> Comp<T> create(ImList<NodeSetComp<T>> comps) {
        assert !comps.isEmpty();
        
        if(comps.size() > 1)
            return new ListComp<>(comps);
        return comps.single();
    } 
}
