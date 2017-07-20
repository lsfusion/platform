package lsfusion.server.logics.property.cases;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.CaseUnionProperty;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.UnionProperty;

import java.util.List;

public class ImplicitCalcCase<P extends PropertyInterface> extends AbstractCalcCase<P> {
            
    private boolean sameNamespace;

    public ImplicitCalcCase(CalcPropertyMapImplement<?, P> property, List<ResolveClassSet> signature, boolean sameNamespace) {
        super(property.mapClassProperty(), property, signature);
        this.sameNamespace = sameNamespace;
    }

    @Override
    protected boolean isImplicit() {
        return true;
    }

    @Override
    protected boolean getSameNamespace() {
        return sameNamespace;
    }
}
