package lsfusion.server.logics.property.value;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.InfiniteExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.property.NoIncrementProperty;
import lsfusion.server.logics.property.classes.infer.CalcType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

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
