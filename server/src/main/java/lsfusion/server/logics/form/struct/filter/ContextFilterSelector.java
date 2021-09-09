package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public abstract class ContextFilterSelector<V extends PropertyInterface, O extends ObjectSelector> {

    public static <O extends ObjectSelector, V extends PropertyInterface> ImSet<ContextFilterEntity<?, V, O>> getEntities(ImSet<ContextFilterSelector<V, O>> contextFilters) {
        MExclSet<ContextFilterEntity<?, V, O>> mContextFilters = SetFact.mExclSet();
        for(ContextFilterSelector<V, O> contextFilter : contextFilters)
            mContextFilters.exclAddAll(contextFilter.getEntities());
        return mContextFilters.immutable();
    }

    public abstract ImSet<? extends ContextFilterEntity<?, V, O>> getEntities();

    public abstract <C extends PropertyInterface> ContextFilterSelector<C, O> map(ImRevMap<V, C> map);
    
    public abstract PropertyMapImplement<?, V> getWhereProperty(ImRevMap<O, V> objects);
}
