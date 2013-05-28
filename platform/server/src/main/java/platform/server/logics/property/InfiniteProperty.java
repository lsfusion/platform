package platform.server.logics.property;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.DataClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.InfiniteExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

public class InfiniteProperty extends NoIncrementProperty<PropertyInterface> {

    private final DataClass dataClass;
    public InfiniteProperty(String sID, String caption, DataClass dataClass) {
        super(sID, caption, SetFact.<PropertyInterface>EMPTYORDER());
        this.dataClass = dataClass;

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return new InfiniteExpr(dataClass);
    }
}
