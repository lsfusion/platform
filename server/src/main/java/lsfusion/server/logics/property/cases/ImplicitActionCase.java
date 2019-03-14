package lsfusion.server.logics.property.cases;

import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.property.ActionPropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.List;

public class ImplicitActionCase extends AbstractActionCase<PropertyInterface> {
    
    private boolean sameNamespace;

    public ImplicitActionCase(ActionPropertyMapImplement<?, PropertyInterface> action, List<ResolveClassSet> signature, boolean sameNamespace) {
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
}
