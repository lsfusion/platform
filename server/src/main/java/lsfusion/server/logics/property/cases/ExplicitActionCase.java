package lsfusion.server.logics.property.cases;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.property.ActionPropertyMapImplement;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.PropertyInterface;

import java.util.List;

public class ExplicitActionCase<P extends PropertyInterface> extends AbstractActionCase<P> {

    public ExplicitActionCase(CalcPropertyMapImplement<?, P> where, ActionPropertyMapImplement<?, P> implement) {
        this(where, implement, null);
    }

    public ExplicitActionCase(CalcPropertyMapImplement<?, P> where, ActionPropertyMapImplement<?, P> implement, List<ResolveClassSet> signature) {
        super(where, implement, signature);
    }
}
