package lsfusion.server.logics.property.cases;

import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.List;

public class ImplicitCalcCase<P extends PropertyInterface> extends AbstractCalcCase<P> {
            
    private boolean sameNamespace;

    public ImplicitCalcCase(PropertyMapImplement<?, P> property, List<ResolveClassSet> signature, boolean sameNamespace) {
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
