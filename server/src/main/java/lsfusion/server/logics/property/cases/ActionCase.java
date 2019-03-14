package lsfusion.server.logics.property.cases;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.action.implement.ActionPropertyMapImplement;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class ActionCase<P extends PropertyInterface> extends Case<P, PropertyInterfaceImplement<P>, ActionPropertyMapImplement<?, P>> {

    public ActionCase(PropertyInterfaceImplement<P> where, ActionPropertyMapImplement<?, P> action) {
        super(where, action);
    }

    public ActionCase(AbstractActionCase<P> aCase) {
        this(aCase.where, aCase.implement);
    }
    
    public <T extends PropertyInterface> ActionCase<T> map(ImRevMap<P, T> map) {
        return new ActionCase<>(where.map(map), implement.map(map));        
    }
}
