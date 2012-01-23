package platform.server.logics.property;

import platform.server.classes.DataClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.InfiniteExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.ArrayList;
import java.util.Map;

public class InfiniteProperty extends NoIncrementProperty<PropertyInterface> {

    private final DataClass dataClass;
    public InfiniteProperty(String sID, String caption, DataClass dataClass) {
        super(sID, caption, new ArrayList<PropertyInterface>());
        this.dataClass = dataClass;

        finalizeInit();
    }

    protected Expr calculateExpr(Map<PropertyInterface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return new InfiniteExpr(dataClass);
    }
}
