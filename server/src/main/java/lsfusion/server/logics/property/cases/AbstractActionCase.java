package lsfusion.server.logics.property.cases;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.property.*;

import java.util.List;

public class AbstractActionCase<P extends PropertyInterface> extends AbstractCase<P, CalcPropertyInterfaceImplement<P>, ActionPropertyMapImplement<?, P>> {

    public AbstractActionCase(CalcPropertyInterfaceImplement<P> where, ActionPropertyMapImplement<?, P> implement, List<ResolveClassSet> signature) {
        super(where, implement, signature);
    }
}