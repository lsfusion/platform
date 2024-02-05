package lsfusion.server.logics.property.cases;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class ActionCase<P extends PropertyInterface> extends Case<P, PropertyInterfaceImplement<P>, ActionMapImplement<?, P>> {

    public final boolean optimisticAsync;
    
    public ActionCase(PropertyInterfaceImplement<P> where, ActionMapImplement<?, P> action, boolean optimisticAsync) {
        super(where, action);
        
        this.optimisticAsync = optimisticAsync;
    }

    public ActionCase(PropertyInterfaceImplement<P> where, ActionMapImplement<?, P> action) {
        this(where, action, false);
    }

    public ActionCase(AbstractActionCase<P> aCase) {
        this(aCase.where, aCase.implement, aCase.isOptimisticAsync());
    }
    
    public <T extends PropertyInterface> ActionCase<T> map(ImRevMap<P, T> map) {
        return new ActionCase<>(where.map(map), implement.map(map));        
    }

    public boolean isClassSimple() { // hack
        Action.onlyCachedWhereProperty.set(true); // optimization, not to calculate where property too early
        try {
            PropertyMapImplement<?, P> classProperty = implement.mapClassProperty();
            return classProperty != null && classProperty.equalsMap(where);
        } finally {
            Action.onlyCachedWhereProperty.set(null);
        }
    }
}
