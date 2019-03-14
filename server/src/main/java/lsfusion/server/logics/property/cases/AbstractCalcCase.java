package lsfusion.server.logics.property.cases;

import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.List;

public class AbstractCalcCase<P extends PropertyInterface> extends AbstractCase<P, PropertyInterfaceImplement<P>, PropertyInterfaceImplement<P>> {

    public AbstractCalcCase(PropertyInterfaceImplement<P> where, PropertyInterfaceImplement<P> implement, List<ResolveClassSet> signature) {
        super(where, implement, signature);
    }
}
