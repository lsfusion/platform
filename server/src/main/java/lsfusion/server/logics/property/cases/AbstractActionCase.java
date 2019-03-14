package lsfusion.server.logics.property.cases;

import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.List;

public class AbstractActionCase<P extends PropertyInterface> extends AbstractCase<P, PropertyInterfaceImplement<P>, ActionMapImplement<?, P>> {

    public AbstractActionCase(PropertyInterfaceImplement<P> where, ActionMapImplement<?, P> implement, List<ResolveClassSet> signature) {
        super(where, implement, signature);
    }
}