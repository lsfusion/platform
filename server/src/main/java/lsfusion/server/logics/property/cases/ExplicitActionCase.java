package lsfusion.server.logics.property.cases;

import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.action.implement.ActionPropertyMapImplement;
import lsfusion.server.logics.property.implement.CalcPropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.List;

public class ExplicitActionCase<P extends PropertyInterface> extends AbstractActionCase<P> {

    public ExplicitActionCase(CalcPropertyMapImplement<?, P> where, ActionPropertyMapImplement<?, P> implement) {
        this(where, implement, null);
    }

    public ExplicitActionCase(CalcPropertyMapImplement<?, P> where, ActionPropertyMapImplement<?, P> implement, List<ResolveClassSet> signature) {
        super(where, implement, signature);
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
