package lsfusion.server.data.expr.join.select;

import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.caches.AbstractOuterContext;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.join.inner.InnerJoins;
import lsfusion.server.data.expr.join.where.WhereJoin;
import lsfusion.server.data.stat.*;
import lsfusion.server.data.translate.MapTranslate;

public class ExprEqualsJoin extends ExprCompareJoin<BaseExpr, ExprEqualsJoin> {

    public ExprEqualsJoin(BaseExpr expr1, BaseExpr expr2) {
        super(expr1, expr2);
    }

    @Override
    protected ExprEqualsJoin createThis(BaseExpr expr1, BaseExpr expr2) {
        return createThis(expr1, expr2);
    }
}
