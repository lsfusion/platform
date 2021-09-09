package lsfusion.server.logics.property.cases;

import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.List;

public abstract class AbstractActionCase<P extends PropertyInterface> extends AbstractCase<P, PropertyInterfaceImplement<P>, ActionMapImplement<?, P>> {

    public AbstractActionCase(PropertyInterfaceImplement<P> where, ActionMapImplement<?, P> implement, List<ResolveClassSet> signature) {
        super(where, implement, signature);
    }
    
    public abstract boolean isOptimisticAsync(); 
}