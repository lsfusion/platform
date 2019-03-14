package lsfusion.server.logics.property.cases;

import lsfusion.server.logics.action.implement.ActionPropertyMapImplement;
import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.property.implement.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.List;

public class AbstractActionCase<P extends PropertyInterface> extends AbstractCase<P, CalcPropertyInterfaceImplement<P>, ActionPropertyMapImplement<?, P>> {

    public AbstractActionCase(CalcPropertyInterfaceImplement<P> where, ActionPropertyMapImplement<?, P> implement, List<ResolveClassSet> signature) {
        super(where, implement, signature);
    }
}