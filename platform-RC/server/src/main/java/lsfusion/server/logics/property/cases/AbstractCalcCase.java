package lsfusion.server.logics.property.cases;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.property.*;

import java.util.List;

public class AbstractCalcCase<P extends PropertyInterface> extends AbstractCase<P, CalcPropertyInterfaceImplement<P>, CalcPropertyInterfaceImplement<P>> {

    public AbstractCalcCase(CalcPropertyInterfaceImplement<P> where, CalcPropertyInterfaceImplement<P> implement, List<ResolveClassSet> signature) {
        super(where, implement, signature);
    }
}
