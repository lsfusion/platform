package lsfusion.server.logics.property.cases;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.UnionProperty;

import java.util.List;

public class ExplicitCalcCase<P extends PropertyInterface> extends AbstractCalcCase<P> {

    public ExplicitCalcCase(CalcPropertyInterfaceImplement<P> where, CalcPropertyInterfaceImplement<P> implement) {
        this(where, implement, null);
    }

    public ExplicitCalcCase(CalcPropertyInterfaceImplement<P> where, CalcPropertyInterfaceImplement<P> implement, List<ResolveClassSet> signature) {
        super(where, implement, signature);
    }
}
