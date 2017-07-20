package lsfusion.server.logics.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.InfiniteExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.session.PropertyChanges;

public class InfiniteProperty extends NoIncrementProperty<PropertyInterface> {

    private final DataClass dataClass;
    public InfiniteProperty(LocalizedString caption, DataClass dataClass) {
        super(caption, SetFact.<PropertyInterface>EMPTYORDER());
        this.dataClass = dataClass;

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return new InfiniteExpr(dataClass);
    }
}
