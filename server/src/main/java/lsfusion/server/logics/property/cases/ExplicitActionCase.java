package lsfusion.server.logics.property.cases;

import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.List;

public class ExplicitActionCase<P extends PropertyInterface> extends AbstractActionCase<P> {

    private final boolean optimisticAsync;
    
    public ExplicitActionCase(PropertyMapImplement<?, P> where, ActionMapImplement<?, P> implement, boolean optimisticAsync) {
        this(where, implement, null, optimisticAsync);
    }

    public ExplicitActionCase(PropertyMapImplement<?, P> where, ActionMapImplement<?, P> implement, List<ResolveClassSet> signature, boolean optimisticAsync) {
        super(where, implement, signature);
        
        this.optimisticAsync = optimisticAsync;
    }

    @Override
    public boolean isOptimisticAsync() {
        return optimisticAsync;
    }

    // см. ExplicitCalcCase
    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof AbstractCase && where.equalsMap(((AbstractCase) o).where) && implement.equalsMap(((AbstractCase) o).implement) && signature.equals(((AbstractCase) o).signature);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * where.hashMap() + implement.hashMap()) + signature.hashCode();
    }
}
