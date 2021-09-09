package lsfusion.server.logics.property.cases;

import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.List;

public class ImplicitActionCase extends AbstractActionCase<PropertyInterface> {
    
    private boolean sameNamespace;

    public ImplicitActionCase(ActionMapImplement<?, PropertyInterface> action, List<ResolveClassSet> signature, boolean sameNamespace) {
        super(action.mapWhereProperty().mapClassProperty(), action, signature);
        
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

    @Override
    public boolean isOptimisticAsync() {
        return false;
    }
}
